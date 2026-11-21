
package com.elvero.shop.specification;

import com.elvero.shop.entity.Product;
import jakarta.persistence.criteria.Predicate;
import java.util.ArrayList;
import java.util.List;
import org.springframework.data.jpa.domain.Specification;
import com.elvero.shop.specification.ProductSpecification;
import org.springframework.data.jpa.domain.Specification;

public class ProductSpecification {

    public static Specification<Product> filterProducts(
            String status,
            Long categoryId,
            Long subcategoryId,
            String search
    ) {
        return (root, query, cb) -> {

            List<Predicate> predicates = new ArrayList<>();

            if (status != null && !status.isBlank()) {
                predicates.add(
                        cb.equal(root.get("status"), status)
                );
            }

            if (categoryId != null) {
                predicates.add(
                        cb.equal(root.get("category").get("id"), categoryId)
                );
            }

            if (subcategoryId != null) {
                predicates.add(
                        cb.equal(root.get("subcategory").get("id"), subcategoryId)
                );
            }

            if (search != null && !search.isBlank()) {
                String keyword = "%" + search.trim().toLowerCase() + "%";

                predicates.add(
                        cb.or(
                                cb.like(
                                        cb.lower(root.get("productName")),
                                        keyword
                                ),
                                cb.like(
                                        cb.lower(root.get("slug")),
                                        keyword
                                )
                        )
                );
            }

            return cb.and(predicates.toArray(new Predicate[0]));
        };
    }
}
