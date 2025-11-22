package com.manugoswami.monk.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class ApplicableCouponDto {
    private Long couponId;
    private String type;
    private BigDecimal discount;
}
