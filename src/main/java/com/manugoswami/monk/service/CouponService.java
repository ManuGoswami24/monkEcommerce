package com.manugoswami.monk.service;


import com.fasterxml.jackson.databind.ObjectMapper;
import com.manugoswami.monk.dto.ApplicableCouponDto;
import com.manugoswami.monk.dto.CartDto;
import com.manugoswami.monk.dto.CartItemDto;
import com.manugoswami.monk.dto.UpdatedCartDto;
import com.manugoswami.monk.dto.UpdatedItemDto;
import com.manugoswami.monk.model.Coupon;
import com.manugoswami.monk.processor.CouponProcessor;
import com.manugoswami.monk.processor.ProcessorRegistry;
import com.manugoswami.monk.repository.CouponRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class CouponService {

    private final CouponRepository couponRepository;
    private final ProcessorRegistry registry;
    private final ObjectMapper om = new ObjectMapper();

    public CouponService(CouponRepository couponRepository, ProcessorRegistry registry) {
        this.couponRepository = couponRepository;
        this.registry = registry;
    }

    // CRUD
    public Coupon create(Coupon c) {
        return couponRepository.save(c);
    }

    public List<Coupon> getAll() {
        return couponRepository.findAll();
    }

    public Coupon getById(Long id) {
        return couponRepository.findById(id)
                .orElseThrow(() ->
                        new ResponseStatusException(HttpStatus.NOT_FOUND, "Coupon not found"));
    }

    public Coupon update(Long id, Coupon payload) {
        Coupon exist = getById(id);
        exist.setCode(payload.getCode());
        exist.setType(payload.getType());
        exist.setPayloadJson(payload.getPayloadJson());
        exist.setEnabled(payload.getEnabled());
        exist.setExpiresAt(payload.getExpiresAt());
        return couponRepository.save(exist);
    }

    public void delete(Long id) {
        couponRepository.deleteById(id);
    }

    // BUSINESS --------------------------------------------------------------
    public List<ApplicableCouponDto> findApplicableCoupons(CartDto cart) {
        Instant now = Instant.now();

        List<Coupon> all = couponRepository.findAll()
                .stream()
                .filter(c -> Boolean.TRUE.equals(c.getEnabled()))
                .filter(c -> c.getExpiresAt() == null || c.getExpiresAt().isAfter(now))
                .collect(Collectors.toList());

        List<ApplicableCouponDto> result = new ArrayList<>();

        for (Coupon c : all) {
            CouponProcessor p = registry.getProcessor(c.getType());
            if (p == null) {
                continue;
            }
            try {
                if (p.isApplicable(cart, c)) {
                    BigDecimal discount = p.calculateDiscount(cart, c);
                    if (discount.compareTo(BigDecimal.ZERO) > 0) {
                        result.add(new ApplicableCouponDto(c.getId(), c.getType(), discount));
                    }
                }
            } catch (Exception ignored) {
            }
        }
        return result;
    }

    public UpdatedCartDto applyCoupon(Long couponId, CartDto cart) {
        Coupon coupon = getById(couponId);
        Instant now = Instant.now();

        if (!Boolean.TRUE.equals(coupon.getEnabled())
                || (coupon.getExpiresAt() != null && coupon.getExpiresAt().isBefore(now))) {
            throw new ResponseStatusException(HttpStatus.UNPROCESSABLE_ENTITY, "Coupon not active");
        }

        CouponProcessor p = registry.getProcessor(coupon.getType());
        if (p == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Unsupported coupon type");
        }

        if (!p.isApplicable(cart, coupon)) {
            throw new ResponseStatusException(HttpStatus.UNPROCESSABLE_ENTITY, "Coupon not applicable to cart");
        }

        BigDecimal totalDiscount = p.applyAndReturnTotalDiscount(cart, coupon);

        List<UpdatedItemDto> updated = new ArrayList<>();
        BigDecimal totalPrice = cart.getItems()
                .stream()
                .map(i -> i.getPrice().multiply(new BigDecimal(i.getQuantity())))
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        // COUPON TYPE HANDLING ---------------------------------------------------
        if ("CART_WISE".equals(coupon.getType())) {
            applyCartWiseDiscount(cart, totalDiscount, updated, totalPrice);

        } else if ("PRODUCT_WISE".equals(coupon.getType())) {
            applyProductWiseDiscount(cart, coupon, totalDiscount, updated);

        } else if ("BXGY".equals(coupon.getType())) {
            applyBxGyDiscount(cart, coupon, updated);

        } else {
            // fallback: no per-item distribution
            for (CartItemDto it : cart.getItems()) {
                updated.add(new UpdatedItemDto(
                        it.getProductId(),
                        it.getQuantity(),
                        it.getPrice(),
                        BigDecimal.ZERO
                ));
            }
        }

        BigDecimal aggregatedDiscount = updated.stream()
                .map(UpdatedItemDto::getTotalDiscount)
                .reduce(BigDecimal.ZERO, BigDecimal::add)
                .setScale(2, BigDecimal.ROUND_HALF_UP);

        BigDecimal finalPrice = totalPrice
                .subtract(aggregatedDiscount)
                .setScale(2, BigDecimal.ROUND_HALF_UP);

        return new UpdatedCartDto(
                updated,
                totalPrice.setScale(2, BigDecimal.ROUND_HALF_UP),
                aggregatedDiscount,
                finalPrice
        );
    }

    // HELPERS -------------------------------------------------------------------
    private void applyCartWiseDiscount(
            CartDto cart,
            BigDecimal totalDiscount,
            List<UpdatedItemDto> updated,
            BigDecimal totalPrice
    ) {
        if (totalPrice.compareTo(BigDecimal.ZERO) == 0) {
            for (CartItemDto it : cart.getItems()) {
                updated.add(new UpdatedItemDto(
                        it.getProductId(),
                        it.getQuantity(),
                        it.getPrice(),
                        BigDecimal.ZERO
                ));
            }
            return;
        }

        BigDecimal distributed = BigDecimal.ZERO;

        for (int i = 0; i < cart.getItems().size(); i++) {
            CartItemDto it = cart.getItems().get(i);
            BigDecimal itemTotal = it.getPrice().multiply(new BigDecimal(it.getQuantity()));

            BigDecimal share = itemTotal
                    .multiply(totalDiscount)
                    .divide(totalPrice, 8, BigDecimal.ROUND_HALF_UP)
                    .setScale(2, BigDecimal.ROUND_HALF_UP);

            if (i == cart.getItems().size() - 1) {
                share = totalDiscount.subtract(distributed);
            }

            distributed = distributed.add(share);

            updated.add(new UpdatedItemDto(
                    it.getProductId(),
                    it.getQuantity(),
                    it.getPrice(),
                    share
            ));
        }
    }

    private void applyProductWiseDiscount(
            CartDto cart,
            Coupon coupon,
            BigDecimal totalDiscount,
            List<UpdatedItemDto> updated
    ) {
        try {
            com.manugoswami.monk.payload.ProductWisePayload pw =
                    om.readValue(coupon.getPayloadJson(),
                            com.manugoswami.monk.payload.ProductWisePayload.class);

            BigDecimal appliedTotal = BigDecimal.ZERO;

            for (CartItemDto it : cart.getItems()) {
                BigDecimal itemDiscount = BigDecimal.ZERO;

                if (pw.getProductIds() != null && pw.getProductIds().contains(it.getProductId())) {
                    int eligibleQty = it.getQuantity();

                    if (pw.getMinQuantity() != null && eligibleQty < pw.getMinQuantity()) {
                        eligibleQty = 0;
                    }

                    if (pw.getMaxQuantity() != null) {
                        eligibleQty = Math.min(eligibleQty, pw.getMaxQuantity());
                    }

                    if (eligibleQty > 0 && pw.getDiscountPercent() != null) {
                        itemDiscount = it.getPrice()
                                .multiply(new BigDecimal(eligibleQty))
                                .multiply(pw.getDiscountPercent().divide(new BigDecimal("100")));
                    }
                }

                appliedTotal = appliedTotal.add(itemDiscount);

                updated.add(new UpdatedItemDto(
                        it.getProductId(),
                        it.getQuantity(),
                        it.getPrice(),
                        itemDiscount.setScale(2, BigDecimal.ROUND_HALF_UP)
                ));
            }

            BigDecimal diff = totalDiscount
                    .subtract(appliedTotal)
                    .setScale(2, BigDecimal.ROUND_HALF_UP);

            if (diff.compareTo(BigDecimal.ZERO) != 0 && !updated.isEmpty()) {
                UpdatedItemDto first = updated.get(0);
                first.setTotalDiscount(first.getTotalDiscount().add(diff));
            }

        } catch (Exception ex) {
            for (CartItemDto it : cart.getItems()) {
                updated.add(new UpdatedItemDto(
                        it.getProductId(),
                        it.getQuantity(),
                        it.getPrice(),
                        BigDecimal.ZERO
                ));
            }
        }
    }

    private void applyBxGyDiscount(
            CartDto cart,
            Coupon coupon,
            List<UpdatedItemDto> updated
    ) {
        try {
            com.manugoswami.monk.payload.BxGyPayload bx =
                    om.readValue(coupon.getPayloadJson(),
                            com.manugoswami.monk.payload.BxGyPayload.class);

            int totalBuyQty = cart.getItems()
                    .stream()
                    .filter(i -> bx.getBuyProductIds() != null
                            && bx.getBuyProductIds().contains(i.getProductId()))
                    .mapToInt(CartItemDto::getQuantity)
                    .sum();

            int required = bx.getBuyRequiredCount() == null ? Integer.MAX_VALUE : bx.getBuyRequiredCount();

            int timesApplicable = totalBuyQty / required;
            if (timesApplicable < 0) {
                timesApplicable = 0;
            }

            if (bx.getRepetitionLimit() != null && bx.getRepetitionLimit() > 0) {
                timesApplicable = Math.min(timesApplicable, bx.getRepetitionLimit());
            }

            int totalFreeAllowed = timesApplicable * (bx.getGetQuantity() == null ? 0 : bx.getGetQuantity());

            // Prepare
            Map<Long, UpdatedItemDto> map = new LinkedHashMap<>();
            for (CartItemDto it : cart.getItems()) {
                map.put(it.getProductId(), new UpdatedItemDto(
                        it.getProductId(),
                        it.getQuantity(),
                        it.getPrice(),
                        BigDecimal.ZERO
                ));
            }

            if (totalFreeAllowed > 0 && bx.getGetProductIds() != null) {
                List<CartItemDto> eligible = cart.getItems()
                        .stream()
                        .filter(i -> bx.getGetProductIds().contains(i.getProductId()))
                        .sorted((a, b) -> b.getPrice().compareTo(a.getPrice()))
                        .collect(Collectors.toList());

                int remaining = totalFreeAllowed;

                for (CartItemDto it : eligible) {
                    if (remaining <= 0) {
                        break;
                    }

                    int freeQty = Math.min(it.getQuantity(), remaining);

                    BigDecimal disc = it.getPrice().multiply(new BigDecimal(freeQty));

                    UpdatedItemDto u = map.get(it.getProductId());
                    u.setTotalDiscount(u.getTotalDiscount().add(disc));

                    remaining -= freeQty;
                }
            }

            updated.addAll(map.values());

        } catch (Exception ex) {
            for (CartItemDto it : cart.getItems()) {
                updated.add(new UpdatedItemDto(
                        it.getProductId(),
                        it.getQuantity(),
                        it.getPrice(),
                        BigDecimal.ZERO
                ));
            }
        }
    }
}