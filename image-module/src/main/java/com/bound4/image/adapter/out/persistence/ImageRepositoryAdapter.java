package com.bound4.image.adapter.out.persistence;

import com.bound4.image.application.port.out.ImageRepository;
import com.bound4.image.domain.FileHash;
import com.bound4.image.domain.Image;
import com.bound4.image.domain.ImageId;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public class ImageRepositoryAdapter implements ImageRepository {
    
    private final ImageJpaRepository jpaRepository;
    private final ImageMapper mapper;
    
    public ImageRepositoryAdapter(ImageJpaRepository jpaRepository, ImageMapper mapper) {
        this.jpaRepository = jpaRepository;
        this.mapper = mapper;
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
    public Image updateWithOptimisticLock(Image image) {
        ImageEntity entity = mapper.toEntity(image);
        ImageEntity savedEntity = jpaRepository.save(entity);
        return mapper.toDomain(savedEntity);
    }
}