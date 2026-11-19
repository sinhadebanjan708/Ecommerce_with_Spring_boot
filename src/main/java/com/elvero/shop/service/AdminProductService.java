package com.elvero.shop.service;

import com.elvero.shop.dto.catalog.ProductDtos.*;
import com.elvero.shop.entity.*;
import com.elvero.shop.exception.ApiException;
import com.elvero.shop.repository.*;
import com.elvero.shop.util.SlugUtils;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

@Service
public class AdminProductService {

    private final ProductRepository productRepository;
    private final ProductVariantRepository variantRepository;
    private final ProductVariantImageRepository variantImageRepository;
    private final CategoryRepository categoryRepository;
    private final SubcategoryRepository subcategoryRepository;
    private final InventoryRepository inventoryRepository;
    private final WarehouseRepository warehouseRepository;
    private final FileStorageService fileStorageService;

    public AdminProductService(
            ProductRepository productRepository,
            ProductVariantRepository variantRepository,
            ProductVariantImageRepository variantImageRepository,
            CategoryRepository categoryRepository,
            SubcategoryRepository subcategoryRepository,
            InventoryRepository inventoryRepository,
            WarehouseRepository warehouseRepository,
            FileStorageService fileStorageService) {
        this.productRepository = productRepository;
        this.variantRepository = variantRepository;
        this.variantImageRepository = variantImageRepository;
        this.categoryRepository = categoryRepository;
        this.subcategoryRepository = subcategoryRepository;
        this.inventoryRepository = inventoryRepository;
        this.warehouseRepository = warehouseRepository;
        this.fileStorageService = fileStorageService;
    }

    @Transactional
    public ProductResponse createProduct(CreateProductRequest req, Long adminId, MultipartFile image) {
        if (req.productName() == null || req.productName().isBlank()) {
            throw ApiException.badRequest("product_name is required");
        }
        if (req.categoryId() == null) {
            throw ApiException.badRequest("category_id is required");
        }
        Category category = categoryRepository.findById(req.categoryId())
                .orElseThrow(() -> ApiException.notFound("Category not found"));

        Subcategory subcategory = null;
        if (req.subcategoryId() != null) {
            subcategory = subcategoryRepository.findById(req.subcategoryId())
                    .orElseThrow(() -> ApiException.notFound("Subcategory not found"));
            if (!subcategory.getCategory().getId().equals(req.categoryId())) {
                throw ApiException.badRequest("Subcategory does not belong to the selected category");
            }
        }

        Product p = new Product();
        p.setProductName(req.productName().trim());
        p.setCategory(category);
        p.setSubcategory(subcategory);
        p.setDescription(req.description());
        p.setBasePrice(req.basePrice());
        p.setSlug(uniqueProductSlug(req.productName(), null));
        p.setStatus(req.status() != null ? req.status() : "draft");
        p.setCreatedBy(adminId);
        p.setUpdatedBy(adminId);
        if (image != null && !image.isEmpty()) {
            p.setImage(fileStorageService.store(image, "product"));
        }
        productRepository.save(p);
        return toProductResponse(p, List.of());
    }

    @Transactional(readOnly = true)
    public PageMetaAndList listProducts(int page, int limit, String status, Long categoryId, Long subcategoryId, String search) {
        int p = Math.max(page, 1);
        int size = Math.min(Math.max(limit, 1), 100);
        PageRequest pr = PageRequest.of(p - 1, size, Sort.by(Sort.Direction.DESC, "createdAt"));

        Page<Product> result;
        if (categoryId != null && status != null) {
            result = productRepository.findByCategoryIdAndStatusIn(categoryId, List.of(status), pr);
        } else if (subcategoryId != null && status != null) {
            result = productRepository.findBySubcategoryIdAndStatusIn(subcategoryId, List.of(status), pr);
        } else if (status != null) {
            result = productRepository.findByStatusIn(List.of(status), pr);
        } else {
            result = productRepository.findAll(pr);
        }

        List<ProductListItem> items = result.getContent().stream().map(this::toListItem).toList();
        return new PageMetaAndList(items, new PageMeta(result.getTotalElements(), p, size, result.getTotalPages()));
    }

    @Transactional(readOnly = true)
    public ProductResponse getById(Long productId) {
        Product p = productRepository.findByIdWithDetails(productId)
                .orElseThrow(() -> ApiException.notFound("Product not found"));
        List<ProductVariant> variants = variantRepository.findByProductId(productId);
        return toProductResponse(p, variants);
    }

