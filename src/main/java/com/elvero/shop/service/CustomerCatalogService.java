package com.elvero.shop.service;

import com.elvero.shop.dto.catalog.ProductDtos.*;
import com.elvero.shop.entity.*;
import com.elvero.shop.exception.ApiException;
import com.elvero.shop.repository.*;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class CustomerCatalogService {

    private static final List<String> PUBLISHED = List.of("published", "active");

    private final ProductRepository productRepository;
    private final ProductVariantRepository variantRepository;
    private final ProductVariantImageRepository variantImageRepository;
    private final InventoryRepository inventoryRepository;
    private final CategoryRepository categoryRepository;
    private final SubcategoryRepository subcategoryRepository;
    private final DepartmentRepository departmentRepository;

    public CustomerCatalogService(
            ProductRepository productRepository,
            ProductVariantRepository variantRepository,
            ProductVariantImageRepository variantImageRepository,
            InventoryRepository inventoryRepository,
            CategoryRepository categoryRepository,
            SubcategoryRepository subcategoryRepository,
            DepartmentRepository departmentRepository) {
        this.productRepository = productRepository;
        this.variantRepository = variantRepository;
        this.variantImageRepository = variantImageRepository;
        this.inventoryRepository = inventoryRepository;
        this.categoryRepository = categoryRepository;
        this.subcategoryRepository = subcategoryRepository;
        this.departmentRepository = departmentRepository;
    }

    @Transactional(readOnly = true)
    public List<Department> listDepartments() {
        return departmentRepository.findAllByOrderByDepartmentNameAsc();
    }

    @Transactional(readOnly = true)
    public ProductPage listProducts(
            int page, int limit, Long categoryId, Long subcategoryId, Long departmentId,
            String search, String sort) {
        int p = Math.max(page, 1);
        int size = Math.min(Math.max(limit, 1), 100);
        Sort sortObj = switch (sort != null ? sort : "newest") {
            case "name" -> Sort.by("productName").ascending();
            default -> Sort.by(Sort.Direction.DESC, "createdAt");
        };
        PageRequest pr = PageRequest.of(p - 1, size, sortObj);

        var pageResult = productRepository.findByStatusIn(PUBLISHED, pr);
        // Further filter in memory for department / search when needed (keep simple & correct)
        List<Product> filtered = pageResult.getContent().stream()
                .filter(prod -> categoryId == null || (prod.getCategory() != null && prod.getCategory().getId().equals(categoryId)))
                .filter(prod -> subcategoryId == null || (prod.getSubcategory() != null && prod.getSubcategory().getId().equals(subcategoryId)))
                .filter(prod -> departmentId == null || (prod.getCategory() != null
                        && prod.getCategory().getDepartment() != null
                        && prod.getCategory().getDepartment().getId().equals(departmentId)))
                .filter(prod -> search == null || search.isBlank()
                        || prod.getProductName().toLowerCase().contains(search.toLowerCase())
                        || (prod.getSlug() != null && prod.getSlug().toLowerCase().contains(search.toLowerCase())))
                .toList();

        List<ProductResponse> items = filtered.stream().map(this::toProductResponse).toList();
        return new ProductPage(items, new PageMeta(pageResult.getTotalElements(), p, size, pageResult.getTotalPages()));
    }

    @Transactional(readOnly = true)
    public ProductResponse getProductDetails(Long productId) {
        Product p = productRepository.findByIdWithDetails(productId)
                .orElseThrow(() -> ApiException.notFound("Product not found"));
        if (!PUBLISHED.contains(p.getStatus())) {
            throw ApiException.notFound("Product not found");
        }
        return toProductResponse(p);
    }

    @Transactional(readOnly = true)
    public AvailabilityResponse checkAvailability(Long variantId) {
        ProductVariant v = variantRepository.findById(variantId)
                .orElseThrow(() -> ApiException.notFound("Variant not found"));
        List<Inventory> invs = inventoryRepository.findByVariantId(variantId);
        int available = invs.stream()
                .mapToInt(i -> Math.max(0, i.getQuantityAvailable() - i.getQuantityReserved()))
                .sum();
        return new AvailabilityResponse(
                v.getId(), v.getSku(), available, available > 0,
                v.getSellerType(), v.getPrice(), v.getSalePrice());
    }

    @Transactional(readOnly = true)
    public List<VariantResponse> listVariants(Long productId) {
        Product p = productRepository.findById(productId)
                .orElseThrow(() -> ApiException.notFound("Product not found"));
        if (!PUBLISHED.contains(p.getStatus())) {
            throw ApiException.notFound("Product not found");
        }
        return variantRepository.findByProductIdAndStatus(productId, "active").stream()
                .map(this::toVariantResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public ProductPage byCategory(Long categoryId, int page, int limit) {
        return listProducts(page, limit, categoryId, null, null, null, "newest");
    }

    @Transactional(readOnly = true)
    public ProductPage bySubcategory(Long subcategoryId, int page, int limit) {
        return listProducts(page, limit, null, subcategoryId, null, null, "newest");
    }

    @Transactional(readOnly = true)
    public ProductPage related(Long productId, int limit) {
        Product p = productRepository.findById(productId)
                .orElseThrow(() -> ApiException.notFound("Product not found"));
        Long catId = p.getCategory() != null ? p.getCategory().getId() : null;
        return listProducts(1, limit, catId, null, null, null, "newest");
    }

    @Transactional(readOnly = true)
    public ProductFilters filters(Long categoryId, Long subcategoryId) {
        List<String> colors = variantRepository.findDistinctColors(PUBLISHED, categoryId, subcategoryId);
        List<String> sizes = variantRepository.findDistinctSizes(PUBLISHED, categoryId, subcategoryId);
        List<String> fits = variantRepository.findDistinctFits(PUBLISHED, categoryId, subcategoryId);
        Object[] range = variantRepository.findPriceRange(PUBLISHED, categoryId, subcategoryId);
        BigDecimal min = null, max = null;
        if (range != null && range.length >= 2) {
            min = (BigDecimal) range[0];
            max = (BigDecimal) range[1];
        }
        return new ProductFilters(colors, sizes, fits, min, max);
    }

    public record ProductPage(List<ProductResponse> products, PageMeta pagination) {}

    private ProductResponse toProductResponse(Product p) {
        List<ProductVariant> variants = variantRepository.findByProductIdAndStatus(p.getId(), "active");
        List<VariantResponse> vs = variants.stream().map(this::toVariantResponse).toList();
        return new ProductResponse(
                p.getId(), p.getProductName(), p.getSlug(), p.getDescription(), p.getImage(),
                p.getBasePrice(), p.getStatus(),
                p.getCategory() != null ? p.getCategory().getId() : null,
                p.getCategory() != null ? p.getCategory().getCategoryName() : null,
                p.getSubcategory() != null ? p.getSubcategory().getId() : null,
                p.getSubcategory() != null ? p.getSubcategory().getSubcategoryName() : null,
                p.getCreatedBy(), p.getUpdatedBy(), p.getPublishedBy(),
                p.getPublishedAt(), p.getArchivedAt(), vs);
    }

    private VariantResponse toVariantResponse(ProductVariant v) {
        List<VariantImageDto> imgs = variantImageRepository
                .findByVariantIdOrderByDisplayOrderAsc(v.getId()).stream()
                .map(i -> new VariantImageDto(i.getId(), i.getImageUrl(), i.getDisplayOrder()))
                .toList();
        List<InventoryDto> invs = inventoryRepository.findByVariantId(v.getId()).stream()
                .map(i -> new InventoryDto(
                        i.getId(),
                        i.getWarehouse() != null ? i.getWarehouse().getId() : null,
                        i.getQuantityAvailable(),
                        i.getQuantityReserved(),
                        i.getReorderLevel()))
                .toList();
        return new VariantResponse(
                v.getId(),
                v.getProduct() != null ? v.getProduct().getId() : null,
                v.getSku(), v.getSize(), v.getColor(), v.getFit(),
                v.getPrice(), v.getSalePrice(), v.getStatus(), v.getSellerType(),
                v.getDeliveryDate(), imgs, invs);
    }
}
