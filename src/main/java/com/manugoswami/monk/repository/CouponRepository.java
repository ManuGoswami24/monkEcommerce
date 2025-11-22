package com.manugoswami.monk.repository;

import com.manugoswami.monk.model.Coupon;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;

@Repository
public interface CouponRepository extends JpaRepository<Coupon, Long> {
    List<Coupon> findByEnabledTrueAndExpiresAtAfter(Instant now);
}