    @Transactional
    public ProductResponse updateProduct(Long productId, UpdateProductRequest req, Long adminId, MultipartFile image) {
        Product p = productRepository.findById(productId)
                .orElseThrow(() -> ApiException.notFound("Product not found"));

        if (req.categoryId() != null) {
            Category cat = categoryRepository.findById(req.categoryId())
                    .orElseThrow(() -> ApiException.notFound("Category not found"));
            p.setCategory(cat);
        }
        if (req.subcategoryId() != null) {
            if (req.subcategoryId() == 0) {
                p.setSubcategory(null);
            } else {
                Subcategory sub = subcategoryRepository.findById(req.subcategoryId())
                        .orElseThrow(() -> ApiException.notFound("Subcategory not found"));
                p.setSubcategory(sub);
            }
        }
        if (req.productName() != null && !req.productName().isBlank()
                && !req.productName().equals(p.getProductName())) {
            p.setProductName(req.productName().trim());
            p.setSlug(uniqueProductSlug(req.productName(), productId));
        }
        if (req.description() != null) p.setDescription(req.description());
        if (req.basePrice() != null) p.setBasePrice(req.basePrice());
        if (req.status() != null) p.setStatus(req.status());
        if (image != null && !image.isEmpty()) {
            p.setImage(fileStorageService.store(image, "product"));
        }
        p.setUpdatedBy(adminId);
        productRepository.save(p);
        return toProductResponse(p, variantRepository.findByProductId(productId));
    }

    @Transactional
    public VariantResponse createVariant(Long productId, CreateVariantRequest req, Long adminId, List<MultipartFile> images) {
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> ApiException.notFound("Product not found"));
        if (req.sku() == null || req.sku().isBlank()) {
            throw ApiException.badRequest("SKU is required");
        }
        if (req.price() == null) {
            throw ApiException.badRequest("Price is required");
        }
        if (variantRepository.existsBySku(req.sku())) {
            throw ApiException.conflict("Variant SKU already exists");
        }
        if (images != null && images.size() > 4) {
            throw ApiException.badRequest("Maximum 4 variant images are allowed");
        }

        ProductVariant v = new ProductVariant();
        v.setProduct(product);
        v.setSku(req.sku().trim());
        v.setColor(req.color());
        v.setSize(req.size());
        v.setFit(req.fit());
        v.setPrice(req.price());
        v.setSalePrice(req.salePrice());
        v.setStatus(req.status() != null ? req.status() : "active");
        v.setSellerType(req.sellerType() != null ? req.sellerType() : "Inventory");
        v.setDeliveryDate(req.deliveryDate());
        variantRepository.save(v);

        if (images != null) {
            int order = 1;
            for (MultipartFile file : images) {
                if (file == null || file.isEmpty()) continue;
                ProductVariantImage img = new ProductVariantImage();
                img.setVariant(v);
                img.setImageUrl(fileStorageService.store(file, "productvariant"));
                img.setDisplayOrder(order++);
                img.setProductId(productId);
                variantImageRepository.save(img);
            }
        }

        if (req.warehouseId() != null) {
            Warehouse wh = warehouseRepository.findById(req.warehouseId())
                    .orElseThrow(() -> ApiException.notFound("Warehouse not found"));
            Inventory inv = new Inventory();
            inv.setVariant(v);
            inv.setWarehouse(wh);
            inv.setQuantityAvailable(req.quantityAvailable() != null ? req.quantityAvailable() : 0);
            inv.setQuantityReserved(req.quantityReserved() != null ? req.quantityReserved() : 0);
            inv.setReorderLevel(req.reorderLevel() != null ? req.reorderLevel() : 0);
            inv.setUpdatedBy(adminId);
            inventoryRepository.save(inv);
        }

