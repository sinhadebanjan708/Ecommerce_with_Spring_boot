package com.elvero.shop.dto;

import com.elvero.shop.entity.Address;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/** Address as the Node API returned it: snake_case fields, decimals as strings, camelCase timestamps. */
public record AddressDto(
        Long id,
        @JsonProperty("user_id") Long userId,
        @JsonProperty("full_name") String fullName,
        @JsonProperty("address_line1") String addressLine1,
        String city,
        String state,
        String pincode,
        String latitude,
        String longitude,
        @JsonProperty("location_accuracy") String locationAccuracy,
        @JsonProperty("is_default") Boolean isDefault,
        LocalDateTime createdAt,
        LocalDateTime updatedAt) {

    public static AddressDto from(Address a) {
        return new AddressDto(a.getId(), a.getUser().getId(), a.getFullName(), a.getAddressLine1(), a.getCity(),
                a.getState(), a.getPincode(), plain(a.getLatitude()), plain(a.getLongitude()),
                plain(a.getLocationAccuracy()), a.getIsDefault(), a.getCreatedAt(), a.getUpdatedAt());
    }

    static String plain(BigDecimal value) {
        return value == null ? null : value.toPlainString();
    }
}
