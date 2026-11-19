package com.elvero.shop.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "subcategories")
@Getter
@Setter
@NoArgsConstructor
public class Subcategory extends Timestamped {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "category_id", nullable = false)
    @JsonIgnore
    private Category category;

    @Column(name = "subcategory_name", length = 150, nullable = false)
    private String subcategoryName;

    @Column(name = "slug", length = 180, nullable = false, unique = true)
    private String slug;

    @Column(name = "image", length = 255)
    private String image;

    @Column(name = "status", length = 30, nullable = false)
    private String status = "active";
}
