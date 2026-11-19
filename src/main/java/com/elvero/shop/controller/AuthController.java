package com.elvero.shop.controller;

import com.elvero.shop.dto.AuthRequests.EmailRequest;
import com.elvero.shop.dto.AuthRequests.LoginWithPasswordRequest;
import com.elvero.shop.dto.AuthRequests.RefreshTokenRequest;
import com.elvero.shop.dto.AuthRequests.SendRegisterOtpRequest;
import com.elvero.shop.dto.AuthRequests.SetPasswordRequest;
import com.elvero.shop.dto.AuthRequests.VerifyOtpRequest;
import com.elvero.shop.security.AuthenticatedUser;
import com.elvero.shop.service.AuthService;
import java.util.LinkedHashMap;
import java.util.Map;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Port of routes/auth.route.js + controllers/auth.controller.js.
 * Success bodies are { success: true, ...result } except password login / set password, which nest the
 * result under "data" (as Node did). Errors come from the global handler as { success: false, message }.
 */
@RestController
@RequestMapping("/api/ecommerce/auth")
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/refresh-token")
    public Map<String, Object> refreshToken(@RequestBody RefreshTokenRequest request) {
        return flat(authService.refreshToken(request.refreshToken()));
    }

    @PostMapping("/register/send-otp")
    public Map<String, Object> sendRegisterOtp(@RequestBody SendRegisterOtpRequest request) {
        return flat(authService.sendRegisterOtp(request));
    }

    @PostMapping("/check-existing-user")
    public Map<String, Object> checkExistingUser(@RequestBody EmailRequest request) {
        return flat(authService.existingUser(request.email()));
    }

    @GetMapping("/my-addresses")
    public Map<String, Object> myAddresses(@AuthenticationPrincipal AuthenticatedUser user) {
        return nested(authService.getCustomerAddresses(user.userId()));
    }

    @PostMapping("/register/verify-otp-create-customer")
    public ResponseEntity<Map<String, Object>> verifyRegisterOtp(@RequestBody VerifyOtpRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(flat(authService.verifyRegisterOtp(request)));
    }

    @PostMapping("/login")
    public ResponseEntity<Map<String, Object>> sendLoginOtp(@RequestBody EmailRequest request) {
        Map<String, Object> result = authService.sendLoginOtp(request);
        if ("NOT_REGISTERED".equals(result.get("status"))) {
            Map<String, Object> body = new LinkedHashMap<>();
            body.put("success", false);
            body.putAll(result);
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(body);
        }
        return ResponseEntity.ok(flat(result));
    }

    @PostMapping("/login-with-password")
    public Map<String, Object> loginWithPassword(@RequestBody LoginWithPasswordRequest request) {
        return nested(authService.loginWithPassword(request));
    }

    @PostMapping("/password-set")
    public Map<String, Object> setPassword(@AuthenticationPrincipal AuthenticatedUser user,
                                           @RequestBody SetPasswordRequest request) {
        return nested(authService.setPassword(user.userId(), request.password()));
    }

    @PostMapping("/login/verify-otp")
    public Map<String, Object> verifyLoginOtp(@RequestBody VerifyOtpRequest request) {
        return flat(authService.verifyLoginOtp(request));
    }

    @PostMapping("/logout")
    public Map<String, Object> logout(@RequestBody RefreshTokenRequest request) {
        Map<String, Object> result = authService.logout(request.refreshToken());
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("success", true);
        body.put("message", result.get("message"));
        return body;
    }

    /** { success: true, ...result } */
    private static Map<String, Object> flat(Map<String, Object> result) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("success", true);
        body.putAll(result);
        return body;
    }

    /** { success: true, data: result } */
    private static Map<String, Object> nested(Map<String, Object> result) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("success", true);
        body.put("data", result);
        return body;
    }
}
