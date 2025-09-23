package com.bound4.image.application.service;

import com.bound4.image.adapter.out.persistence.ImageQueryRepository;
import com.bound4.image.application.port.in.ImageListQuery;
import com.bound4.image.application.port.in.ImageListUseCase;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class ImageListService implements ImageListUseCase {
    
    private final ImageQueryRepository imageQueryRepository;
    
    public ImageListService(ImageQueryRepository imageQueryRepository) {
        this.imageQueryRepository = imageQueryRepository;
    }
    
    @Override
    public Page<ImageQueryRepository.ImageListProjection> getImageList(ImageListQuery query) {
        Pageable pageable = createPageable(query);
        
        return imageQueryRepository.findImagesByProjectId(
            query.getProjectId(),
            query.getStatus(),
            query.getTags(),
            pageable
        );
    }
    
    private Pageable createPageable(ImageListQuery query) {
        Sort sort = createSort(query.getSort(), query.getDirection());
        return PageRequest.of(query.getPage(), query.getSize(), sort);
    }
    
    private Sort createSort(String sortProperty, String direction) {
        Sort.Direction sortDirection = "asc".equalsIgnoreCase(direction) ? 
            Sort.Direction.ASC : Sort.Direction.DESC;
        
        String property = validateAndGetSortProperty(sortProperty);
        return Sort.by(sortDirection, property);
    }
    
    private String validateAndGetSortProperty(String sortProperty) {
        if (sortProperty == null) {
            return "createdAt";
        }
        
        switch (sortProperty.toLowerCase()) {
            case "createdat":
            case "created_at":
                return "createdAt";
            case "updatedat":
            case "updated_at":
                return "updatedAt";
            case "filename":
            case "originalfilename":
                return "filename";
            default:
                return "createdAt";
        }
    }
}