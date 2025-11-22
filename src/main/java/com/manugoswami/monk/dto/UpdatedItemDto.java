package com.manugoswami.monk.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class UpdatedItemDto {
    private Long productId;
    private Integer quantity;
    private BigDecimal price;
    private BigDecimal totalDiscount;
}
