package com.elvero.shop.service;

import com.elvero.shop.dto.catalog.SubcategoryDtos.CreateSubcategoryRequest;
import com.elvero.shop.dto.catalog.SubcategoryDtos.SubcategoryResponse;
import com.elvero.shop.dto.catalog.SubcategoryDtos.UpdateSubcategoryRequest;
import com.elvero.shop.entity.Category;
import com.elvero.shop.entity.Subcategory;
import com.elvero.shop.exception.ApiException;
import com.elvero.shop.repository.CategoryRepository;
import com.elvero.shop.repository.ProductRepository;
import com.elvero.shop.repository.SubcategoryRepository;
import com.elvero.shop.util.SlugUtils;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class SubcategoryService {

    private final SubcategoryRepository subcategoryRepository;
    private final CategoryRepository categoryRepository;
    private final ProductRepository productRepository;

    public SubcategoryService(
            SubcategoryRepository subcategoryRepository,
            CategoryRepository categoryRepository,
            ProductRepository productRepository) {
        this.subcategoryRepository = subcategoryRepository;
        this.categoryRepository = categoryRepository;
        this.productRepository = productRepository;
    }

    @Transactional
    public SubcategoryResponse create(CreateSubcategoryRequest req, String imagePath) {
        if (req.categoryId() == null) {
            throw ApiException.badRequest("category_id is required");
        }
        if (req.subcategoryName() == null || req.subcategoryName().isBlank()) {
            throw ApiException.badRequest("subcategory_name is required");
        }
        Category category = categoryRepository.findById(req.categoryId())
                .orElseThrow(() -> ApiException.notFound("Category not found"));

        Subcategory s = new Subcategory();
        s.setCategory(category);
        s.setSubcategoryName(req.subcategoryName().trim());
        s.setSlug(slugifyUnique(req.subcategoryName(), null));
        s.setImage(imagePath);
        s.setStatus(req.status() != null ? req.status() : "active");
        subcategoryRepository.save(s);
        return toResponse(s);
    }

    @Transactional(readOnly = true)
    public Page<SubcategoryResponse> listAll(
            int pageIndex,
            int pageSize,
            String search
    ) {
        Pageable pageable = PageRequest.of(
                pageIndex,
                pageSize,
                Sort.by(Sort.Direction.DESC, "id")
        );

        // Step 1: Fetch paginated subcategories

        String searchTerm = (search == null || search.isBlank())
                ? null
                : search.trim();
        System.out.println("Search parameter received: " + search);
        System.out.println("Normalized search: " + searchTerm);
        Page<Subcategory> subcategoryPage =
                subcategoryRepository.findAllWithSearch(searchTerm, pageable);

        List<Subcategory> subcategories = subcategoryPage.getContent();

        // Step 2: Avoid an unnecessary count query for an empty page
        if (subcategories.isEmpty()) {
            return subcategoryPage.map(subcategory -> toResponse(subcategory, 0L));
        }

        // Step 3: Get IDs from the current page
        List<Long> subcategoryIds = subcategories.stream()
                .map(Subcategory::getId)
                .toList();

        // Step 4: Fetch product counts in one query
        Map<Long, Long> productCountMap =
                subcategoryRepository
                        .findProductCountsBySubcategoryIds(subcategoryIds)
                        .stream()
                        .collect(Collectors.toMap(
                                SubcategoryRepository.SubcategoryProductCountProjection::getSubcategoryId,
                                SubcategoryRepository.SubcategoryProductCountProjection::getProductCount
                        ));

        // Step 5: Preserve pagination metadata
        return subcategoryPage.map(subcategory ->
                toResponse(
                        subcategory,
                        productCountMap.getOrDefault(subcategory.getId(), 0L)
                )
        );
    }

//    @Transactional(readOnly = true)
//    public List<SubcategoryResponse> listByCategory(Long categoryId) {
//        if (!categoryRepository.existsById(categoryId)) {
//            throw ApiException.notFound("Category not found");
//        }
//        return subcategoryRepository
//                .findByCategoryIdAndStatusOrderBySubcategoryNameAsc(categoryId, "active")
//                .stream()
//                .map(this::toResponse)
//                .toList();
//    }

    @Transactional(readOnly = true)
    public List<SubcategoryResponse> listByCategory(Long categoryId) {

        if (!categoryRepository.existsById(categoryId)) {
            throw ApiException.notFound("Category not found");
        }

        List<Subcategory> subcategories =
                subcategoryRepository
                        .findByCategoryIdAndStatusOrderBySubcategoryNameAsc(
                                categoryId, "active"
                        );

        List<Long> subcategoryIds = subcategories.stream()
                .map(Subcategory::getId)
                .toList();

        Map<Long, Long> productCountMap =
                subcategoryRepository.findProductCountsBySubcategoryIds(subcategoryIds)
                        .stream()
                        .collect(Collectors.toMap(
                                SubcategoryRepository.SubcategoryProductCountProjection::getSubcategoryId,
                                SubcategoryRepository.SubcategoryProductCountProjection::getProductCount
                        ));

        return subcategories.stream()
                .map(subcategory -> toResponse(
                        subcategory,
                        productCountMap.getOrDefault(subcategory.getId(), 0L)
                ))
                .toList();
    }

    @Transactional(readOnly = true)
    public SubcategoryResponse getById(Long id) {
        Subcategory s = subcategoryRepository.findByIdWithCategory(id)
                .orElseThrow(() -> ApiException.notFound("Subcategory not found"));
        return toResponse(s);
    }

    @Transactional
    public SubcategoryResponse update(Long id, UpdateSubcategoryRequest req, String imagePath) {
        Subcategory s = subcategoryRepository.findById(id)
                .orElseThrow(() -> ApiException.notFound("Subcategory not found"));

        if (req.categoryId() != null && !req.categoryId().equals(s.getCategory().getId())) {
            Category cat = categoryRepository.findById(req.categoryId())
                    .orElseThrow(() -> ApiException.notFound("Category not found"));
            s.setCategory(cat);
        }
        if (req.subcategoryName() != null && !req.subcategoryName().isBlank()
                && !req.subcategoryName().equals(s.getSubcategoryName())) {
            s.setSubcategoryName(req.subcategoryName().trim());
            s.setSlug(slugifyUnique(req.slug(), id));
        }
        if (imagePath != null) {
            s.setImage(imagePath);
        }
        if (req.status() != null) {
            s.setStatus(req.status());
        }
        if (req.slug() != null) {
            s.setSlug(req.slug());
        }
        subcategoryRepository.save(s);
        return toResponse(s);
    }

    @Transactional
    public void delete(Long id) {
        Subcategory s = subcategoryRepository.findById(id)
                .orElseThrow(() -> ApiException.notFound("Subcategory not found"));
        long productCount = productRepository.countBySubcategoryId(id);
        if (productCount > 0) {
            throw ApiException.badRequest(
                    "Cannot delete subcategory with existing products. Reassign or delete products first.");
        }
        subcategoryRepository.delete(s);
    }

    private String slugifyUnique(String name, Long excludeId) {
        String base = SlugUtils.slugify(name);
        String slug = base;
        int i = 1;
        while (true) {
            boolean exists = excludeId == null
                    ? subcategoryRepository.existsBySlug(slug)
                    : subcategoryRepository.existsBySlugAndIdNot(slug, excludeId);
            if (!exists) {
                return slug;
            }
            slug = base + "-" + (i++);
        }
    }

    private SubcategoryResponse toResponse(
            Subcategory subcategory,
            Long productCount
    ) {
        return new SubcategoryResponse(
                subcategory.getId(),
                subcategory.getCategory().getId(),
                subcategory.getCategory().getCategoryName(),
                subcategory.getSubcategoryName(),
                subcategory.getSlug(),
                subcategory.getImage(),
                subcategory.getStatus(),
                productCount
        );
    }

    private SubcategoryResponse toResponse(Subcategory subcategory) {
        Long productCount = productRepository.countBySubcategoryId(
                subcategory.getId()
        );

        return toResponse(subcategory, productCount);
    }
}
