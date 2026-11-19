package com.elvero.shop.dto.catalog;

import lombok.*;


public final class SubcategoryDtos {

    private SubcategoryDtos() {}

    public record CreateSubcategoryRequest(
            Long categoryId,
            String subcategoryName,
            String status
    ) {}

    public record UpdateSubcategoryRequest(
            Long categoryId,
            String subcategoryName,
            String status
    ) {}
    public record SubcategoryResponse(
            Long id,
            Long categoryId,
            String categoryName,
            String subcategoryName,
            String slug,
            String image,
            String status,
            Long productCount
    ) {}
}
