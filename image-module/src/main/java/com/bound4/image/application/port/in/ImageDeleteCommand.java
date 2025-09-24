package com.bound4.image.application.port.in;

public class ImageDeleteCommand {
    
    private final Long imageId;
    
    public ImageDeleteCommand(Long imageId) {
        this.imageId = imageId;
    }
    
    public Long getImageId() {
        return imageId;
    }
}