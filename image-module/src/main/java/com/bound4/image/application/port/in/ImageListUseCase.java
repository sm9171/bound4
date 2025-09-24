package com.bound4.image.application.port.in;

import com.bound4.image.adapter.out.persistence.ImageQueryRepository;
import org.springframework.data.domain.Page;

public interface ImageListUseCase {
    
    Page<ImageQueryRepository.ImageListProjection> getImageList(ImageListQuery query);
}