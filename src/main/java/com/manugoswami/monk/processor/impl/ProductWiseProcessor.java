package com.manugoswami.monk.processor.impl;

import com.manugoswami.monk.dto.CartDto;
import com.manugoswami.monk.dto.CartItemDto;
import com.manugoswami.monk.model.Coupon;
import com.manugoswami.monk.payload.ProductWisePayload;
import com.manugoswami.monk.processor.CouponProcessor;
import tools.jackson.databind.ObjectMapper;

import java.math.BigDecimal;
import java.math.RoundingMode;

public class ProductWiseProcessor implements CouponProcessor {
    private final ObjectMapper om = new ObjectMapper();

    @Override
    public String type() {
        return "PRODUCT_WISE";
    }

    @Override
    public boolean isApplicable(CartDto cart, Coupon coupon) {
        try {
            ProductWisePayload p = om.readValue(coupon.getPayloadJson(), ProductWisePayload.class);
            if (p.getProductIds() == null || p.getProductIds().isEmpty()) return false;
            for (CartItemDto it : cart.getItems()) {
                if (p.getProductIds().contains(it.getProductId())) {
                    if (p.getMinQuantity() == null || it.getQuantity() >= p.getMinQuantity()) {
                        return true;
                    }
                }
            }
            return false;
        } catch (Exception e) {
            return false;
        }
    }

    @Override
    public BigDecimal calculateDiscount(CartDto cart, Coupon coupon) {
        try {
            ProductWisePayload p = om.readValue(coupon.getPayloadJson(), ProductWisePayload.class);
            if (p.getProductIds() == null || p.getProductIds().isEmpty()) return BigDecimal.ZERO;
            BigDecimal total = BigDecimal.ZERO;
            for (CartItemDto it : cart.getItems()) {
                if (p.getProductIds().contains(it.getProductId())) {
                    int eligibleQty = it.getQuantity();
                    if (p.getMinQuantity() != null && eligibleQty < p.getMinQuantity()) continue;
                    if (p.getMaxQuantity() != null) {
                        eligibleQty = Math.min(eligibleQty, p.getMaxQuantity());
                    }
                    BigDecimal itemDiscount = it.getPrice()
                            .multiply(BigDecimal.valueOf(eligibleQty))
                            .multiply(p.getDiscountPercent().divide(BigDecimal.valueOf(100)));
                    total = total.add(itemDiscount);
                }
            }
            return total.setScale(2, RoundingMode.HALF_UP);
        } catch (Exception e) {
            return BigDecimal.ZERO;
        }
    }

    @Override
    public BigDecimal applyAndReturnTotalDiscount(CartDto cart, Coupon coupon) {
        return calculateDiscount(cart, coupon);
    }
}
