package com.bound4.image.application.port.in;

import com.bound4.image.domain.Image;

public interface ImageDetailUseCase {
    
    Image getImageDetail(ImageDetailQuery query);
}