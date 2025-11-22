package com.manugoswami.monk.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class UpdatedCartDto {
    private List<UpdatedItemDto> items;
    private BigDecimal totalPrice;
    private BigDecimal totalDiscount;
    private BigDecimal finalPrice;
}
