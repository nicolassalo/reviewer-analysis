package com.reviewerAnalysis.model;

public class TextRating {

    private int rating;
    private double languageConfidence;

    public TextRating(int rating, double languageConfidence) {
        this.rating = rating;
        this.languageConfidence = languageConfidence;
    }

    public TextRating() {}

    public double getLanguageConfidence() {
        return languageConfidence;
    }

    public int getRating() {
        return rating;
    }
}
