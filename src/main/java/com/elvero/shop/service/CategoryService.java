package com.elvero.shop.service;

import com.elvero.shop.dto.catalog.CategoryDtos;
import com.elvero.shop.dto.catalog.CategoryDtos.CategoryResponse;
import com.elvero.shop.dto.catalog.CategoryDtos.CreateCategoryRequest;
import com.elvero.shop.dto.catalog.CategoryDtos.PageResponse;
import com.elvero.shop.dto.catalog.CategoryDtos.Pagination;
import com.elvero.shop.dto.catalog.CategoryDtos.SubcategorySummary;
import com.elvero.shop.dto.catalog.CategoryDtos.UpdateCategoryRequest;
import com.elvero.shop.entity.Category;
import com.elvero.shop.entity.Department;
import com.elvero.shop.entity.Subcategory;
import com.elvero.shop.exception.ApiException;
import com.elvero.shop.repository.CategoryRepository;
import com.elvero.shop.repository.DepartmentRepository;
import com.elvero.shop.repository.SubcategoryRepository;
import com.elvero.shop.util.SlugUtils;

import java.util.*;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.transaction.annotation.Transactional;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
public class CategoryService {

    private final CategoryRepository categoryRepository;
    private final DepartmentRepository departmentRepository;
    private final SubcategoryRepository subcategoryRepository;

    public CategoryService(
            CategoryRepository categoryRepository,
            DepartmentRepository departmentRepository,
            SubcategoryRepository subcategoryRepository) {
        this.categoryRepository = categoryRepository;
        this.departmentRepository = departmentRepository;
        this.subcategoryRepository = subcategoryRepository;
    }

    @Transactional
    public CategoryResponse create(CreateCategoryRequest req, String imagePath) {
        if (req.categoryName() == null || req.categoryName().isBlank()) {
            throw ApiException.badRequest("category_name is required");
        }
        if (req.departmentId() == null) {
            throw ApiException.badRequest("department_id is required");
        }
        Department dept = departmentRepository.findById(req.departmentId())
                .orElseThrow(() -> ApiException.notFound("Department not found"));

        String baseSlug = slugifyUnique(req.slug(), null);
        Category c = new Category();
        c.setCategoryName(req.categoryName().trim());
        c.setDepartment(dept);
        c.setDescription(req.description());
        c.setImage(imagePath);
        c.setSlug(baseSlug);
        c.setStatus(req.status() != null ? req.status() : "active");
        categoryRepository.save(c);
        return toResponse(c, List.of());
    }
    /**
     * Optimized list:
     *  1) Page only category IDs (filters applied, pagination correct — no collection join).
     *  2) One query loads those categories + department + subcategories (JOIN FETCH).
     *  3) Re-order to match the page order and map to DTOs.
     */


    @Transactional(readOnly = true)
    public PageResponse<Category> list(
            int pageIndex,
            int pageSize,
            String status,
            Long departmentId,
            String search) {

        // 1. Validate pagination
        int size = Math.min(Math.max(pageSize, 1), 100);
        int page = Math.max(pageIndex, 0);

        PageRequest pageable = PageRequest.of(
                page,
                size,
                Sort.by(Sort.Direction.DESC, "createdAt")
        );

        // 2. Normalize filters
        String statusFilter = blankToNull(status);
        String searchFilter = blankToNull(search);

        // 3. Fetch paginated category IDs
        Page<Long> idPage = categoryRepository.findIds(
                statusFilter,
                departmentId,
                searchFilter,
                pageable
        );

        List<Long> ids = idPage.getContent();
        List<Category> data = new ArrayList<>();


        if (!ids.isEmpty()) {

            // 4. Fetch categories with department and subcategories
            List<Category> loaded =
                    categoryRepository.findByIdInWithDepartmentAndSubcategories(ids);

            // 5. Create map
            Map<Long, Category> byId = new HashMap<>();

            for (Category category : loaded) {
                byId.put(category.getId(), category);
            }

            for (Category category : loaded) {

                System.out.println(
                        "Category: " + category.getCategoryName()
                );

                System.out.println(
                        "Subcategories count: " +
                                category.getSubcategories().size()
                );
            }

            // 6. Preserve original pagination order
            for (Long id : ids) {
                Category category = byId.get(id);

                if (category != null) {
                    data.add(category);
                }
            }
        }

        // 7. Create pagination metadata
        Pagination pagination = new Pagination(
                page,
                size,
                idPage.getTotalElements(),
                idPage.getTotalPages(),
                idPage.hasNext(),
                idPage.hasPrevious()
        );

        return new PageResponse<>(data, pagination);
    }


//    @Transactional(readOnly = true)
//    public PageResponse<CategoryResponse> list(int pageIndex, int pageSize, String status, Long departmentId, String search) {
//        int size = Math.min(Math.max(pageSize, 1), 100);
//        int page = Math.max(pageIndex, 0);
//        PageRequest pr = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));
//
//        // Simple approach: load page then map (filters applied via search list when needed)
//        Page<Category> result;
//        if (status == null && departmentId == null && (search == null || search.isBlank())) {
//            result = categoryRepository.findAll(pr);
//        } else {
//            List<Category> all = categoryRepository.search(status, departmentId, blankToNull(search));
//            int from = Math.min(page * size, all.size());
//            int to = Math.min(from + size, all.size());
//            List<Category> slice = all.subList(from, to);
//            long total = all.size();
//            List<CategoryResponse> data = slice.stream()
//                    .map(c -> toResponse(c, subcategoryRepository.findByCategoryId(c.getId())))
//                    .toList();
//            int totalPages = (int) Math.ceil((double) total / size);
//            return new PageResponse<>(data, new Pagination(page, size, total, totalPages, page + 1 < totalPages, page > 0));
//        }
//
//        List<CategoryResponse> data = result.getContent().stream()
//                .map(c -> toResponse(c, subcategoryRepository.findByCategoryId(c.getId())))
//                .toList();
//        int totalPages = result.getTotalPages();
//        return new PageResponse<>(data, new Pagination(
//                page, size, result.getTotalElements(), totalPages,
//                page + 1 < totalPages, page > 0));
//    }

