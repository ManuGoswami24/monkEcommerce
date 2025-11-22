package com.manugoswami.monk.controller;

import com.manugoswami.monk.dto.ApplicableCouponDto;
import com.manugoswami.monk.dto.CartDto;
import com.manugoswami.monk.dto.UpdatedCartDto;
import com.manugoswami.monk.model.Coupon;
import com.manugoswami.monk.service.CouponService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api")
public class CouponController {

    private final CouponService service;

    public CouponController(CouponService service) {
        this.service = service;
    }

    // CRUD
    @PostMapping("/coupons")
    public ResponseEntity<Coupon> create(@RequestBody Coupon coupon) {
        return ResponseEntity.ok(service.create(coupon));
    }

    @GetMapping("/coupons")
    public ResponseEntity<List<Coupon>> list() {
        return ResponseEntity.ok(service.getAll());
    }

    @GetMapping("/coupons/{id}")
    public ResponseEntity<Coupon> get(@PathVariable Long id) {
        return ResponseEntity.ok(service.getById(id));
    }

    @PutMapping("/coupons/{id}")
    public ResponseEntity<Coupon> update(@PathVariable Long id, @RequestBody Coupon coupon) {
        return ResponseEntity.ok(service.update(id, coupon));
    }

    @DeleteMapping("/coupons/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        service.delete(id);
        return ResponseEntity.noContent().build();
    }

    // Business endpoints
    @PostMapping("/applicable-coupons")
    public ResponseEntity<List<ApplicableCouponDto>> applicable(@RequestBody CartDto cart) {
        return ResponseEntity.ok(service.findApplicableCoupons(cart));
    }

    @PostMapping("/apply-coupon/{id}")
    public ResponseEntity<UpdatedCartDto> apply(@PathVariable Long id, @RequestBody CartDto cart) {
        return ResponseEntity.ok(service.applyCoupon(id, cart));
    }
}
