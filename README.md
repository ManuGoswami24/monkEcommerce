# Coupon Service – Documentation

## Implemented Scope

The service supports applying discount coupons on a cart based on:

* Flat amount discounts
* Percentage-based discounts
* Product-specific applicability

The system calculates effective discount per eligible item and returns updated cart totals and per-item breakdowns. Business logic is split into processors for maintainability.

## Assumptions

* Only one coupon is applied per request.
* Product data received in the cart request is considered accurate.
* Discount cannot reduce payable value below zero.
* No dependency on user/session context.

## Limitations

* No coupon lifecycle management (expiry, max usage, activation windows).
* No enforcement of cart value thresholds or eligibility rules.
* No stacking or combination of multiple coupons.
* No persistent product or cart catalog.

## Considered but Not Implemented

| Case                             | Reason                                            |
| -------------------------------- | ------------------------------------------------- |
| Tiered or slab-based discounting | Requires additional rule engine logic             |
| Buy-X-Get-Y type promotions      | Depends on detailed product inventory tracking    |
| Multi-coupon application         | Needs compatibility and precedence rules          |
| Coupon usage history per user    | Requires authentication and user state management |

## Notes

Service runs on Spring Boot with H2 in-memory database for quick evaluation and clean lifecycle across runs.
