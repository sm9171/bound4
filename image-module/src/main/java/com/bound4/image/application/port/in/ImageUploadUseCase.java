package com.bound4.image.application.port.in;

import com.bound4.image.domain.Image;

import java.util.List;

public interface ImageUploadUseCase {
    List<Image> uploadImages(List<UploadImageCommand> commands);
}