package com.bound4.image.application.port.in;

import com.bound4.image.domain.ImageData;

public interface ImageDataUseCase {
    
    ImageDataResponse getImageData(ImageDataQuery query);
    
    class ImageDataResponse {
        private final ImageData imageData;
        private final String mimeType;
        private final String filename;
        
        public ImageDataResponse(ImageData imageData, String mimeType, String filename) {
            this.imageData = imageData;
            this.mimeType = mimeType;
            this.filename = filename;
        }
        
        public ImageData getImageData() {
            return imageData;
        }
        
        public String getMimeType() {
            return mimeType;
        }
        
        public String getFilename() {
            return filename;
        }
    }
}