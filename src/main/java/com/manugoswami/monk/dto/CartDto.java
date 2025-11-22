package com.manugoswami.monk.dto;

import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

@Data
public class CartDto {
    private List<CartItemDto> items;
}
