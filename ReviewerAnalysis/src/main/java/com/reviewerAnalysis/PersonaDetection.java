package com.reviewerAnalysis;

import com.reviewerAnalysis.controller.ReviewController;
import com.reviewerAnalysis.data.Persona;
import com.reviewerAnalysis.data.PersonaRepository;
import com.reviewerAnalysis.data.Review;
import com.reviewerAnalysis.data.ReviewRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import weka.classifiers.Classifier;
import weka.classifiers.trees.lmt.LogisticBase;
import weka.core.Instances;
import weka.core.converters.ConverterUtils;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.util.*;

@Service
public class PersonaDetection {

    @Autowired
    PersonaRepository personaRepository;

    @Autowired
    ReviewRepository reviewRepository;

    private LogisticBase logisticBase;

    private Instances train;

    private Instances accuracyTrain;

    private Map<String, Double> accuracies;
    private boolean isCalculating;

    private PersonaDetection() {
        accuracies = new HashMap<>();
        isCalculating = false;
    }

    public boolean isCalculating() {
        return isCalculating;
    }

    public double getAccuracy(String lang) {
        return accuracies.get(lang) == null ? -1.0 : accuracies.get(lang);
    }

    public ReviewController.Result calcAccuracy(String lang, Classifier classifier) {
        isCalculating = true;
        Classifier defaultClassifier = new LogisticBase();
        if (classifier == null) {
            classifier = defaultClassifier;
        }
        long start = System.currentTimeMillis();
        int correct = 0;
        Map<String, Integer> correctCounter = new HashMap<>();
        Map<String, Integer> wrongCounter = new HashMap<>();
        for (Persona persona : personaRepository.findAllByOrderByIdAsc()) {
            correctCounter.put(persona.getName(), 0);
            wrongCounter.put(persona.getName(), 0);
        }
        List<Review> reviews = reviewRepository.findByLangAndIsForTraining(lang, true);
        for (int i = 0; i < reviews.size(); i++) {

            try {
                String fileName = "accuracy-test-" + lang + ".arff";
                List<Review> list = reviewRepository.findByLangAndIsForTraining(lang, true);
                list.remove(reviews.get(i));
                writeFile(fileName, lang, list);

                ConverterUtils.DataSource source1 = new ConverterUtils.DataSource("accuracy-test-" + lang + ".arff");
                accuracyTrain = source1.getDataSet();
                if (accuracyTrain.classIndex() == -1) {
                    accuracyTrain.setClassIndex(accuracyTrain.numAttributes() - 1);
                }

                classifier.buildClassifier(accuracyTrain);
            } catch (Exception e) {
                e.printStackTrace();
            }

            try {

                String fileName = "predict-accuracy-" + lang + ".arff";
                List<Review> predictAccuracy = new LinkedList<>();
                predictAccuracy.add(reviews.get(i));
                writeFile(fileName, lang, predictAccuracy);

                ConverterUtils.DataSource source2 = new ConverterUtils.DataSource(fileName);
                Instances prediction = source2.getDataSet();

                if (prediction.classIndex() == -1) {
                    prediction.setClassIndex(accuracyTrain.numAttributes() - 1);
                }

                List<String> personas = new LinkedList<>();
                for (int j = 0; j < prediction.numInstances(); j++) {
                    double label = classifier.classifyInstance(prediction.instance(j));
                    prediction.instance(j).setClassValue(label);
                    String result = prediction.instance(j).stringValue(prediction.numAttributes() - 1);
                    //System.out.println(persona);
                    personas.add(result);
                }

                if (personas.get(0).equals(reviews.get(i).getPersona())) {
                    correct++;
                    correctCounter.put(personas.get(0), correctCounter.get(personas.get(0)) + 1);
                } else {
                    wrongCounter.put(personas.get(0), wrongCounter.get(personas.get(0)) + 1);
                    System.err.println("Error! Expected " + reviews.get(i).getPersona() + ", got " + personas.get(0));
                }
            } catch (Exception e) {
                e.printStackTrace();
                isCalculating = false;
                return null;
            }
        }
        double accuracy = (double) correct / reviews.size();

        if (defaultClassifier == classifier) {
            accuracies.put(lang, accuracy);
        }

        Map<String, Double> personaAccuracies = new HashMap<>();
        for (Persona persona : personaRepository.findAllByOrderByIdAsc()) {
            int rights = correctCounter.get(persona.getName());
            int wrongs = wrongCounter.get(persona.getName());
            personaAccuracies.put(persona.getName(), (double) rights / (rights + wrongs));
        }

        isCalculating = false;
        return new ReviewController.Result(personaAccuracies, accuracy, System.currentTimeMillis() - start);
    }

