package com.incidentiq.analytics.service;

import com.incidentiq.ai.entity.AnalysisStatus;
import com.incidentiq.ai.entity.FeedbackRating;
import com.incidentiq.ai.entity.RecommendationStatus;
import com.incidentiq.ai.repository.AiAnalysisRepository;
import com.incidentiq.ai.repository.RecommendationRepository;
import com.incidentiq.analytics.dto.*;
import com.incidentiq.common.cache.CacheConfig;
import com.incidentiq.incident.entity.IncidentCategory;
import com.incidentiq.incident.entity.IncidentPriority;
import com.incidentiq.incident.entity.IncidentStatus;
import com.incidentiq.incident.repository.IncidentRepository;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.Instant;
import java.util.*;

@Service
@Transactional(readOnly = true)
public class AnalyticsService {

    private static final Set<IncidentStatus> ACTIVE_STATUSES = Set.of(
            IncidentStatus.OPEN,
            IncidentStatus.ACKNOWLEDGED,
            IncidentStatus.IN_PROGRESS
    );

    private final IncidentRepository incidentRepository;
    private final AiAnalysisRepository aiAnalysisRepository;
    private final RecommendationRepository recommendationRepository;

    public AnalyticsService(IncidentRepository incidentRepository,
                            AiAnalysisRepository aiAnalysisRepository,
                            RecommendationRepository recommendationRepository) {
        this.incidentRepository = incidentRepository;
        this.aiAnalysisRepository = aiAnalysisRepository;
        this.recommendationRepository = recommendationRepository;
    }

    @Cacheable(value = CacheConfig.ANALYTICS_CACHE, key = "'overview-' + #days")
    public AnalyticsOverviewResponse getOverview(int days) {
        int rangeDays = sanitizeDays(days);
        Instant since = Instant.now().minus(Duration.ofDays(rangeDays));

        Double mttrRaw = incidentRepository.calculateResolvedMttrMinutes(since);
        double mttr = (mttrRaw != null && mttrRaw > 0) ? Math.round(mttrRaw * 10.0) / 10.0 : 0.0;

        Double mttdRaw = incidentRepository.calculateMttdMinutes(since);
        double mttd = (mttdRaw != null && mttdRaw > 0) ? Math.round(mttdRaw * 10.0) / 10.0 : 0.0;

        long slaMet = incidentRepository.countSlaMetSince(since);
        long slaBreached = incidentRepository.countSlaBreachedSince(since);
        long totalSlaEvaluated = slaMet + slaBreached;
        double slaRate = totalSlaEvaluated > 0
                ? Math.round(((double) slaMet / totalSlaEvaluated) * 1000.0) / 10.0
                : 100.0;

        long totalCreated = incidentRepository.countByCreatedAtGreaterThanEqual(since);
        long totalResolved = incidentRepository.countByResolvedAtGreaterThanEqual(since);
        long activeCount = incidentRepository.countByStatusIn(ACTIVE_STATUSES);

        Map<IncidentPriority, Long> priorityMap = new EnumMap<>(IncidentPriority.class);
        for (IncidentPriority p : IncidentPriority.values()) {
            priorityMap.put(p, 0L);
        }
        for (Object[] row : incidentRepository.countByPrioritySince(since)) {
            if (row != null && row.length >= 2 && row[0] instanceof IncidentPriority prio && row[1] instanceof Long cnt) {
                priorityMap.put(prio, cnt);
            }
        }

        return new AnalyticsOverviewResponse(
                rangeDays,
                mttr,
                mttd,
                slaRate,
                totalCreated,
                totalResolved,
                activeCount,
                priorityMap
        );
    }

    @Cacheable(value = CacheConfig.ANALYTICS_CACHE, key = "'volume-' + #days + '-' + #groupBy")
    public AnalyticsVolumeResponse getVolumeTrend(int days, String groupBy) {
        int rangeDays = sanitizeDays(days);
        String group = (groupBy != null && groupBy.equalsIgnoreCase("week")) ? "week" : "day";
        Instant since = Instant.now().minus(Duration.ofDays(rangeDays));

        List<VolumeTrendProjection> rawProjections = group.equals("week")
                ? incidentRepository.findVolumeTrendWeekly(since)
                : incidentRepository.findVolumeTrendDaily(since);

        List<VolumeTrendPoint> series = rawProjections.stream()
                .map(p -> new VolumeTrendPoint(
                        p.getTimestamp(),
                        p.getP1(),
                        p.getP2(),
                        p.getP3(),
                        p.getP4(),
                        p.getTotal()
                ))
                .toList();

        return new AnalyticsVolumeResponse(rangeDays, group, series);
    }

