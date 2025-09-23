package com.bound4.image.application.port.out;

import com.bound4.image.application.port.in.ImageCursorListQuery;
import com.bound4.image.application.port.in.ImageCursorListUseCase;
import com.bound4.image.domain.FileHash;
import com.bound4.image.domain.Image;
import com.bound4.image.domain.ImageId;

import java.util.Optional;

public interface ImageRepository {
    Image save(Image image);
    Optional<Image> findByHash(FileHash fileHash);
    Optional<Image> findById(ImageId imageId);

    ImageCursorListUseCase.CursorPageResult<Image> findImagesByCursor(ImageCursorListQuery query);
}