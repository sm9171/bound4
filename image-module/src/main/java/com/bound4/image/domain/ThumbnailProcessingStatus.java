package com.bound4.image.domain;

/**
 * 썸네일 처리 상태 열거형
 */
public enum ThumbnailProcessingStatus {
    
    /**
     * 썸네일 생성 대기 중
     */
    PENDING("pending", "썸네일 생성 대기 중"),
    
    /**
     * 썸네일 생성 중
     */
    PROCESSING("processing", "썸네일 생성 중"),
    
    /**
     * 썸네일 생성 완료
     */
    COMPLETED("completed", "썸네일 생성 완료"),
    
    /**
     * 썸네일 생성 실패 (재시도 가능)
     */
    FAILED_RETRYABLE("failed_retryable", "썸네일 생성 실패 (재시도 가능)"),
    
    /**
     * 썸네일 생성 실패 (재시도 불가)
     */
    FAILED_PERMANENT("failed_permanent", "썸네일 생성 실패 (재시도 불가)");
    
    private final String value;
    private final String description;
    
    ThumbnailProcessingStatus(String value, String description) {
        this.value = value;
        this.description = description;
    }
    
    public String getValue() {
        return value;
    }
    
    public String getDescription() {
        return description;
    }
    
    public boolean canRetry() {
        return this == FAILED_RETRYABLE;
    }
    
    public boolean isCompleted() {
        return this == COMPLETED;
    }
    
    public boolean isFailed() {
        return this == FAILED_RETRYABLE || this == FAILED_PERMANENT;
    }
    
    public boolean isInProgress() {
        return this == PENDING || this == PROCESSING;
    }
    
    public static ThumbnailProcessingStatus fromString(String value) {
        for (ThumbnailProcessingStatus status : values()) {
            if (status.value.equals(value)) {
                return status;
            }
        }
        throw new IllegalArgumentException("Unknown ThumbnailProcessingStatus: " + value);
    }
}