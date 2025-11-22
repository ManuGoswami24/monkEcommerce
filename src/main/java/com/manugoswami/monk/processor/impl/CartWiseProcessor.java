package com.manugoswami.monk.processor.impl;

import com.manugoswami.monk.dto.CartDto;
import com.manugoswami.monk.model.Coupon;
import com.manugoswami.monk.payload.CartWisePayload;
import com.manugoswami.monk.processor.CouponProcessor;
import org.springframework.stereotype.Component;
import tools.jackson.databind.ObjectMapper;

import java.math.BigDecimal;
import java.math.RoundingMode;

@Component
public class CartWiseProcessor implements CouponProcessor {

    private final ObjectMapper om = new ObjectMapper();

    @Override
    public String type() {
        return "CART_WISE";
    }

    @Override
    public boolean isApplicable(CartDto cart, Coupon coupon) {
        try {
            CartWisePayload p = om.readValue(coupon.getPayloadJson(), CartWisePayload.class);
            BigDecimal total = computeCartTotal(cart);
            return total.compareTo(p.getThreshold() == null ? BigDecimal.ZERO : p.getThreshold()) >= 0;
        } catch (Exception e) {
            return false;
        }
    }

    @Override
    public BigDecimal calculateDiscount(CartDto cart, Coupon coupon) {
        try {
            CartWisePayload p = om.readValue(coupon.getPayloadJson(), CartWisePayload.class);
            BigDecimal total = computeCartTotal(cart);
            BigDecimal percent = p.getDiscountPercent() == null ? BigDecimal.ZERO : p.getDiscountPercent();
            BigDecimal discount = total.multiply(percent).divide(BigDecimal.valueOf(100), 8, RoundingMode.HALF_UP);
            if (p.getMaxDiscount() != null) {
                discount = discount.min(p.getMaxDiscount());
            }
            return discount.setScale(2, RoundingMode.HALF_UP);
        } catch (Exception e) {
            return BigDecimal.ZERO;
        }
    }

    @Override
    public BigDecimal applyAndReturnTotalDiscount(CartDto cart, Coupon coupon) {
        return calculateDiscount(cart, coupon);
    }

    private BigDecimal computeCartTotal(CartDto cart) {
        if (cart == null || cart.getItems() == null) return BigDecimal.ZERO;
        return cart.getItems().stream()
                .map(i -> i.getPrice().multiply(BigDecimal.valueOf(i.getQuantity())))
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }
}
