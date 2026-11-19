package com.elvero.shop.dto;

import com.elvero.shop.entity.Address;
import com.fasterxml.jackson.annotation.JsonProperty;

/** The reduced address list returned by login/verify-otp. */
public record AddressSummaryDto(
        Long id,
        @JsonProperty("address_line1") String addressLine1,
        String city,
        String state,
        String pincode,
        String latitude,
        String longitude,
        @JsonProperty("location_accuracy") String locationAccuracy,
        @JsonProperty("is_default") Boolean isDefault) {

    public static AddressSummaryDto from(Address a) {
        return new AddressSummaryDto(a.getId(), a.getAddressLine1(), a.getCity(), a.getState(), a.getPincode(),
                AddressDto.plain(a.getLatitude()), AddressDto.plain(a.getLongitude()),
                AddressDto.plain(a.getLocationAccuracy()), a.getIsDefault());
    }
}
