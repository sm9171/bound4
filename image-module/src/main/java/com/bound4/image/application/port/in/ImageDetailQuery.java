package com.bound4.image.application.port.in;

public class ImageDetailQuery {
    
    private final Long imageId;
    
    public ImageDetailQuery(Long imageId) {
        this.imageId = imageId;
    }
    
    public Long getImageId() {
        return imageId;
    }
}