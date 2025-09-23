package com.bound4.image.application.port.in;

public interface ImagePresignedUrlUseCase {
    
    String generatePresignedUrl(ImagePresignedUrlQuery query);
}