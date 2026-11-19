package com.elvero.shop.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "warehouses")
@Getter
@Setter
@NoArgsConstructor
public class Warehouse extends Timestamped {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    @Column(name = "warehouse_name", length = 150, nullable = false)
    private String warehouseName;

    @Column(name = "city", length = 100)
    private String city;

    @Column(name = "pincode", length = 20)
    private String pincode;

    @Column(name = "status", length = 30, nullable = false)
    private String status = "active";

    @Column(name = "address_line1", length = 255)
    private String addressLine1;

    @Column(name = "state", length = 100)
    private String state;

    @Column(name = "country", length = 100)
    private String country = "India";
}
