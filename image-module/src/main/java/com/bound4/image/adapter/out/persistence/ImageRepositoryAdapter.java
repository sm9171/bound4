package com.bound4.image.adapter.out.persistence;

import com.bound4.image.application.port.in.ImageCursorListQuery;
import com.bound4.image.application.port.in.ImageCursorListUseCase;
import com.bound4.image.application.port.out.ImageRepository;
import com.bound4.image.domain.*;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public class ImageRepositoryAdapter implements ImageRepository {
    
    private final ImageJpaRepository jpaRepository;
    private final ImageMapper mapper;
    private final ImageQueryRepository queryRepository;
    
    public ImageRepositoryAdapter(ImageJpaRepository jpaRepository, ImageMapper mapper, ImageQueryRepository queryRepository) {
        this.jpaRepository = jpaRepository;
        this.mapper = mapper;
        this.queryRepository = queryRepository;
    }
    
    @Override
    public Image save(Image image) {
        ImageEntity entity = mapper.toEntity(image);
        ImageEntity savedEntity = jpaRepository.save(entity);
        return mapper.toDomain(savedEntity);
    }

    @Override
    public Optional<Image> findByHash(FileHash fileHash) {
        return jpaRepository.findByFileHash(fileHash.value())
                .map(mapper::toDomain);
    }
    
    @Override
    public Optional<Image> findById(ImageId imageId) {
        return jpaRepository.findByIdAndDeletedAtIsNull(imageId.value())
                .map(mapper::toDomain);
    }
    
    @Override
    public ImageCursorListUseCase.CursorPageResult<Image> findImagesByCursor(ImageCursorListQuery query) {
        ImageQueryRepository.CursorPageResult<ImageEntity> entityResult = queryRepository.findImagesByCursor(query);
        
        List<Image> images = entityResult.getContent().stream()
                .map(mapper::toDomain)
                .toList();
        
        Cursor nextCursor = null;
        Cursor previousCursor = null;
        
        if (entityResult.hasNext() && !images.isEmpty()) {
            Image lastImage = images.get(images.size() - 1);
            nextCursor = Cursor.of(lastImage.getId().value(), getTimestampForSort(lastImage, query.getSortBy()));
        }
        
        if (entityResult.hasPrevious() && !images.isEmpty()) {
            Image firstImage = images.get(0);
            previousCursor = Cursor.of(firstImage.getId().value(), getTimestampForSort(firstImage, query.getSortBy()));
        }
        
        PageInfo pageInfo = PageInfo.of(
            entityResult.hasNext(),
            entityResult.hasPrevious(),
            nextCursor,
            previousCursor,
            entityResult.getRequestedSize(),
            entityResult.getActualSize()
        );
        
        return new ImageCursorListUseCase.CursorPageResult<>(images, pageInfo);
    }
    
    private java.time.LocalDateTime getTimestampForSort(Image image, String sortBy) {
        if ("updatedAt".equals(sortBy)) {
            return image.getUpdatedAt();
        }
        return image.getCreatedAt(); // 기본값
    }
}