    public void train(String lang) {
        try {
            String fileName = "train-" + lang + ".arff";
            writeFile(fileName, lang, reviewRepository.findByLangAndIsForTraining(lang, true));

            ConverterUtils.DataSource source1 = new ConverterUtils.DataSource("train-" + lang + ".arff");
            train = source1.getDataSet();
            if (train.classIndex() == -1) {
                train.setClassIndex(train.numAttributes() - 1);
            }

            logisticBase = new LogisticBase();
            logisticBase.buildClassifier(train);
            System.out.println(logisticBase.getWeightTrimBeta());
            System.out.println(logisticBase.getNumRegressions());
            System.out.println(logisticBase.getBatchSize());
            System.out.println(logisticBase);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }


    public List<String> detectPersona(List<Review> reviews) {
        try {
            Iterator<Review> iterator = reviews.iterator();
            while(iterator.hasNext()) {
                iterator.next().setPersona(null);
            }
            String lang = reviews.get(0).getLang();
            String fileName = "predict-" + lang + ".arff";
            writeFile(fileName, lang, reviews);

            ConverterUtils.DataSource source2 = new ConverterUtils.DataSource(fileName);
            Instances prediction = source2.getDataSet();

            if (prediction.classIndex() == -1) {
                prediction.setClassIndex(train.numAttributes() - 1);
            }

            List<String> personas = new LinkedList<>();
            for (int i = 0; i < prediction.numInstances(); i++) {
                double label = logisticBase.classifyInstance(prediction.instance(i));
                prediction.instance(i).setClassValue(label);
                String persona = prediction.instance(i).stringValue(prediction.numAttributes() - 1);
                System.out.println(persona);
                personas.add(persona);
            }

            return personas;
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    private void writeFile(String fileName, String lang, List<Review> reviews) {
        InputStream dataIn = null;
        try {
            BufferedWriter writer = new BufferedWriter(new FileWriter(fileName));

            writeBasis(writer, lang);

            for (Review review : reviews) {
                Stats stats = calculateTextStats(review.getReviewText());
                int sentimentRatingOffset = review.getSentimentAnalysis() - review.getRating();
                sentimentRatingOffset += 5; // Some classifiers cannot deal with negative numbers
                String string = "";
                string += (review.isHasPicture() ? 1 : 0) + ",";
                string += (review.isHasVideo() ? 1 : 0) + ",";
                string += (review.isPurchaseVerified() ? 1 : 0) + ",";
                string += review.getLength() + ",";
                string += review.getRating() + ",";
                string += review.getSentimentAnalysis() + ",";
                string += sentimentRatingOffset + ",";
                string += stats.getConsecutiveCaps() + ",";
                string += stats.getConsecutivePeriods() + ",";
                string += stats.getConsCapsTextRatio() + ",";
                string += stats.getConsPeriodsTextRatio() + ",";
                string += stats.getDistinctWordRatio() + ",";
                string += stats.getAverageWordLength() + ",";
                string += stats.getLineBreaks() + ",";
                string += stats.getQuestionMarks() + ",";
                string += stats.getExclMarks() + ",";
                string += stats.getLineBreakTextRatio() + ",";
                string += stats.getPunctuationLetterRatio() + ",";
                string += review.getPersona() == null ? "?" : review.getPersona();
                writer.write(string + "\n");
            }
            writer.close();
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            if (dataIn != null) {
                try {
                    dataIn.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    private void writeBasis(BufferedWriter writer, String lang) throws IOException {
        List<String> personaNames = new LinkedList<>();
        for (Persona persona : personaRepository.findAll()) {
            personaNames.add(persona.getName());
        }
        String personas = "{";
        personas += String.join(",", personaNames) + "}";

        writer.write("@RELATION reviews-" + lang + "\n\n");
        writer.write("@ATTRIBUTE hasPicture             NUMERIC\n");
        writer.write("@ATTRIBUTE hasVideo               NUMERIC\n");
        writer.write("@ATTRIBUTE isPurchaseVerified     NUMERIC\n");
        writer.write("@ATTRIBUTE length                 NUMERIC\n");
        writer.write("@ATTRIBUTE rating                 NUMERIC\n");
        writer.write("@ATTRIBUTE sentimentAnalysis      NUMERIC\n");
        writer.write("@ATTRIBUTE sentimentRatingOffset  NUMERIC\n");
        writer.write("@ATTRIBUTE consecutiveCaps        NUMERIC\n");
        writer.write("@ATTRIBUTE consecutivePeriods     NUMERIC\n");
        writer.write("@ATTRIBUTE consCapsTextRatio      NUMERIC\n");
        writer.write("@ATTRIBUTE consPeriodsTextRatio   NUMERIC\n");
        writer.write("@ATTRIBUTE distinctWordRatio      NUMERIC\n");
        writer.write("@ATTRIBUTE averageWordLength      NUMERIC\n");
        writer.write("@ATTRIBUTE numberOfLineBreaks     NUMERIC\n");
        writer.write("@ATTRIBUTE numberOfQuestionMarks  NUMERIC\n");
        writer.write("@ATTRIBUTE numberOfExclMarks      NUMERIC\n");
        writer.write("@ATTRIBUTE lineBreakTextRatio     NUMERIC\n");
        writer.write("@ATTRIBUTE punctuationLetterRatio NUMERIC\n");
        writer.write("@ATTRIBUTE persona                " + personas + "\n\n");
        writer.write("@DATA\n");
    }

    private Stats calculateTextStats(String text) {
        int questionMarks = StringUtils.countOccurrencesOf(text, "?");
        int exclMarks = StringUtils.countOccurrencesOf(text, "!");
        int lineBreaks = StringUtils.countOccurrencesOf(text, "<br>");


        text = StringUtils.replace(text, "<br>", " ");
        Scanner scan = new Scanner(text);
        ArrayList<String> words = new ArrayList<>();
        ArrayList<String> uniqueWords = new ArrayList<>();
        while (scan.hasNext()) {
            String word = scan.next();
            words.add(word);
            if (!uniqueWords.contains(word)) {
                uniqueWords.add(word);
            }
        }

        int consecutiveCaps = 0;
        int consecutivePeriods = 0;
        for (int i = 0; i < text.length() - 1; i++) {
            if (Character.isUpperCase(text.charAt(i)) && Character.isUpperCase(text.charAt(i + 1))) {
                consecutiveCaps++;
            }
            if (text.charAt(i) == '.' && text.charAt(i + 1) == '.') {
                consecutivePeriods++;
            }
        }

        double consCapsTextRatio = (double) consecutiveCaps / text.length();
        double consPeriodsTextRatio = (double) consecutivePeriods / text.length();
        double lineBreakTextRatio = (double) lineBreaks / words.size();
        double averageWordLength = (double) text.length() / words.size();
        double distinctWordRatio = (double) uniqueWords.size() / words.size();
        double punctuationLetterRatio = (double) (questionMarks + exclMarks) / words.size();
        return new Stats(consecutivePeriods, consecutiveCaps, exclMarks, questionMarks, lineBreaks, consCapsTextRatio, consPeriodsTextRatio, lineBreakTextRatio, distinctWordRatio, averageWordLength, punctuationLetterRatio);
    }

    private class Stats {
        private int consecutivePeriods;
        private int consecutiveCaps;
        private int exclMarks;
        private int questionMarks;
        private int lineBreaks;
        private double consCapsTextRatio;
        private double consPeriodsTextRatio;
        private double lineBreakTextRatio;
        private double distinctWordRatio;
        private double averageWordLength;
        private double punctuationLetterRatio;

        public Stats(int consecutivePeriods, int consecutiveCaps, int exclMarks, int questionMarks, int lineBreaks, double consCapsTextRatio, double consPeriodsTextRatio, double lineBreakTextRatio, double distinctWordRatio, double averageWordLength, double punctuationLetterRatio) {
            this.consecutivePeriods = consecutivePeriods;
            this.consecutiveCaps = consecutiveCaps;
            this.exclMarks = exclMarks;
            this.questionMarks = questionMarks;
            this.lineBreaks = lineBreaks;
            this.consCapsTextRatio = consCapsTextRatio;
            this.consPeriodsTextRatio = consPeriodsTextRatio;
            this.lineBreakTextRatio = lineBreakTextRatio;
            this.distinctWordRatio = distinctWordRatio;
            this.averageWordLength = averageWordLength;
            this.punctuationLetterRatio = punctuationLetterRatio;
        }

        public int getConsecutiveCaps() {
            return consecutiveCaps;
        }

        public int getExclMarks() {
            return exclMarks;
        }

        public int getQuestionMarks() {
            return questionMarks;
        }

        public double getDistinctWordRatio() {
            return distinctWordRatio;
        }

        public double getAverageWordLength() {
            return averageWordLength;
        }

        public int getLineBreaks() {
            return lineBreaks;
        }

        public double getPunctuationLetterRatio() {
            return punctuationLetterRatio;
        }

        public double getLineBreakTextRatio() {
            return lineBreakTextRatio;
        }

        public double getConsPeriodsTextRatio() {
            return consPeriodsTextRatio;
        }

        public double getConsCapsTextRatio() {
            return consCapsTextRatio;
        }

        public int getConsecutivePeriods() {
            return consecutivePeriods;
        }
    }
}
