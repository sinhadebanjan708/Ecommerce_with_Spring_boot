package com.elvero.shop.controller;

import com.elvero.shop.common.ApiResponse;
import com.elvero.shop.dto.catalog.CategoryDtos.CategoryResponse;
import com.elvero.shop.dto.catalog.ProductDtos.*;
import com.elvero.shop.entity.Department;
import com.elvero.shop.service.CategoryService;
import com.elvero.shop.service.CustomerCatalogService;
import com.elvero.shop.service.CustomerCatalogService.ProductPage;
import java.util.List;
import java.util.Map;
import org.springframework.web.bind.annotation.*;

/**
 * Customer-facing storefront GET APIs (public).
 * Mirrors Node customerProduct.routes.js + customerCategory routes.
 */
@RestController
@RequestMapping("/api/ecommerce/products")
public class StorefrontController {

    private final CustomerCatalogService catalogService;
    private final CategoryService categoryService;

    public StorefrontController(CustomerCatalogService catalogService, CategoryService categoryService) {
        this.catalogService = catalogService;
        this.categoryService = categoryService;
    }

    // ---- Categories ----
    @GetMapping("/categories")
    public ApiResponse<List<CategoryResponse>> categories() {
        return ApiResponse.ok(categoryService.listActiveForStorefront());
    }

    @GetMapping("/categories/{categoryId}")
    public ApiResponse<CategoryResponse> categoryById(@PathVariable Long categoryId) {
        return ApiResponse.ok(categoryService.getActiveById(categoryId));
    }

    // ---- Departments ----
    @GetMapping("/departments")
    public ApiResponse<List<Department>> departments() {
        return ApiResponse.ok(catalogService.listDepartments());
    }

    // ---- Product lists ----
    @GetMapping
    public ApiResponse<Map<String, Object>> products(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int limit,
            @RequestParam(required = false) Long category_id,
            @RequestParam(required = false) Long subcategory_id,
            @RequestParam(required = false) Long department_id,
            @RequestParam(required = false) String search,
            @RequestParam(required = false) String sort) {
        ProductPage result = catalogService.listProducts(
                page, limit, category_id, subcategory_id, department_id, search, sort);
        return ApiResponse.ok(Map.of("products", result.products(), "pagination", result.pagination()));
    }

    @GetMapping("/search")
    public ApiResponse<Map<String, Object>> search(
            @RequestParam(required = false) String q,
            @RequestParam(required = false) String search,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int limit) {
        String term = q != null ? q : search;
        ProductPage result = catalogService.listProducts(page, limit, null, null, null, term, "newest");
        return ApiResponse.ok(Map.of("products", result.products(), "pagination", result.pagination()));
    }

    @GetMapping({"/featured", "/trending", "/new-arrivals", "/best-sellers"})
    public ApiResponse<Map<String, Object>> curated(
            @RequestParam(defaultValue = "12") int limit) {
        ProductPage result = catalogService.listProducts(1, limit, null, null, null, null, "newest");
        return ApiResponse.ok(Map.of("products", result.products(), "pagination", result.pagination()));
    }

    @GetMapping("/on-sale")
    public ApiResponse<Map<String, Object>> onSale(@RequestParam(defaultValue = "20") int limit) {
        ProductPage result = catalogService.listProducts(1, limit, null, null, null, null, "newest");
        List<ProductResponse> onSale = result.products().stream()
                .filter(p -> p.variants() != null && p.variants().stream()
                        .anyMatch(v -> v.salePrice() != null
                                && v.price() != null
                                && v.salePrice().compareTo(v.price()) < 0))
                .toList();
        return ApiResponse.ok(Map.of("products", onSale, "pagination", result.pagination()));
    }

    @GetMapping("/filters")
    public ApiResponse<ProductFilters> filters(
            @RequestParam(required = false) Long category_id,
            @RequestParam(required = false) Long subcategory_id) {
        return ApiResponse.ok(catalogService.filters(category_id, subcategory_id));
    }

    @GetMapping("/category/{categoryId}")
    public ApiResponse<Map<String, Object>> byCategory(
            @PathVariable Long categoryId,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int limit) {
        ProductPage result = catalogService.byCategory(categoryId, page, limit);
        return ApiResponse.ok(Map.of("products", result.products(), "pagination", result.pagination()));
    }

    @GetMapping("/subcategory/{subcategoryId}")
    public ApiResponse<Map<String, Object>> bySubcategory(
            @PathVariable Long subcategoryId,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int limit) {
        ProductPage result = catalogService.bySubcategory(subcategoryId, page, limit);
        return ApiResponse.ok(Map.of("products", result.products(), "pagination", result.pagination()));
    }

    @GetMapping("/variants/{variantId}/availability")
    public ApiResponse<AvailabilityResponse> availability(@PathVariable Long variantId) {
        return ApiResponse.ok(catalogService.checkAvailability(variantId));
    }

    @GetMapping("/{productId}")
    public ApiResponse<ProductResponse> productDetails(@PathVariable Long productId) {
        return ApiResponse.ok(catalogService.getProductDetails(productId));
    }

    @GetMapping("/{productId}/variants")
    public ApiResponse<List<VariantResponse>> productVariants(@PathVariable Long productId) {
        return ApiResponse.ok(catalogService.listVariants(productId));
    }

    @GetMapping("/{productId}/related")
    public ApiResponse<Map<String, Object>> related(
            @PathVariable Long productId,
            @RequestParam(defaultValue = "8") int limit) {
        ProductPage result = catalogService.related(productId, limit);
        return ApiResponse.ok(Map.of("products", result.products(), "pagination", result.pagination()));
    }

    @GetMapping("/{productId}/recommendations")
    public ApiResponse<Map<String, Object>> recommendations(
            @PathVariable Long productId,
            @RequestParam(defaultValue = "12") int limit) {
        ProductPage result = catalogService.listProducts(1, limit, null, null, null, null, "newest");
        return ApiResponse.ok(Map.of("products", result.products(), "pagination", result.pagination()));
    }
}