    @Transactional(readOnly = true)
    public CategoryResponse getById(Long id) {
        Category c = categoryRepository.findByIdWithDepartment(id)
                .orElseThrow(() -> ApiException.notFound("Category not found"));
        List<Subcategory> subs = subcategoryRepository.findByCategoryId(id);
        return toResponse(c, subs);
    }

    @Transactional
    public CategoryResponse update(Long id, UpdateCategoryRequest req, String imagePath) {
        Category c = categoryRepository.findById(id)
                .orElseThrow(() -> ApiException.notFound("Category not found"));

        if (req.departmentId() != null) {
            Department dept = departmentRepository.findById(req.departmentId())
                    .orElseThrow(() -> ApiException.notFound("Department not found"));
            c.setDepartment(dept);
        }
        if (req.categoryName() != null && !req.categoryName().isBlank()
                && !req.categoryName().equals(c.getCategoryName())) {
            c.setCategoryName(req.categoryName().trim());
            c.setSlug(slugifyUnique(req.categoryName(), id));
        }
        if (req.description() != null) {
            c.setDescription(req.description());
        }
        if (imagePath != null) {
            c.setImage(imagePath);
        }
        if (req.status() != null) {
            c.setStatus(req.status());
        }
        categoryRepository.save(c);
        return toResponse(c, subcategoryRepository.findByCategoryId(id));
    }

    @Transactional
    public void delete(Long id) {
        Category c = categoryRepository.findById(id)
                .orElseThrow(() -> ApiException.notFound("Category not found"));
        long subCount = subcategoryRepository.countByCategoryId(id);
        if (subCount > 0) {
            throw ApiException.badRequest(
                    "Cannot delete category with existing subcategories. Delete or reassign them first.");
        }
        categoryRepository.delete(c);
    }

    // ---------- Storefront ----------

    @Transactional(readOnly = true)
    public List<CategoryResponse> listActiveForStorefront() {
        List<Category> categories = categoryRepository.findByStatusOrderByCategoryNameAsc("active");
        return categories.stream()
                .map(c -> {
                    List<Subcategory> subs =
                            subcategoryRepository.findByCategoryIdAndStatusOrderBySubcategoryNameAsc(
                                    c.getId(), "active");
                    return toResponse(c, subs);
                })
                .toList();
    }

    @Transactional(readOnly = true)
    public CategoryResponse getActiveById(Long id) {
        Category c = categoryRepository.findByIdWithDepartment(id)
                .orElseThrow(() -> ApiException.notFound("Category not found"));
        if (!"active".equalsIgnoreCase(c.getStatus())) {
            throw ApiException.notFound("Category not found");
        }
        List<Subcategory> subs =
                subcategoryRepository.findByCategoryIdAndStatusOrderBySubcategoryNameAsc(id, "active");
        return toResponse(c, subs);
    }

    private String slugifyUnique(String name, Long excludeId) {
        String base = SlugUtils.slugify(name);
        String slug = base;
        int i = 1;
        while (true) {
            boolean exists = excludeId == null
                    ? categoryRepository.existsBySlug(slug)
                    : categoryRepository.existsBySlugAndIdNot(slug, excludeId);
            if (!exists) {
                return slug;
            }
            slug = base + "-" + (i++);
        }
    }

    private CategoryResponse toResponse(Category c, List<Subcategory> subs) {
        List<SubcategorySummary> subDtos = subs == null ? List.of() : subs.stream()
                .map(s -> new SubcategorySummary(
                        s.getId(), s.getSubcategoryName(), s.getSlug(), s.getImage(), s.getStatus()))
                .toList();
        Long deptId = c.getDepartment() != null ? c.getDepartment().getId() : null;
        String deptName = c.getDepartment() != null ? c.getDepartment().getDepartmentName() : null;
        return new CategoryResponse(
                c.getId(), c.getCategoryName(), c.getSlug(), c.getDescription(),
                c.getImage(), c.getStatus(), deptId, deptName, subDtos);
    }

    private static String blankToNull(String s) {
        return s == null || s.isBlank() ? null : s;
    }
}
