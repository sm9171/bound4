package com.bound4.image.application.service;

import com.bound4.image.adapter.out.persistence.ImageQueryRepository;
import com.bound4.image.application.port.in.ImageCursorListQuery;
import com.bound4.image.application.port.in.ImageCursorListUseCase;
import com.bound4.image.application.port.in.ImageListQuery;
import com.bound4.image.application.port.in.ImageListUseCase;
import com.bound4.image.application.port.out.ImageRepository;
import com.bound4.image.domain.Image;
import org.springframework.data.domain.Page;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.Instant;

@Service
@Transactional(readOnly = true)
public class ImageCursorListService implements ImageCursorListUseCase {
    
    private static final Logger logger = LoggerFactory.getLogger(ImageCursorListService.class);
    
    private final ImageRepository imageRepository;
    private final ImageListUseCase imageListUseCase;
    
    public ImageCursorListService(ImageRepository imageRepository, ImageListUseCase imageListUseCase) {
        this.imageRepository = imageRepository;
        this.imageListUseCase = imageListUseCase;
    }
    
    @Override
    public CursorPageResult<Image> getImagesCursorBased(ImageCursorListQuery query) {
        logger.info("Executing cursor-based pagination query for project: {}, size: {}, cursor: {}", 
                   query.getProjectId(), query.getSize(), 
                   query.getCursor() != null ? query.getCursor().getEncodedValue() : "null");
        
        Instant startTime = Instant.now();
        
        try {
            CursorPageResult<Image> result = imageRepository.findImagesByCursor(query);
            
            Instant endTime = Instant.now();
            Duration duration = Duration.between(startTime, endTime);
            
            logger.info("Cursor pagination completed in {}ms for project: {}, returned {} items", 
                       duration.toMillis(), query.getProjectId(), result.getContent().size());
            
            return result;
            
        } catch (Exception e) {
            logger.error("Error executing cursor-based pagination query for project: {}", 
                        query.getProjectId(), e);
            throw new RuntimeException("Failed to execute cursor-based pagination", e);
        }
    }
    
    @Override
    public PagingPerformanceMetrics comparePerformance(ImageListQuery offsetQuery, ImageCursorListQuery cursorQuery) {
        logger.info("Starting pagination performance comparison for project: {}", offsetQuery.getProjectId());
        
        // Offset 페이징 성능 측정
        Instant offsetStart = Instant.now();
        Page<ImageQueryRepository.ImageListProjection> offsetResult;
        try {
            offsetResult = imageListUseCase.getImageList(offsetQuery);
        } catch (Exception e) {
            logger.error("Error executing offset pagination", e);
            throw new RuntimeException("Failed to execute offset pagination for comparison", e);
        }
        Instant offsetEnd = Instant.now();
        Duration offsetDuration = Duration.between(offsetStart, offsetEnd);
        
        // Cursor 페이징 성능 측정
        Instant cursorStart = Instant.now();
        CursorPageResult<Image> cursorResult;
        try {
            cursorResult = getImagesCursorBased(cursorQuery);
        } catch (Exception e) {
            logger.error("Error executing cursor pagination", e);
            throw new RuntimeException("Failed to execute cursor pagination for comparison", e);
        }
        Instant cursorEnd = Instant.now();
        Duration cursorDuration = Duration.between(cursorStart, cursorEnd);
        
        // 쿼리 복잡도 분석
        String complexity = analyzeQueryComplexity(offsetQuery, cursorQuery);
        
        PagingPerformanceMetrics metrics = new PagingPerformanceMetrics(
            offsetDuration,
            cursorDuration,
            offsetResult.getNumberOfElements(),
            cursorResult.getContent().size(),
            complexity
        );
        
        logger.info("Performance comparison completed - Offset: {}ms, Cursor: {}ms, Improvement: {:.2f}%",
                   offsetDuration.toMillis(), cursorDuration.toMillis(), metrics.getPerformanceImprovement());
        
        return metrics;
    }
    
    private String analyzeQueryComplexity(ImageListQuery offsetQuery, ImageCursorListQuery cursorQuery) {
        StringBuilder complexity = new StringBuilder();
        
        // 필터 조건 분석
        boolean hasStatusFilter = offsetQuery.getStatus() != null || cursorQuery.getStatus() != null;
        boolean hasTagsFilter = (offsetQuery.getTags() != null && !offsetQuery.getTags().isEmpty()) ||
                               (cursorQuery.getTags() != null && !cursorQuery.getTags().isEmpty());
        
        if (hasStatusFilter) {
            complexity.append("STATUS_FILTER,");
        }
        if (hasTagsFilter) {
            complexity.append("TAGS_FILTER,");
        }
        
        // 페이지 위치 분석
        if (offsetQuery.getPage() > 10) {
            complexity.append("DEEP_OFFSET,");
        }
        
        if (cursorQuery.getCursor() != null) {
            complexity.append("CURSOR_NAVIGATION,");
        }
        
        // 정렬 조건 분석
        complexity.append("SORT_").append(cursorQuery.getSortBy().toUpperCase()).append(",");
        complexity.append("DIR_").append(cursorQuery.getDirection().getValue().toUpperCase());
        
        return complexity.toString();
    }
}