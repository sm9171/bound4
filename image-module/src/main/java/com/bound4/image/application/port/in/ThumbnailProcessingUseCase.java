package com.bound4.image.application.port.in;

import com.bound4.image.domain.ImageId;
import com.bound4.image.domain.ThumbnailProcessingStatus;

/**
 * 썸네일 처리 관련 UseCase
 */
public interface ThumbnailProcessingUseCase {
    
    /**
     * 썸네일 생성 요청
     * @param imageId 이미지 ID
     */
    void requestThumbnailGeneration(ImageId imageId);
    
    /**
     * 썸네일 처리 상태 조회
     * @param imageId 이미지 ID
     * @return 썸네일 처리 상태
     */
    ThumbnailProcessingStatus getThumbnailProcessingStatus(ImageId imageId);
    
    /**
     * 썸네일 생성 재시도
     * @param imageId 이미지 ID
     */
    void retryThumbnailGeneration(ImageId imageId);
}