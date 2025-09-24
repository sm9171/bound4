package com.bound4.image.adapter.out.storage;

import java.time.LocalDateTime;

public class MockS3Object {
    private final String key;
    private final byte[] data;
    private final String contentType;
    private final LocalDateTime uploadTime;
    private final long size;

    private MockS3Object(Builder builder) {
        this.key = builder.key;
        this.data = builder.data;
        this.contentType = builder.contentType;
        this.uploadTime = builder.uploadTime;
        this.size = builder.size;
    }

    public static Builder builder() {
        return new Builder();
    }

    public String getKey() {
        return key;
    }

    public byte[] getData() {
        return data.clone();
    }

    public String getContentType() {
        return contentType;
    }

    public LocalDateTime getUploadTime() {
        return uploadTime;
    }

    public long getSize() {
        return size;
    }

    public static class Builder {
        private String key;
        private byte[] data;
        private String contentType;
        private LocalDateTime uploadTime;
        private long size;

        public Builder key(String key) {
            this.key = key;
            return this;
        }

        public Builder data(byte[] data) {
            this.data = data.clone();
            this.size = data.length;
            return this;
        }

        public Builder contentType(String contentType) {
            this.contentType = contentType;
            return this;
        }

        public Builder uploadTime(LocalDateTime uploadTime) {
            this.uploadTime = uploadTime;
            return this;
        }

        public MockS3Object build() {
            if (key == null || data == null || contentType == null) {
                throw new IllegalArgumentException("Key, data, and contentType are required");
            }
            if (uploadTime == null) {
                uploadTime = LocalDateTime.now();
            }
            return new MockS3Object(this);
        }
    }
}