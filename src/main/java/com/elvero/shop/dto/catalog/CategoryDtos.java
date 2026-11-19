package com.elvero.shop.dto.catalog;

import java.util.List;

public final class CategoryDtos {

    private CategoryDtos() {}

    public record CreateCategoryRequest(
            String categoryName,
            Long departmentId,
            String description,
            String status,
            String slug
    ) {}

    public record UpdateCategoryRequest(
            String categoryName,
            Long departmentId,
            String description,
            String status
    ) {}

    public record CategoryResponse(
            Long id,
            String categoryName,
            String slug,
            String description,
            String image,
            String status,
            Long departmentId,
            String departmentName,
            List<SubcategorySummary> subcategories
    ) {}

    public record SubcategorySummary(
            Long id,
            String subcategoryName,
            String slug,
            String image,
            String status
    ) {}

    public record PageResponse<T>(
            List<T> data,
            Pagination pagination
    ) {}

    public record Pagination(
            int pageIndex,
            int pageSize,
            long totalRecords,
            int totalPages,
            boolean hasNextPage,
            boolean hasPreviousPage
    ) {}
}
