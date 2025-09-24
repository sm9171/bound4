package com.bound4.image.application.port.in;

import com.bound4.image.domain.Image;
import com.bound4.image.domain.PageInfo;

import java.time.Duration;
import java.util.List;

public interface ImageCursorListUseCase {
    
    CursorPageResult<Image> getImagesCursorBased(ImageCursorListQuery query);
    
    PagingPerformanceMetrics comparePerformance(ImageListQuery offsetQuery, ImageCursorListQuery cursorQuery);
    
    class CursorPageResult<T> {
        private final List<T> content;
        private final PageInfo pageInfo;
        
        public CursorPageResult(List<T> content, PageInfo pageInfo) {
            this.content = content;
            this.pageInfo = pageInfo;
        }
        
        public List<T> getContent() {
            return content;
        }
        
        public PageInfo getPageInfo() {
            return pageInfo;
        }
    }
    
    class PagingPerformanceMetrics {
        private final Duration offsetPagingDuration;
        private final Duration cursorPagingDuration;
        private final long offsetResultCount;
        private final long cursorResultCount;
        private final String queryComplexity;
        
        public PagingPerformanceMetrics(Duration offsetPagingDuration, Duration cursorPagingDuration,
                                      long offsetResultCount, long cursorResultCount, String queryComplexity) {
            this.offsetPagingDuration = offsetPagingDuration;
            this.cursorPagingDuration = cursorPagingDuration;
            this.offsetResultCount = offsetResultCount;
            this.cursorResultCount = cursorResultCount;
            this.queryComplexity = queryComplexity;
        }
        
        public Duration getOffsetPagingDuration() {
            return offsetPagingDuration;
        }
        
        public Duration getCursorPagingDuration() {
            return cursorPagingDuration;
        }
        
        public long getOffsetResultCount() {
            return offsetResultCount;
        }
        
        public long getCursorResultCount() {
            return cursorResultCount;
        }
        
        public String getQueryComplexity() {
            return queryComplexity;
        }
        
        public double getPerformanceImprovement() {
            if (offsetPagingDuration.isZero()) {
                return 0.0;
            }
            
            double offsetMs = offsetPagingDuration.toNanos() / 1_000_000.0;
            double cursorMs = cursorPagingDuration.toNanos() / 1_000_000.0;
            
            return ((offsetMs - cursorMs) / offsetMs) * 100.0;
        }
    }
}