package com.manugoswami.monk.model;

import jakarta.persistence.*;
import lombok.Data;

import java.time.Instant;

@Entity
@Data
@Table(name = "coupons")
public class Coupon {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true)
    private String code;

    // "CART_WISE", "PRODUCT_WISE", "BXGY"
    private String type;

    @Column(columnDefinition = "TEXT")
    private String payloadJson;

    private Boolean enabled = true;

    private Instant createdAt = Instant.now();

    private Instant expiresAt;

}

