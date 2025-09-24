package com.bound4.image.application.port.out;

import com.bound4.image.domain.ImageId;

/**
 * 썸네일 생성을 위한 외부 서비스 포트
 */
public interface ThumbnailGenerationService {
    
    /**
     * 이미지로부터 썸네일을 생성하고 저장소에 업로드
     * @param imageId 이미지 ID
     * @param originalImageKey 원본 이미지 키
     * @param mimeType MIME 타입
     * @return 생성된 썸네일의 저장소 키
     * @throws ThumbnailGenerationException 썸네일 생성 실패 시
     */
    String generateThumbnail(ImageId imageId, String originalImageKey, String mimeType) 
            throws ThumbnailGenerationException;
    
    /**
     * 썸네일 생성 지원 여부 확인
     * @param mimeType MIME 타입
     * @return 지원 여부
     */
    boolean isSupported(String mimeType);
    
    /**
     * 썸네일 생성 예외
     */
    class ThumbnailGenerationException extends Exception {
        private final boolean retryable;
        
        public ThumbnailGenerationException(String message, boolean retryable) {
            super(message);
            this.retryable = retryable;
        }
        
        public ThumbnailGenerationException(String message, Throwable cause, boolean retryable) {
            super(message, cause);
            this.retryable = retryable;
        }
        
        public boolean isRetryable() {
            return retryable;
        }
    }
}