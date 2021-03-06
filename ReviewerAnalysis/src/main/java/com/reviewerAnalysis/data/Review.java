package com.reviewerAnalysis.data;

import javax.persistence.*;

/*
    This table holds very detailed information about each review.
    Information that can be extracted from other variables are not
    saved separately to avoid having to add, change or remove fields.
 */
@Entity
public class Review {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private Long id;

    // in ms
    private Long timestamp;

    // in ms
    private Long timeSincePreviousReview;

    private int rating;

    private double averageProductRating;

    private int length;

    private boolean hasPicture;

    private boolean hasVideo;

    private boolean isPurchaseVerified;

    // calculated ideal rating
    private int sentimentAnalysis;

    /*
    data extractable from reviewText:
        length
        average length of words and sentences
        number of question marks, exclamation marks, consecutive capital letters
        distinct words / all words
        number of <br> tags
     */
    @Column( length = 10000 ) // limit might vary between platforms, Amazon's limit is 5000
    private String reviewText;

    private String lang;

    private String password;

    private String persona;

    private boolean isForTraining; // for being able to save reviewers' reviews while also collecting training data

    public Review(Long timestamp, Long timeSincePreviousReview, int rating, double averageProductRating, int length, boolean hasPicture, boolean hasVideo, boolean isPurchaseVerified, int sentimentAnalysis, String reviewText, String lang, String password, String persona, boolean isForTraining) {
        this.timestamp = timestamp;
        this.timeSincePreviousReview = timeSincePreviousReview;
        this.rating = rating;
        this.averageProductRating = averageProductRating;
        this.length = length;
        this.hasPicture = hasPicture;
        this.hasVideo = hasVideo;
        this.isPurchaseVerified = isPurchaseVerified;
        this.sentimentAnalysis = sentimentAnalysis;
        this.reviewText = reviewText;
        this.lang = lang;
        this.password = password;
        this.persona = persona;
        this.isForTraining = isForTraining;
    }

    public Review() {}

    public void setId(Long id) {
        this.id = id;
    }

    public Long getId() {
        return id;
    }

    public Long getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(Long timestamp) {
        this.timestamp = timestamp;
    }

    public Long getTimeSincePreviousReview() {
        return timeSincePreviousReview;
    }

    public void setTimeSincePreviousReview(Long timeSincePreviousReview) {
        this.timeSincePreviousReview = timeSincePreviousReview;
    }

    public int getRating() {
        return rating;
    }

    public void setRating(int rating) {
        this.rating = rating;
    }

    public double getAverageProductRating() {
        return averageProductRating;
    }

    public void setAverageProductRating(double averageProductRating) {
        this.averageProductRating = averageProductRating;
    }

    public boolean isHasPicture() {
        return hasPicture;
    }

    public void setHasPicture(boolean hasPicture) {
        this.hasPicture = hasPicture;
    }

    public boolean isHasVideo() {
        return hasVideo;
    }

    public void setHasVideo(boolean hasVideo) {
        this.hasVideo = hasVideo;
    }

    public boolean isPurchaseVerified() {
        return isPurchaseVerified;
    }

    public void setPurchaseVerified(boolean purchaseVerified) {
        isPurchaseVerified = purchaseVerified;
    }

    public int getSentimentAnalysis() {
        return sentimentAnalysis;
    }

    public void setSentimentAnalysis(int sentimentAnalysis) {
        this.sentimentAnalysis = sentimentAnalysis;
    }

    public String getReviewText() {
        return reviewText;
    }

    public void setReviewText(String reviewText) {
        this.reviewText = reviewText;
    }

    public String getLang() {
        return lang;
    }

    public void setLang(String lang) {
        this.lang = lang;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public String getPersona() {
        return persona;
    }

    public void setPersona(String persona) {
        this.persona = persona;
    }

    public int getLength() {
        return length;
    }

    public void setLength(int length) {
        this.length = length;
    }

    public boolean isForTraining() {
        return isForTraining;
    }
}

