package com.incidentiq.ai.entity;

import com.incidentiq.user.entity.User;
import jakarta.persistence.*;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(
        name = "recommendations",
        uniqueConstraints = {
                @UniqueConstraint(name = "uq_rec_analysis_pos", columnNames = {"analysis_id", "position"})
        },
        indexes = {
                @Index(name = "idx_recommendations_analysis", columnList = "analysis_id")
        }
)
public class Recommendation {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "analysis_id", nullable = false)
    private AiAnalysis analysis;

    @Column(name = "position", nullable = false)
    private int position;

    @Column(name = "action", columnDefinition = "TEXT", nullable = false)
    private String action;

    @Column(name = "rationale", columnDefinition = "TEXT", nullable = false)
    private String rationale;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", length = 10, nullable = false)
    private RecommendationStatus status = RecommendationStatus.SUGGESTED;

    @Enumerated(EnumType.STRING)
    @Column(name = "feedback_rating", length = 11)
    private FeedbackRating feedbackRating;

    @Column(name = "feedback_comment", columnDefinition = "TEXT")
    private String feedbackComment;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "feedback_by")
    private User feedbackBy;

    @Column(name = "feedback_at")
    private Instant feedbackAt;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt = Instant.now();

    protected Recommendation() {}

    public Recommendation(AiAnalysis analysis, int position, String action, String rationale) {
        this.analysis = analysis;
        this.position = position;
        this.action = action;
        this.rationale = rationale;
        this.status = RecommendationStatus.SUGGESTED;
        this.createdAt = Instant.now();
    }

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public AiAnalysis getAnalysis() {
        return analysis;
    }

    public int getPosition() {
        return position;
    }

    public String getAction() {
        return action;
    }

    public String getRationale() {
        return rationale;
    }

    public RecommendationStatus getStatus() {
        return status;
    }

    public void setStatus(RecommendationStatus status) {
        this.status = status;
    }

    public FeedbackRating getFeedbackRating() {
        return feedbackRating;
    }

    public void setFeedbackRating(FeedbackRating feedbackRating) {
        this.feedbackRating = feedbackRating;
    }

    public String getFeedbackComment() {
        return feedbackComment;
    }

    public void setFeedbackComment(String feedbackComment) {
        this.feedbackComment = feedbackComment;
    }

    public User getFeedbackBy() {
        return feedbackBy;
    }

    public void setFeedbackBy(User feedbackBy) {
        this.feedbackBy = feedbackBy;
    }

    public Instant getFeedbackAt() {
        return feedbackAt;
    }

    public void setFeedbackAt(Instant feedbackAt) {
        this.feedbackAt = feedbackAt;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}
