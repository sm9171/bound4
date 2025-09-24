package com.bound4.image.adapter.out.persistence;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface ImageJpaRepository extends JpaRepository<ImageEntity, Long> {
    
    Optional<ImageEntity> findByFileHash(String fileHash);
    Optional<ImageEntity> findByIdAndDeletedAtIsNull(Long id);
}