package com.elvero.shop.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.Map;

/** Request bodies of /api/ecommerce/auth. Validation messages live in AuthService (same text as Node). */
public final class AuthRequests {

    private AuthRequests() {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record RefreshTokenRequest(String refreshToken) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record SendRegisterOtpRequest(
            @JsonProperty("first_name") String firstName,
            @JsonProperty("last_name") String lastName,
            String email,
            String password,
            Map<String, Object> address) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record EmailRequest(String email) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record VerifyOtpRequest(String email, String otp) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record LoginWithPasswordRequest(String email, String password) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record SetPasswordRequest(String password) {
    }
}
