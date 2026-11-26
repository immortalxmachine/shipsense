package com.shipsense.dto;

import com.shipsense.entity.Hub;

import java.math.BigDecimal;

/** API view of a {@link Hub}. */
public record HubResponse(Integer id, String code, String name, String city,
                          String region, BigDecimal lat, BigDecimal lng) {

    public static HubResponse from(Hub h) {
        return new HubResponse(h.getId(), h.getCode(), h.getName(), h.getCity(),
                h.getRegion(), h.getLat(), h.getLng());
    }
}
