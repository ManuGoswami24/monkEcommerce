package com.manugoswami.monk.processor.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.manugoswami.monk.dto.CartDto;
import com.manugoswami.monk.dto.CartItemDto;
import com.manugoswami.monk.model.Coupon;
import com.manugoswami.monk.payload.BxGyPayload;
import com.manugoswami.monk.processor.CouponProcessor;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

public class BxGyProcessor implements CouponProcessor {

    private final ObjectMapper om = new ObjectMapper();

    @Override
    public String type() {
        return "BXGY";
    }

    @Override
    public boolean isApplicable(CartDto cart, Coupon coupon) {
        try {
            BxGyPayload p = om.readValue(coupon.getPayloadJson(), BxGyPayload.class);
            if (p.getBuyProductIds() == null || p.getBuyProductIds().isEmpty()) return false;
            if (p.getBuyRequiredCount() == null || p.getBuyRequiredCount() <= 0) return false;
            int totalBuyQty = cart.getItems().stream()
                    .filter(i -> p.getBuyProductIds().contains(i.getProductId()))
                    .mapToInt(CartItemDto::getQuantity)
                    .sum();
            return totalBuyQty >= p.getBuyRequiredCount();
        } catch (Exception e) {
            return false;
        }
    }

    @Override
    public BigDecimal calculateDiscount(CartDto cart, Coupon coupon) {
        try {
            BxGyPayload p = om.readValue(coupon.getPayloadJson(), BxGyPayload.class);
            int totalBuyQty = cart.getItems().stream()
                    .filter(i -> p.getBuyProductIds().contains(i.getProductId()))
                    .mapToInt(CartItemDto::getQuantity)
                    .sum();

            int timesApplicable = totalBuyQty / p.getBuyRequiredCount();
            if (timesApplicable <= 0) return BigDecimal.ZERO;
            if (p.getRepetitionLimit() != null && p.getRepetitionLimit() > 0) {
                timesApplicable = Math.min(timesApplicable, p.getRepetitionLimit());
            }

            int totalFreeAllowed = timesApplicable * (p.getGetQuantity() == null ? 0 : p.getGetQuantity());
            if (totalFreeAllowed <= 0) return BigDecimal.ZERO;

            // choose eligible get items in cart
            List<CartItemDto> getEligible = cart.getItems().stream()
                    .filter(i -> p.getGetProductIds() != null && p.getGetProductIds().contains(i.getProductId()))
                    .sorted(Comparator.comparing(CartItemDto::getPrice).reversed()) // highest price first
                    .collect(Collectors.toList());

            int remaining = totalFreeAllowed;
            BigDecimal totalDiscount = BigDecimal.ZERO;
            for (CartItemDto it : getEligible) {
                if (remaining <= 0) break;
                int freeQty = Math.min(it.getQuantity(), remaining);
                totalDiscount = totalDiscount.add(it.getPrice().multiply(BigDecimal.valueOf(freeQty)));
                remaining -= freeQty;
            }
            return totalDiscount.setScale(2, RoundingMode.HALF_UP);
        } catch (Exception e) {
            return BigDecimal.ZERO;
        }
    }

    @Override
    public BigDecimal applyAndReturnTotalDiscount(CartDto cart, Coupon coupon) {
        return calculateDiscount(cart, coupon);
    }
}