    @Cacheable(value = CacheConfig.ANALYTICS_CACHE, key = "'sla-' + #days")
    public AnalyticsSlaResponse getSlaPerformance(int days) {
        int rangeDays = sanitizeDays(days);
        Instant since = Instant.now().minus(Duration.ofDays(rangeDays));

        long slaMet = incidentRepository.countSlaMetSince(since);
        long slaBreached = incidentRepository.countSlaBreachedSince(since);
        long totalEvaluated = slaMet + slaBreached;
        double overallCompliance = totalEvaluated > 0
                ? Math.round(((double) slaMet / totalEvaluated) * 1000.0) / 10.0
                : 100.0;

        Map<IncidentPriority, PrioritySlaProjection> projectionMap = new EnumMap<>(IncidentPriority.class);
        for (PrioritySlaProjection proj : incidentRepository.findSlaBreakdownByPriority(since)) {
            try {
                IncidentPriority p = IncidentPriority.valueOf(proj.getPriority());
                projectionMap.put(p, proj);
            } catch (Exception ignored) {
            }
        }

        List<PrioritySlaBreakdown> byPriority = new ArrayList<>();
        for (IncidentPriority p : IncidentPriority.values()) {
            int targetMinutes = switch (p) {
                case P1 -> 240;
                case P2 -> 480;
                case P3 -> 4320;
                case P4 -> 10080;
            };

            PrioritySlaProjection proj = projectionMap.get(p);
            long met = proj != null ? proj.getMetCount() : 0;
            long breached = proj != null ? proj.getBreachedCount() : 0;
            long total = met + breached;
            double rate = total > 0 ? Math.round(((double) met / total) * 1000.0) / 10.0 : 100.0;

            byPriority.add(new PrioritySlaBreakdown(p, targetMinutes, met, breached, rate));
        }

        return new AnalyticsSlaResponse(rangeDays, overallCompliance, byPriority);
    }

    @Cacheable(value = CacheConfig.ANALYTICS_CACHE, key = "'causes-' + #days")
    public AnalyticsCausesResponse getCausesAndRecurrence(int days) {
        int rangeDays = sanitizeDays(days);
        Instant since = Instant.now().minus(Duration.ofDays(rangeDays));

        List<CategoryCountProjection> catProjections = incidentRepository.findCategoryDistribution(since);
        long totalCatIncidents = catProjections.stream().mapToLong(CategoryCountProjection::getCount).sum();

        List<CategoryDistributionItem> categories = catProjections.stream()
                .map(cp -> {
                    IncidentCategory cat = null;
                    try {
                        cat = IncidentCategory.valueOf(cp.getCategory());
                    } catch (Exception ignored) {}
                    double pct = totalCatIncidents > 0
                            ? Math.round(((double) cp.getCount() / totalCatIncidents) * 1000.0) / 10.0
                            : 0.0;
                    return new CategoryDistributionItem(cat, cp.getCount(), pct);
                })
                .filter(item -> item.category() != null)
                .toList();

        List<RecurringCauseProjection> recProjections = incidentRepository.findCausesAndRecurrence(since);
        List<RecurringCauseItem> recurringCauses = recProjections.stream()
                .map(rp -> {
                    IncidentCategory cat = null;
                    try {
                        cat = IncidentCategory.valueOf(rp.getCategory());
                    } catch (Exception ignored) {}
                    boolean isRecurring = rp.getIncidentCount() >= 2;
                    return new RecurringCauseItem(
                            rp.getServiceId(),
                            rp.getServiceName(),
                            cat,
                            rp.getIncidentCount(),
                            rp.getLastIncidentNumber(),
                            rp.getLastOccurredAt(),
                            isRecurring
                    );
                })
                .filter(item -> item.category() != null)
                .toList();

        return new AnalyticsCausesResponse(rangeDays, categories, recurringCauses);
    }

    @Cacheable(value = CacheConfig.ANALYTICS_CACHE, key = "'ai-feedback-' + #days")
    public AiFeedbackMetricsResponse getAiFeedbackMetrics(int days) {
        int rangeDays = sanitizeDays(days);
        Instant since = Instant.now().minus(Duration.ofDays(rangeDays));

        long totalAnalyses = aiAnalysisRepository.countByCreatedAtGreaterThanEqualAndStatus(since, AnalysisStatus.COMPLETED);
        long helpfulCount = recommendationRepository.countByCreatedAtGreaterThanEqualAndFeedbackRating(since, FeedbackRating.HELPFUL);
        long notHelpfulCount = recommendationRepository.countByCreatedAtGreaterThanEqualAndFeedbackRating(since, FeedbackRating.NOT_HELPFUL);
        long ratedCount = helpfulCount + notHelpfulCount;
        double helpfulRate = ratedCount > 0
                ? Math.round(((double) helpfulCount / ratedCount) * 1000.0) / 10.0
                : 0.0;

        long acceptedCount = recommendationRepository.countByCreatedAtGreaterThanEqualAndStatus(since, RecommendationStatus.ACCEPTED);
        long rejectedCount = recommendationRepository.countByCreatedAtGreaterThanEqualAndStatus(since, RecommendationStatus.REJECTED);
        long decidedCount = acceptedCount + rejectedCount;
        double acceptanceRate = decidedCount > 0
                ? Math.round(((double) acceptedCount / decidedCount) * 1000.0) / 10.0
                : 0.0;

        Double avgConfRaw = aiAnalysisRepository.getAvgConfidence(since, AnalysisStatus.COMPLETED);
        double avgConfidence = (avgConfRaw != null) ? Math.round(avgConfRaw * 100.0) / 100.0 : 0.0;

        Double avgLatRaw = aiAnalysisRepository.getAvgLatencyMs(since, AnalysisStatus.COMPLETED);
        double avgLatencyMs = (avgLatRaw != null) ? Math.round(avgLatRaw * 10.0) / 10.0 : 0.0;

        return new AiFeedbackMetricsResponse(
                rangeDays,
                totalAnalyses,
                helpfulCount,
                notHelpfulCount,
                helpfulRate,
                acceptedCount,
                rejectedCount,
                acceptanceRate,
                avgConfidence,
                avgLatencyMs
        );
    }

    private int sanitizeDays(int days) {
        if (days == 7 || days == 90) {
            return days;
        }
        return 30; // default to 30 days
    }
}
