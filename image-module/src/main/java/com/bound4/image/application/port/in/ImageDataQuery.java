package com.bound4.image.application.port.in;

public class ImageDataQuery {
    
    private final Long imageId;
    private final ImageDataType dataType;
    
    public ImageDataQuery(Long imageId, ImageDataType dataType) {
        this.imageId = imageId;
        this.dataType = dataType;
    }
    
    public Long getImageId() {
        return imageId;
    }
    
    public ImageDataType getDataType() {
        return dataType;
    }
    
    public enum ImageDataType {
        ORIGINAL, THUMBNAIL
    }
}