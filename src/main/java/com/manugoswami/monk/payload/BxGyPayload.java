package com.manugoswami.monk.payload;

import lombok.Data;

import java.util.List;

@Data
public class BxGyPayload {
    private List<Long> buyProductIds;
    private Integer buyRequiredCount; // buy N items from buyProductIds
    private List<Long> getProductIds;
    private Integer getQuantity; // how many free per application (e.g., b2g1 -> getQuantity=1)
    private Integer repetitionLimit;
}
