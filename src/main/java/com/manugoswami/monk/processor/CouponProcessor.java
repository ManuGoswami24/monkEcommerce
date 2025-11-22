package com.manugoswami.monk.processor;

import com.manugoswami.monk.dto.CartDto;
import com.manugoswami.monk.model.Coupon;

import java.math.BigDecimal;

public interface CouponProcessor {

    String type();

    boolean isApplicable(CartDto cartDto, Coupon coupon);

    BigDecimal calculateDiscount(CartDto cart, Coupon coupon);

    BigDecimal applyAndReturnTotalDiscount(CartDto cart, Coupon coupon);


}
