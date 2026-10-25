package com.incidentiq.metrics.dto;

import com.incidentiq.metrics.entity.MetricType;
import java.util.List;

public record MetricSeries(
        MetricType metricName,
        String displayName,
        String unit,
        Double current,
        Double min,
        Double max,
        Double avg,
        List<MetricDataPoint> points
) {}
