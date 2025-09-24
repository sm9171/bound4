package com.bound4.image.adapter.out.persistence;

import com.bound4.image.application.port.in.ImageCursorListQuery;
import com.bound4.image.domain.Cursor;
import com.bound4.image.domain.ImageStatus;
import com.bound4.image.domain.SortDirection;
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

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

@Repository
public class ImageQueryRepository {

    public static final String UPDATED_AT = "updatedAt";
    public static final String CREATED_AT = "createdAt";
    private final JPAQueryFactory queryFactory;
    private static final QImageEntity qImage = QImageEntity.imageEntity;
    
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
    
    public CursorPageResult<ImageEntity> findImagesByCursor(ImageCursorListQuery query) {
        BooleanBuilder predicate = new BooleanBuilder();
        
        predicate.and(qImage.projectId.eq(query.getProjectId()));
        predicate.and(qImage.deletedAt.isNull());
        
        if (query.getStatus() != null) {
            predicate.and(qImage.status.eq(query.getStatus()));
        }
        
        if (query.getTags() != null && !query.getTags().isEmpty()) {
            BooleanBuilder tagPredicate = new BooleanBuilder();
            for (String tag : query.getTags()) {
                tagPredicate.or(qImage.tags.contains(tag));
            }
            predicate.and(tagPredicate);
        }
        
        // Cursor 조건 추가
        if (query.getCursor() != null) {
            addCursorCondition(predicate, query.getCursor(), query.getDirection(), query.getSortBy());
        }
        
        JPAQuery<ImageEntity> mainQuery = queryFactory
            .selectFrom(qImage)
            .where(predicate);
        
        // 정렬 조건 추가
        addCursorOrderBy(mainQuery, query.getDirection(), query.getSortBy());
        
        // size + 1 조회하여 hasNext 판단
        List<ImageEntity> content = mainQuery
            .limit(query.getSize() + 1L)
            .fetch();
            
        boolean hasNext = content.size() > query.getSize();
        if (hasNext) {
            content = content.subList(0, query.getSize());
        }
        
        // Previous 페이지 존재 여부 확인
        boolean hasPrevious = query.getCursor() != null;
        
        return new CursorPageResult<>(content, hasNext, hasPrevious, query.getSize());
    }
    
    private void addCursorCondition(BooleanBuilder predicate, Cursor cursor, SortDirection direction, String sortBy) {
        LocalDateTime cursorTimestamp = cursor.getTimestamp();
        Long cursorId = cursor.getId();
        
        if (CREATED_AT.equals(sortBy)) {
            if (direction == SortDirection.DESC) {
                // 다음 페이지: createdAt < cursor OR (createdAt = cursor AND id < cursorId)
                predicate.and(
                    qImage.createdAt.lt(cursorTimestamp)
                    .or(qImage.createdAt.eq(cursorTimestamp).and(qImage.id.lt(cursorId)))
                );
            } else {
                // 이전 페이지: createdAt > cursor OR (createdAt = cursor AND id > cursorId)
                predicate.and(
                    qImage.createdAt.gt(cursorTimestamp)
                    .or(qImage.createdAt.eq(cursorTimestamp).and(qImage.id.gt(cursorId)))
                );
            }
        } else if (UPDATED_AT.equals(sortBy)) {
            if (direction == SortDirection.DESC) {
                predicate.and(
                    qImage.updatedAt.lt(cursorTimestamp)
                    .or(qImage.updatedAt.eq(cursorTimestamp).and(qImage.id.lt(cursorId)))
                );
            } else {
                predicate.and(
                    qImage.updatedAt.gt(cursorTimestamp)
                    .or(qImage.updatedAt.eq(cursorTimestamp).and(qImage.id.gt(cursorId)))
                );
            }
        }
    }
    
    private void addCursorOrderBy(JPAQuery<ImageEntity> query, SortDirection direction, String sortBy) {
        boolean isAsc = direction == SortDirection.ASC;
        
        if (UPDATED_AT.equals(sortBy)) {
            query.orderBy(isAsc ? qImage.updatedAt.asc() : qImage.updatedAt.desc());
            query.orderBy(isAsc ? qImage.id.asc() : qImage.id.desc());
        } else {
            // 기본값은 createdAt
            query.orderBy(isAsc ? qImage.createdAt.asc() : qImage.createdAt.desc());
            query.orderBy(isAsc ? qImage.id.asc() : qImage.id.desc());
        }
    }
    
    public long countImagesByProjectId(Long projectId, ImageStatus status, List<String> tags) {
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
        
        return queryFactory
            .select(qImage.count())
            .from(qImage)
            .where(predicate)
            .fetchOne();
    }
    
    private List<OrderSpecifier<?>> buildOrderSpecifiers(Sort sort) {
        List<OrderSpecifier<?>> orders = new ArrayList<>();
        
        if (sort.isEmpty()) {
            orders.add(qImage.createdAt.desc());
            return orders;
        }
        
        for (Sort.Order order : sort) {
            switch (order.getProperty()) {
                case CREATED_AT:
                    orders.add(order.isAscending() ? qImage.createdAt.asc() : qImage.createdAt.desc());
                    break;
                case UPDATED_AT:
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
    
    public static class CursorPageResult<T> {
        private final List<T> content;
        private final boolean hasNext;
        private final boolean hasPrevious;
        private final int requestedSize;
        
        public CursorPageResult(List<T> content, boolean hasNext, boolean hasPrevious, int requestedSize) {
            this.content = content;
            this.hasNext = hasNext;
            this.hasPrevious = hasPrevious;
            this.requestedSize = requestedSize;
        }
        
        public List<T> getContent() {
            return content;
        }
        
        public boolean hasNext() {
            return hasNext;
        }
        
        public boolean hasPrevious() {
            return hasPrevious;
        }
        
        public int getRequestedSize() {
            return requestedSize;
        }
        
        public int getActualSize() {
            return content.size();
        }
    }
}