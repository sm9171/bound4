package com.bound4.image.application.port.in;

import java.time.Duration;

public class ImagePresignedUrlQuery {
    private final Long imageId;
    private final ImageDataType dataType;
    private final Duration expiration;

    public ImagePresignedUrlQuery(Long imageId, ImageDataType dataType, Duration expiration) {
        this.imageId = imageId;
        this.dataType = dataType;
        this.expiration = expiration;
    }

    public Long getImageId() {
        return imageId;
    }

    public ImageDataType getDataType() {
        return dataType;
    }

    public Duration getExpiration() {
        return expiration;
    }

    public enum ImageDataType {
        ORIGINAL, THUMBNAIL
    }
}