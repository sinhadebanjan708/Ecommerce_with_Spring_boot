package com.elvero.shop.controller;

import com.elvero.shop.common.ApiResponse;
import com.elvero.shop.dto.catalog.CategoryDtos.CategoryResponse;
import com.elvero.shop.dto.catalog.CategoryDtos.CreateCategoryRequest;
import com.elvero.shop.dto.catalog.CategoryDtos.PageResponse;
import com.elvero.shop.dto.catalog.CategoryDtos.UpdateCategoryRequest;
import com.elvero.shop.entity.Category;
import com.elvero.shop.service.CategoryService;
import com.elvero.shop.service.FileStorageService;
import java.util.Map;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/ecommerce/categories")
public class CategoryController {

    private final CategoryService categoryService;
    private final FileStorageService fileStorageService;

    public CategoryController(CategoryService categoryService, FileStorageService fileStorageService) {
        this.categoryService = categoryService;
        this.fileStorageService = fileStorageService;
    }

    @PostMapping(value = "/", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasAnyRole('CUSTOMER', 'ADMIN')")
    public ResponseEntity<ApiResponse<CategoryResponse>> create(
            @RequestParam String categoryName,
            @RequestParam Long departmentId,
            @RequestParam String slug,
            @RequestParam(required = false) String description,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) MultipartFile image) {
        String path = image != null ? fileStorageService.store(image, "category") : null;
        CategoryResponse created = categoryService.create(
                new CreateCategoryRequest(categoryName, departmentId, slug , description, status), path);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.ok("Category created successfully", created));
    }


    @GetMapping("/")
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResponse<PageResponse<Category>> list(
            @RequestParam(defaultValue = "0") int pageIndex,
            @RequestParam(defaultValue = "10") int pageSize,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) Long departmentId,
            @RequestParam(required = false) String search) {

        return ApiResponse.ok(
                categoryService.list(
                        pageIndex,
                        pageSize,
                        status,
                        departmentId,
                        search
                )
        );
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResponse<CategoryResponse> get(@PathVariable Long id) {
        return ApiResponse.ok(categoryService.getById(id));
    }

    @PutMapping(value = "/{categoryId}", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResponse<CategoryResponse> update(
            @PathVariable Long categoryId,
            @RequestParam(required = false) String categoryName,
            @RequestParam(required = false) Long departmentId,
            @RequestParam(required = false) String description,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) MultipartFile image) {
        String path = image != null ? fileStorageService.store(image, "category") : null;
        return ApiResponse.ok("Category updated successfully",
                categoryService.update(categoryId,
                        new UpdateCategoryRequest(categoryName, departmentId, description, status), path));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResponse<Map<String, String>> delete(@PathVariable Long id) {
        categoryService.delete(id);
        return ApiResponse.ok("Category deleted successfully", Map.of("message", "Category deleted successfully"));
    }
}
