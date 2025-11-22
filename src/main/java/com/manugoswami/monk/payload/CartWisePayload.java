package com.manugoswami.monk.payload;

import lombok.Data;

import java.math.BigDecimal;

@Data
public class CartWisePayload {
    private BigDecimal threshold;
    private BigDecimal discountPercent;
    private BigDecimal maxDiscount;
}
