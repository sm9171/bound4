package com.bound4.image.application.port.in;

import com.bound4.image.domain.ProjectId;

public record UploadImageCommand(
    ProjectId projectId,
    String originalFilename,
    String mimeType,
    byte[] data
) {
    public UploadImageCommand {
        if (projectId == null) {
            throw new IllegalArgumentException("ProjectId cannot be null");
        }
        if (originalFilename == null || originalFilename.trim().isEmpty()) {
            throw new IllegalArgumentException("Original filename cannot be null or empty");
        }
        if (mimeType == null || mimeType.trim().isEmpty()) {
            throw new IllegalArgumentException("MIME type cannot be null or empty");
        }
        if (data == null || data.length == 0) {
            throw new IllegalArgumentException("Image data cannot be null or empty");
        }
    }
}