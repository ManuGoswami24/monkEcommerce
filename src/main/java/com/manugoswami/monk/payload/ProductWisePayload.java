package com.manugoswami.monk.payload;

import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

@Data
public class ProductWisePayload {
    private List<Long> productIds;
    private BigDecimal discountPercent;
    private Integer minQuantity;
    private Integer maxQuantity;
}
