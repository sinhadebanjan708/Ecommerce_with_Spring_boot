package com.elvero.shop.controller;

import com.elvero.shop.common.ApiResponse;
import com.elvero.shop.dto.catalog.ProductDtos.*;
import com.elvero.shop.security.AuthenticatedUser;
import com.elvero.shop.service.AdminProductService;
import com.elvero.shop.service.AdminProductService.PageMetaAndList;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@RestController
@RequestMapping("/api/ecommerce/admin")
@PreAuthorize("hasRole('ADMIN')")
public class AdminProductController {

    private final AdminProductService service;
    private static final Logger log =
            LoggerFactory.getLogger(AdminProductController.class);
    public AdminProductController(AdminProductService service) {
        this.service = service;
    }

    @PostMapping(value = "/products", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ApiResponse<ProductResponse>> createProduct(
            @RequestParam String productName,
            @RequestParam Long categoryId,
            @RequestParam(required = false) Long subcategoryId,
            @RequestParam(required = false) String description,
            @RequestParam(required = false) String basePrice,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) MultipartFile image,
            @AuthenticationPrincipal AuthenticatedUser user) {
        ProductResponse created = service.createProduct(
                new CreateProductRequest(productName, categoryId, subcategoryId, description, basePrice, status),
                user.userId(), image);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.ok("Product created as draft", created));
    }

    @GetMapping("/products")
    @PreAuthorize("hasAnyRole('CUSTOMER', 'ADMIN')")
    public ApiResponse<Map<String, Object>> list(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int limit,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) Long categoryId,
            @RequestParam(required = false) Long subcategoryId,
            @RequestParam(required = false) String search
    ) {
        log.info("========== PRODUCT LIST API CALLED ==========");
        log.info("Category ID: {}", categoryId);
        log.info("Subcategory ID: {}", subcategoryId);
        System.out.println("Category ID: " + categoryId);
        System.out.println("Subcategory ID: " + subcategoryId);
        PageMetaAndList result = service.listProducts(
                page,
                limit,
                status,
                categoryId,
                subcategoryId,
                search
        );

        return ApiResponse.ok(Map.of(
                "data", result.items(),
                "pagination", result.meta()
        ));
    }

    @GetMapping("/products/{productId}")
    public ApiResponse<ProductResponse> get(@PathVariable Long productId) {
        return ApiResponse.ok(service.getById(productId));
    }

    @PutMapping(value = "/products/{productId}", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ApiResponse<ProductResponse> update(
            @PathVariable Long productId,
            @RequestParam(required = false) String productName,
            @RequestParam(required = false) Long categoryId,
            @RequestParam(required = false) Long subcategoryId,
            @RequestParam(required = false) String description,
            @RequestParam(required = false) String basePrice,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) MultipartFile image,
            @AuthenticationPrincipal AuthenticatedUser user) {
        return ApiResponse.ok("Product updated",
                service.updateProduct(productId,
                        new UpdateProductRequest(productName, categoryId, subcategoryId, description, basePrice, status),
                        user.userId(), image));
    }

    @GetMapping("/products/{productId}/variants")
    @PreAuthorize("hasAnyRole('CUSTOMER', 'ADMIN')")
    public ApiResponse<List<VariantResponse>> getProductVariants(
            @PathVariable Long productId) {

        return ApiResponse.ok(
                service.getProductVariants(productId)
        );
    }

    @GetMapping("/products/{productId}/variants/{variantId}")
    @PreAuthorize("hasAnyRole('CUSTOMER', 'ADMIN')")
    public ApiResponse<VariantResponse> getProductVariantById(
            @PathVariable Long productId,
            @PathVariable Long variantId) {

        return ApiResponse.ok(
                service.getProductVariantById(productId, variantId)
        );
    }

    @PostMapping(value = "/products/{productId}/variants", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ApiResponse<VariantResponse>> createVariant(
            @PathVariable Long productId,
            @RequestParam String sku,
            @RequestParam BigDecimal price,
            @RequestParam(required = false) String color,
            @RequestParam(required = false) String size,
            @RequestParam(required = false) String fit,
            @RequestParam(required = false) BigDecimal salePrice,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String sellerType,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime deliveryDate,
            @RequestParam(required = false) Long warehouseId,
            @RequestParam(required = false) Integer quantityAvailable,
            @RequestParam(required = false) Integer quantityReserved,
            @RequestParam(required = false) Integer reorderLevel,
            @RequestParam(required = false) List<MultipartFile> images,
            @AuthenticationPrincipal AuthenticatedUser user) {
        VariantResponse created = service.createVariant(productId,
                new CreateVariantRequest(sku, color, size, fit, price, salePrice, status, sellerType,
                        deliveryDate, warehouseId, quantityAvailable, quantityReserved, reorderLevel),
                user.userId(), images);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.ok("Product variant created successfully", created));
    }