        return toVariantResponse(v);
    }

    @Transactional
    public VariantResponse updateVariant(Long productId, Long variantId, UpdateVariantRequest req) {
        ProductVariant v = variantRepository.findById(variantId)
                .orElseThrow(() -> ApiException.notFound("Variant not found"));
        if (!v.getProduct().getId().equals(productId)) {
            throw ApiException.notFound("Variant not found");
        }
        if (req.sku() != null && !req.sku().equals(v.getSku())) {
            if (variantRepository.existsBySkuAndIdNot(req.sku(), variantId)) {
                throw ApiException.conflict("Variant SKU already exists");
            }
            v.setSku(req.sku().trim());
        }
        if (req.color() != null) v.setColor(req.color());
        if (req.size() != null) v.setSize(req.size());
        if (req.fit() != null) v.setFit(req.fit());
        if (req.price() != null) v.setPrice(req.price());
        if (req.salePrice() != null) v.setSalePrice(req.salePrice());
        if (req.status() != null) v.setStatus(req.status());
        if (req.sellerType() != null) v.setSellerType(req.sellerType());
        if (req.deliveryDate() != null) v.setDeliveryDate(req.deliveryDate());
        variantRepository.save(v);
        return toVariantResponse(v);
    }

    @Transactional
    public void deleteVariant(Long productId, Long variantId) {
        ProductVariant v = variantRepository.findById(variantId)
                .orElseThrow(() -> ApiException.notFound("Product variant not found"));
        if (!v.getProduct().getId().equals(productId)) {
            throw ApiException.notFound("Product variant not found");
        }
        variantImageRepository.deleteByVariantId(variantId);
        inventoryRepository.findByVariantId(variantId).forEach(inventoryRepository::delete);
        variantRepository.delete(v);
    }

    @Transactional(readOnly = true)
    public Object submitForPublishing(Long productId) {
        Product p = productRepository.findById(productId)
                .orElseThrow(() -> ApiException.notFound("Product not found"));
        List<ProductVariant> variants = variantRepository.findByProductIdAndStatus(productId, "active");
        if (variants.isEmpty()) {
            throw ApiException.badRequest("Product must have at least one active variant before publishing");
        }
        for (ProductVariant v : variants) {
            if (v.getPrice() == null || v.getPrice().compareTo(BigDecimal.ZERO) < 0) {
                throw ApiException.badRequest("Variant " + v.getSku() + " has invalid price");
            }
        }
        return java.util.Map.of(
                "product_id", p.getId(),
                "ready", true,
                "variant_count", variants.size());
    }

    @Transactional
    public ProductResponse publish(Long productId, Long adminId) {
        Product p = productRepository.findById(productId)
                .orElseThrow(() -> ApiException.notFound("Product not found"));
        List<ProductVariant> variants = variantRepository.findByProductIdAndStatus(productId, "active");
        if (variants.isEmpty()) {
            throw ApiException.badRequest("Product must have at least one variant");
        }
        p.setStatus("published");
        p.setPublishedAt(LocalDateTime.now());
        p.setPublishedBy(adminId);
        p.setUpdatedBy(adminId);
        productRepository.save(p);
        return toProductResponse(p, variants);
    }

    @Transactional
    public ProductResponse archive(Long productId, Long adminId) {
        Product p = productRepository.findById(productId)
                .orElseThrow(() -> ApiException.notFound("Product not found"));
        p.setStatus("archived");
        p.setArchivedAt(LocalDateTime.now());
        p.setUpdatedBy(adminId);
        productRepository.save(p);
        return toProductResponse(p, variantRepository.findByProductId(productId));
    }

    @Transactional
    public ProductResponse restore(Long productId, Long adminId) {
        Product p = productRepository.findById(productId)
                .orElseThrow(() -> ApiException.notFound("Archived product not found"));
        if (!"archived".equalsIgnoreCase(p.getStatus())) {
            throw ApiException.badRequest("Product is not archived");
        }
        p.setStatus("draft");
        p.setArchivedAt(null);
        p.setUpdatedBy(adminId);
        productRepository.save(p);
        return toProductResponse(p, variantRepository.findByProductId(productId));
    }

    // ---- Inventory ----

    @Transactional
    public InventoryDto adjustStock(Long variantId, AdjustStockRequest req, Long adminId) {
        if (req.warehouseId() == null) throw ApiException.badRequest("warehouse_id is required");
        if (req.quantity() == null) throw ApiException.badRequest("quantity is required");
        if (!variantRepository.existsById(variantId)) throw ApiException.notFound("Variant not found");

        Inventory inv = inventoryRepository.findByVariantIdAndWarehouseId(variantId, req.warehouseId())
                .orElseGet(() -> {
                    Warehouse wh = warehouseRepository.findById(req.warehouseId())
                            .orElseThrow(() -> ApiException.notFound("Warehouse not found"));
                    ProductVariant v = variantRepository.findById(variantId).orElseThrow();
                    Inventory n = new Inventory();
                    n.setVariant(v);
                    n.setWarehouse(wh);
                    n.setQuantityAvailable(0);
                    n.setQuantityReserved(0);
                    n.setReorderLevel(0);
                    return n;
                });
        int newQty = inv.getQuantityAvailable() + req.quantity();
        if (newQty < 0) throw ApiException.badRequest("Insufficient stock for adjustment");
        inv.setQuantityAvailable(newQty);
        inv.setUpdatedBy(adminId);
        inventoryRepository.save(inv);
        return toInventoryDto(inv);
    }

    @Transactional
    public InventoryDto reserveStock(Long variantId, ReserveStockRequest req, Long adminId) {
        Inventory inv = inventoryRepository.findByVariantIdAndWarehouseId(variantId, req.warehouseId())
                .orElseThrow(() -> ApiException.notFound("Inventory not found"));
        int available = inv.getQuantityAvailable() - inv.getQuantityReserved();
        if (available < req.quantity()) {
            throw ApiException.badRequest("Insufficient available stock to reserve");
        }
        inv.setQuantityReserved(inv.getQuantityReserved() + req.quantity());
        inv.setUpdatedBy(adminId);
        inventoryRepository.save(inv);
        return toInventoryDto(inv);
    }

    @Transactional
    public InventoryDto releaseStock(Long variantId, ReserveStockRequest req, Long adminId) {
        Inventory inv = inventoryRepository.findByVariantIdAndWarehouseId(variantId, req.warehouseId())
                .orElseThrow(() -> ApiException.notFound("Inventory not found"));
        if (inv.getQuantityReserved() < req.quantity()) {
            throw ApiException.badRequest("Cannot release more than reserved quantity");
        }
        inv.setQuantityReserved(inv.getQuantityReserved() - req.quantity());
        inv.setUpdatedBy(adminId);
        inventoryRepository.save(inv);
        return toInventoryDto(inv);
    }

    @Transactional(readOnly = true)
    public List<Inventory> getLowStock() {
        return inventoryRepository.findLowStock();
    }

    @Transactional
    public InventoryDto updateThreshold(Long variantId, ThresholdRequest req, Long adminId) {
        Inventory inv = inventoryRepository.findByVariantIdAndWarehouseId(variantId, req.warehouseId())
                .orElseThrow(() -> ApiException.notFound("Inventory not found"));
        inv.setReorderLevel(req.reorderLevel());
        inv.setUpdatedBy(adminId);
        inventoryRepository.save(inv);
        return toInventoryDto(inv);
    }

    // ---- helpers ----

    public record PageMetaAndList(List<ProductListItem> items, PageMeta meta) {}

    private String uniqueProductSlug(String name, Long excludeId) {
        String base = SlugUtils.slugify(name);
        String slug = base;
        int i = 1;
        while (true) {
            boolean exists = excludeId == null
                    ? productRepository.existsBySlug(slug)
                    : productRepository.existsBySlugAndIdNot(slug, excludeId);
            if (!exists) return slug;
            slug = base + "-" + (i++);
        }
    }

    private ProductListItem toListItem(Product p) {
        List<ProductVariant> variants = variantRepository.findByProductId(p.getId());
        BigDecimal min = null, max = null;
        for (ProductVariant v : variants) {
            if (v.getPrice() == null) continue;
            if (min == null || v.getPrice().compareTo(min) < 0) min = v.getPrice();
            if (max == null || v.getPrice().compareTo(max) > 0) max = v.getPrice();
        }
        return new ProductListItem(
                p.getId(), p.getProductName(), p.getSlug(), p.getImage(), p.getStatus(),
                p.getCategory() != null ? p.getCategory().getCategoryName() : null,
                p.getSubcategory() != null ? p.getSubcategory().getSubcategoryName() : null,
                min, max, variants.size());
    }

    private ProductResponse toProductResponse(Product p, List<ProductVariant> variants) {
        List<VariantResponse> vs = variants == null ? List.of()
                : variants.stream().map(this::toVariantResponse).toList();
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
                .map(this::toInventoryDto)
                .toList();
        return new VariantResponse(
                v.getId(),
                v.getProduct() != null ? v.getProduct().getId() : null,
                v.getSku(), v.getSize(), v.getColor(), v.getFit(),
                v.getPrice(), v.getSalePrice(), v.getStatus(), v.getSellerType(),
                v.getDeliveryDate(), imgs, invs);
    }

    private InventoryDto toInventoryDto(Inventory i) {
        return new InventoryDto(
                i.getId(),
                i.getWarehouse() != null ? i.getWarehouse().getId() : null,
                i.getQuantityAvailable(),
                i.getQuantityReserved(),
                i.getReorderLevel());
    }
}
