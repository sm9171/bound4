package com.bound4.image.adapter.in.web;

import com.bound4.image.domain.ImageStatus;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;

public class ImageListRequest {
    
    @Min(value = 0, message = "page는 0 이상이어야 합니다")
    private int page = 0;
    
    @Min(value = 1, message = "size는 1 이상이어야 합니다")
    @Max(value = 100, message = "size는 100 이하여야 합니다")
    private int size = 20;
    
    private ImageStatus status;
    
    private String tags;
    
    private String sort = "createdAt";
    
    private String direction = "desc";
    
    public int getPage() {
        return page;
    }
    
    public int getSize() {
        return size;
    }
    
    public ImageStatus getStatus() {
        return status;
    }
    
    public String getTags() {
        return tags;
    }
    
    public String getSort() {
        return sort;
    }
    
    public String getDirection() {
        return direction;
    }
}