//    @PutMapping("/products/{productId}/variants/{variantId}")
//    public ApiResponse<VariantResponse> updateVariant(
//            @PathVariable Long productId,
//            @PathVariable Long variantId,
//            @RequestBody UpdateVariantRequest body) {
//        return ApiResponse.ok("Variant updated", service.updateVariant(productId, variantId, body));
//    }

    @PutMapping(
            value = "/products/{productId}/variants/{variantId}",
            consumes = MediaType.MULTIPART_FORM_DATA_VALUE
    )
    public ApiResponse<VariantResponse> updateVariant(
            @PathVariable Long productId,
            @PathVariable Long variantId,
            @RequestPart("variant") UpdateVariantRequest body,
            @RequestPart(value = "images", required = false)
            List<MultipartFile> images,
            @RequestPart(value = "retainedImageIds", required = false)
            List<Long> retainedImageIds
    ) {
        return ApiResponse.ok(
                "Variant updated",
                service.updateVariant(
                        productId,
                        variantId,
                        body,
                        images,
                        retainedImageIds
                )
        );
    }

    @DeleteMapping("/products/{productId}/variants/{variantId}")
    public ApiResponse<Map<String, Object>> deleteVariant(
            @PathVariable Long productId, @PathVariable Long variantId) {
        service.deleteVariant(productId, variantId);
        return ApiResponse.ok("Product variant deleted successfully",
                Map.of("variant_id", variantId));
    }

    @PostMapping("/products/{productId}/submit-for-publishing")
    public ApiResponse<Object> submitForPublishing(@PathVariable Long productId) {
        return ApiResponse.ok("Product passed publishing validation", service.submitForPublishing(productId));
    }

    @PostMapping("/products/{productId}/publish")
    public ApiResponse<ProductResponse> publish(
            @PathVariable Long productId, @AuthenticationPrincipal AuthenticatedUser user) {
        return ApiResponse.ok("Product published", service.publish(productId, user.userId()));
    }

    @PostMapping("/products/{productId}/archive")
    public ApiResponse<ProductResponse> archive(
            @PathVariable Long productId, @AuthenticationPrincipal AuthenticatedUser user) {
        return ApiResponse.ok("Product archived", service.archive(productId, user.userId()));
    }

    @PostMapping("/products/{productId}/restore")
    public ApiResponse<ProductResponse> restore(
            @PathVariable Long productId, @AuthenticationPrincipal AuthenticatedUser user) {
        return ApiResponse.ok("Product restored", service.restore(productId, user.userId()));
    }

    @PostMapping("/inventory/{variantId}/adjust")
    public ApiResponse<InventoryDto> adjust(
            @PathVariable Long variantId,
            @RequestBody AdjustStockRequest body,
            @AuthenticationPrincipal AuthenticatedUser user) {
        return ApiResponse.ok(service.adjustStock(variantId, body, user.userId()));
    }

    @PostMapping("/inventory/{variantId}/reserve")
    public ApiResponse<InventoryDto> reserve(
            @PathVariable Long variantId,
            @RequestBody ReserveStockRequest body,
            @AuthenticationPrincipal AuthenticatedUser user) {
        return ApiResponse.ok(service.reserveStock(variantId, body, user.userId()));
    }

    @PostMapping("/inventory/{variantId}/release")
    public ApiResponse<InventoryDto> release(
            @PathVariable Long variantId,
            @RequestBody ReserveStockRequest body,
            @AuthenticationPrincipal AuthenticatedUser user) {
        return ApiResponse.ok(service.releaseStock(variantId, body, user.userId()));
    }

    @GetMapping("/inventory/low-stock")
    public ApiResponse<?> lowStock() {
        return ApiResponse.ok(service.getLowStock());
    }

    @PutMapping("/inventory/{variantId}/threshold")
    public ApiResponse<InventoryDto> threshold(
            @PathVariable Long variantId,
            @RequestBody ThresholdRequest body,
            @AuthenticationPrincipal AuthenticatedUser user) {
        return ApiResponse.ok(service.updateThreshold(variantId, body, user.userId()));
    }
}
