package com.elvero.shop.controller;

import com.elvero.shop.common.ApiResponse;
import com.elvero.shop.dto.catalog.SubcategoryDtos.CreateSubcategoryRequest;
import com.elvero.shop.dto.catalog.SubcategoryDtos.SubcategoryResponse;
import com.elvero.shop.dto.catalog.SubcategoryDtos.UpdateSubcategoryRequest;
import com.elvero.shop.service.FileStorageService;
import com.elvero.shop.service.SubcategoryService;
import java.util.List;
import java.util.Map;

import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/ecommerce/sub-categories")
public class SubcategoryController {

    private final SubcategoryService subcategoryService;
    private final FileStorageService fileStorageService;

    public SubcategoryController(SubcategoryService subcategoryService, FileStorageService fileStorageService) {
        this.subcategoryService = subcategoryService;
        this.fileStorageService = fileStorageService;
    }

    @PostMapping(value = "/", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<SubcategoryResponse>> create(
            @RequestParam Long categoryId,
            @RequestParam String subcategoryName,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) MultipartFile image) {
        String path = image != null ? fileStorageService.store(image, "subcategory") : null;
        SubcategoryResponse created = subcategoryService.create(
                new CreateSubcategoryRequest(categoryId, subcategoryName, status), path);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.ok("Subcategory created successfully", created));
    }

    @GetMapping("/")
    @PreAuthorize("hasAnyRole('CUSTOMER', 'ADMIN')")
    public ApiResponse<Page<SubcategoryResponse>> list(
            @RequestParam(defaultValue = "0") int pageIndex,
            @RequestParam(defaultValue = "10") int pageSize,
            @RequestParam(required = false) String search
    ) {
        return ApiResponse.ok(
                subcategoryService.listAll(pageIndex, pageSize, search)
        );
    }

    @GetMapping("/category/{categoryId}")
    @PreAuthorize("hasAnyRole('CUSTOMER', 'ADMIN')")
    public ApiResponse<List<SubcategoryResponse>> byCategory(@PathVariable Long categoryId) {
        return ApiResponse.ok(subcategoryService.listByCategory(categoryId));
    }

    @GetMapping("/{subcategoryId}")
    public ApiResponse<SubcategoryResponse> get(@PathVariable Long subcategoryId) {
        return ApiResponse.ok(subcategoryService.getById(subcategoryId));
    }

    @PutMapping(value = "/{id}", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResponse<SubcategoryResponse> update(
            @PathVariable Long id,
            @RequestParam(required = false) Long categoryId,
            @RequestParam(required = false) String subcategoryName,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String slug,
            @RequestParam(required = false) MultipartFile image) {
        String path = image != null ? fileStorageService.store(image, "subcategory") : null;
        return ApiResponse.ok("Subcategory updated successfully",
                subcategoryService.update(id,
                        new UpdateSubcategoryRequest(categoryId, subcategoryName, status, slug), path));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResponse<Map<String, String>> delete(@PathVariable Long id) {
        subcategoryService.delete(id);
        return ApiResponse.ok("Subcategory deleted successfully",
                Map.of("message", "Subcategory deleted successfully"));
    }
}
