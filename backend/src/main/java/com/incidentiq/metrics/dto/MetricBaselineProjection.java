package com.incidentiq.metrics.dto;

public interface MetricBaselineProjection {
    String getMetricName();
    Long getSampleCount();
    Double getAvgValue();
    Double getStddevValue();
    Double getMaxValue();
    Double getMinValue();
}
