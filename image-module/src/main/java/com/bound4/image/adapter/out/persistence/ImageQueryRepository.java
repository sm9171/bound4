package com.bound4.image.adapter.out.persistence;

import com.bound4.image.domain.ImageStatus;
import com.querydsl.core.BooleanBuilder;
import com.querydsl.core.types.OrderSpecifier;
import com.querydsl.core.types.Projections;
import com.querydsl.jpa.impl.JPAQuery;
import com.querydsl.jpa.impl.JPAQueryFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Repository;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

@Repository
public class ImageQueryRepository {
    
    private final JPAQueryFactory queryFactory;
    private final QImageEntity qImage = QImageEntity.imageEntity;
    
    public ImageQueryRepository(JPAQueryFactory queryFactory) {
        this.queryFactory = queryFactory;
    }
    
    public Page<ImageListProjection> findImagesByProjectId(Long projectId, ImageStatus status, 
                                                          List<String> tags, Pageable pageable) {
        BooleanBuilder predicate = new BooleanBuilder();
        
        predicate.and(qImage.projectId.eq(projectId));
        predicate.and(qImage.deletedAt.isNull());
        
        if (status != null) {
            predicate.and(qImage.status.eq(status));
        }
        
        if (tags != null && !tags.isEmpty()) {
            BooleanBuilder tagPredicate = new BooleanBuilder();
            for (String tag : tags) {
                tagPredicate.or(qImage.tags.contains(tag));
            }
            predicate.and(tagPredicate);
        }
        
        JPAQuery<ImageListProjection> query = queryFactory
            .select(Projections.constructor(ImageListProjection.class,
                qImage.id,
                qImage.projectId,
                qImage.originalFilename,
                qImage.fileSize,
                qImage.mimeType,
                qImage.status,
                qImage.tags,
                qImage.memo,
                qImage.createdAt
            ))
            .from(qImage)
            .where(predicate);
        
        List<OrderSpecifier<?>> orders = buildOrderSpecifiers(pageable.getSort());
        for (OrderSpecifier<?> order : orders) {
            query.orderBy(order);
        }
        
        long total = queryFactory
            .select(qImage.count())
            .from(qImage)
            .where(predicate)
            .fetchOne();
        
        List<ImageListProjection> content = query
            .offset(pageable.getOffset())
            .limit(pageable.getPageSize())
            .fetch();
        
        return new PageImpl<>(content, pageable, total);
    }
    
    private List<OrderSpecifier<?>> buildOrderSpecifiers(Sort sort) {
        List<OrderSpecifier<?>> orders = new ArrayList<>();
        
        if (sort.isEmpty()) {
            orders.add(qImage.createdAt.desc());
            return orders;
        }
        
        for (Sort.Order order : sort) {
            switch (order.getProperty()) {
                case "createdAt":
                    orders.add(order.isAscending() ? qImage.createdAt.asc() : qImage.createdAt.desc());
                    break;
                case "updatedAt":
                    orders.add(order.isAscending() ? qImage.updatedAt.asc() : qImage.updatedAt.desc());
                    break;
                case "filename":
                    orders.add(order.isAscending() ? qImage.originalFilename.asc() : qImage.originalFilename.desc());
                    break;
                default:
                    orders.add(qImage.createdAt.desc());
                    break;
            }
        }
        
        return orders;
    }
    
    public static class ImageListProjection {
        private final Long id;
        private final Long projectId;
        private final String filename;
        private final Long fileSize;
        private final String mimeType;
        private final ImageStatus status;
        private final String tags;
        private final String memo;
        private final java.time.LocalDateTime createdAt;
        
        public ImageListProjection(Long id, Long projectId, String filename, Long fileSize, 
                                 String mimeType, ImageStatus status, String tags, String memo, 
                                 java.time.LocalDateTime createdAt) {
            this.id = id;
            this.projectId = projectId;
            this.filename = filename;
            this.fileSize = fileSize;
            this.mimeType = mimeType;
            this.status = status;
            this.tags = tags;
            this.memo = memo;
            this.createdAt = createdAt;
        }
        
        public Long getId() {
            return id;
        }
        
        public Long getProjectId() {
            return projectId;
        }
        
        public String getFilename() {
            return filename;
        }
        
        public Long getFileSize() {
            return fileSize;
        }
        
        public String getMimeType() {
            return mimeType;
        }
        
        public ImageStatus getStatus() {
            return status;
        }
        
        public List<String> getTags() {
            if (tags == null || tags.trim().isEmpty()) {
                return new ArrayList<>();
            }
            return Arrays.asList(tags.split(","));
        }
        
        public String getMemo() {
            return memo;
        }
        
        public java.time.LocalDateTime getCreatedAt() {
            return createdAt;
        }
    }
}