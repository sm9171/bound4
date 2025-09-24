package com.bound4.image.adapter.in.web;

import com.bound4.image.application.port.in.ImageCursorListUseCase;

public class PagingPerformanceResponse {
    
    private boolean success;
    private PerformanceData data;
    private String message;
    
    public PagingPerformanceResponse(boolean success, PerformanceData data, String message) {
        this.success = success;
        this.data = data;
        this.message = message;
    }
    
    public static PagingPerformanceResponse success(ImageCursorListUseCase.PagingPerformanceMetrics metrics) {
        PerformanceData data = new PerformanceData(
            metrics.getOffsetPagingDuration().toMillis(),
            metrics.getCursorPagingDuration().toMillis(),
            metrics.getOffsetResultCount(),
            metrics.getCursorResultCount(),
            metrics.getQueryComplexity(),
            metrics.getPerformanceImprovement()
        );
        
        return new PagingPerformanceResponse(true, data, null);
    }
    
    public static PagingPerformanceResponse error(String message) {
        return new PagingPerformanceResponse(false, null, message);
    }
    
    public boolean isSuccess() {
        return success;
    }
    
    public PerformanceData getData() {
        return data;
    }
    
    public String getMessage() {
        return message;
    }
    
    public static class PerformanceData {
        private long offsetPagingDurationMs;
        private long cursorPagingDurationMs;
        private long offsetResultCount;
        private long cursorResultCount;
        private String queryComplexity;
        private double performanceImprovementPercent;
        
        public PerformanceData(long offsetPagingDurationMs, long cursorPagingDurationMs,
                             long offsetResultCount, long cursorResultCount, 
                             String queryComplexity, double performanceImprovementPercent) {
            this.offsetPagingDurationMs = offsetPagingDurationMs;
            this.cursorPagingDurationMs = cursorPagingDurationMs;
            this.offsetResultCount = offsetResultCount;
            this.cursorResultCount = cursorResultCount;
            this.queryComplexity = queryComplexity;
            this.performanceImprovementPercent = performanceImprovementPercent;
        }
        
        // Getters
        public long getOffsetPagingDurationMs() { return offsetPagingDurationMs; }
        public long getCursorPagingDurationMs() { return cursorPagingDurationMs; }
        public long getOffsetResultCount() { return offsetResultCount; }
        public long getCursorResultCount() { return cursorResultCount; }
        public String getQueryComplexity() { return queryComplexity; }
        public double getPerformanceImprovementPercent() { return performanceImprovementPercent; }
        
        public String getPerformanceSummary() {
            if (performanceImprovementPercent > 0) {
                return String.format("Cursor pagination is %.2f%% faster", performanceImprovementPercent);
            } else if (performanceImprovementPercent < 0) {
                return String.format("Offset pagination is %.2f%% faster", Math.abs(performanceImprovementPercent));
            } else {
                return "Both pagination methods have similar performance";
            }
        }
    }
}