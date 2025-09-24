package com.bound4.image.application.port.out;

import com.bound4.image.domain.ImageData;

public interface ThumbnailService {
    ImageData generateThumbnail(ImageData originalImage, String mimeType);
}