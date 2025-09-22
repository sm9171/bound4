package com.bound4.image.application.port.out;

import com.bound4.image.domain.FileHash;
import com.bound4.image.domain.Image;

import java.util.Optional;

public interface ImageRepository {
    Image save(Image image);
    Optional<Image> findByHash(FileHash fileHash);
}