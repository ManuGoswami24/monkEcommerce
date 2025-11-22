package com.manugoswami.monk.processor;

import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Component
public class ProcessorRegistry {
    private final Map<String, CouponProcessor> registry = new HashMap<>();

    public ProcessorRegistry(List<CouponProcessor> processors) {
        for (CouponProcessor p : processors) {
            registry.put(p.type(), p);
        }
    }

    public CouponProcessor getProcessor(String type) {
        return registry.get(type);
    }
}
