package com.elvero.shop.service;

import com.auth0.jwt.exceptions.JWTVerificationException;
import com.auth0.jwt.interfaces.DecodedJWT;
import com.elvero.shop.dto.AddressDto;
import com.elvero.shop.dto.AddressSummaryDto;
import com.elvero.shop.dto.AuthRequests.EmailRequest;
import com.elvero.shop.dto.AuthRequests.LoginWithPasswordRequest;
import com.elvero.shop.dto.AuthRequests.SendRegisterOtpRequest;
import com.elvero.shop.dto.AuthRequests.VerifyOtpRequest;
import com.elvero.shop.entity.Address;
import com.elvero.shop.entity.Otp;
import com.elvero.shop.entity.PendingRegistration;
import com.elvero.shop.entity.RefreshToken;
import com.elvero.shop.entity.Role;
import com.elvero.shop.entity.RolePermission;
import com.elvero.shop.entity.User;
import com.elvero.shop.entity.UserRole;
import com.elvero.shop.exception.ApiException;
import com.elvero.shop.repository.AddressRepository;
import com.elvero.shop.repository.OtpRepository;
import com.elvero.shop.repository.PendingRegistrationRepository;
import com.elvero.shop.repository.RefreshTokenRepository;
import com.elvero.shop.repository.RoleRepository;
import com.elvero.shop.repository.RolePermissionRepository;
import com.elvero.shop.repository.UserRepository;
import com.elvero.shop.repository.UserRoleRepository;
import com.elvero.shop.security.JwtService;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.math.BigDecimal;
import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;

/**
 * Port of auth.service.js. Responses are built as maps so every endpoint keeps the exact JSON
 * the Node API produced (snake_case keys, accessToken/refreshToken camelCase).
 */
@Service
public class AuthService {

    private static final Logger log = LoggerFactory.getLogger(AuthService.class);

    private static final SecureRandom RANDOM = new SecureRandom();
    private static final String OTP_REGISTER = "register-customer";
    private static final String OTP_LOGIN = "login";
    private static final String ADMIN_EMAIL = "admin@example.com";

    private final UserRepository userRepository;
    private final OtpRepository otpRepository;
    private final RoleRepository roleRepository;
    private final UserRoleRepository userRoleRepository;
    private final AddressRepository addressRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final RolePermissionRepository rolePermissionRepository;
    private final PendingRegistrationRepository pendingRegistrationRepository;
    private final EmailService emailService;
    private final TokenService tokenService;
    private final JwtService jwtService;
    private final PasswordEncoder passwordEncoder;
    private final ObjectMapper objectMapper;
    private final TransactionTemplate tx;

    public AuthService(UserRepository userRepository, OtpRepository otpRepository, RoleRepository roleRepository,
                       UserRoleRepository userRoleRepository, AddressRepository addressRepository,
                       RefreshTokenRepository refreshTokenRepository,
                       RolePermissionRepository rolePermissionRepository,
                       PendingRegistrationRepository pendingRegistrationRepository,
                       EmailService emailService, TokenService tokenService, JwtService jwtService,
                       PasswordEncoder passwordEncoder, ObjectMapper objectMapper,
                       PlatformTransactionManager transactionManager) {
        this.userRepository = userRepository;
        this.otpRepository = otpRepository;
        this.roleRepository = roleRepository;
        this.userRoleRepository = userRoleRepository;
        this.addressRepository = addressRepository;
        this.refreshTokenRepository = refreshTokenRepository;
        this.rolePermissionRepository = rolePermissionRepository;
        this.pendingRegistrationRepository = pendingRegistrationRepository;
        this.emailService = emailService;
        this.tokenService = tokenService;
        this.jwtService = jwtService;
        this.passwordEncoder = passwordEncoder;
        this.objectMapper = objectMapper;
        this.tx = new TransactionTemplate(transactionManager);
    }

    // ------------------------------------------------------------------ refresh token

    @Transactional
    public Map<String, Object> refreshToken(String refreshToken) {
        if (isBlank(refreshToken)) {
            throw ApiException.unauthorized("Refresh token is required");
        }

        DecodedJWT decoded;
        try {
            decoded = jwtService.verifyRefreshToken(refreshToken);
        } catch (JWTVerificationException e) {
            throw ApiException.unauthorized("Invalid or expired refresh token");
        }

        Long userId = decoded.getClaim("user_id").asLong();
        Object role = JwtService.roleClaim(decoded);
        if (userId == null || decoded.getId() == null || role == null) {
            throw ApiException.unauthorized("Invalid refresh token");
        }

        RefreshToken existing = refreshTokenRepository
                .findActiveForUpdate(userId, TokenService.hashToken(refreshToken))
                .orElseThrow(() -> ApiException.unauthorized("Refresh token has already been used or revoked"));

        if (!existing.getExpiresAt().isAfter(LocalDateTime.now())) {
            throw ApiException.unauthorized("Refresh token expired");
        }

        // Node only checked that status was truthy (always true); here it must really be "active".
        User user = userRepository.findById(userId)
                .filter(u -> "active".equals(u.getStatus()))
                .orElseThrow(() -> ApiException.unauthorized("User account is inactive or unavailable"));

        existing.setRevokedAt(LocalDateTime.now());
        AuthTokens tokens = tokenService.generateAuthTokens(user, role);

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("message", "Token refreshed successfully");
        putTokens(result, tokens);
        return result;
    }

    // ------------------------------------------------------------------ existing user check

    @Transactional(readOnly = true)
    public Map<String, Object> existingUser(String email) {
        if (isBlank(email)) {
            throw ApiException.badRequest("Email is required");
        }
        Map<String, Object> result = new LinkedHashMap<>();
        User existing = userRepository.findByEmail(email).orElse(null);
        if (existing == null) {
            result.put("exists", false);
            result.put("message", "Email is available");
            return result;
        }
        if (ADMIN_EMAIL.equals(existing.getEmail())) {
            result.put("exists", true);
            result.put("message", "Welcome Admin");
            return result;
        }
        throw ApiException.conflict("User Already Exists!");
    }

    // ------------------------------------------------------------------ registration

    public Map<String, Object> sendRegisterOtp(SendRegisterOtpRequest request) {
        if (isBlank(request.firstName()) || isBlank(request.lastName()) || isBlank(request.email())) {
            throw ApiException.badRequest("Name and email are required");
        }

        // The Node validation asked for address_line_1 / postal_code, but account creation read
        // address_line1 / pincode. Accept either spelling and store one canonical shape.
        Map<String, Object> address = request.address();
        String line1 = address == null ? null : text(address, "address_line1", "address_line_1");
        String city = address == null ? null : text(address, "city");
        String state = address == null ? null : text(address, "state");
        String pincode = address == null ? null : text(address, "pincode", "postal_code");
        if (line1 == null || city == null || state == null || pincode == null) {
            throw ApiException.badRequest("Complete delivery address is required");
        }

        String password = request.password();
        if (password != null && !password.isEmpty() && password.length() < 8) {
            throw ApiException.badRequest("Password must be at least 8 characters");
        }

        String email = request.email().trim().toLowerCase();
        if (userRepository.existsByEmail(email)) {
            throw ApiException.badRequest("Email is already registered");
        }

        String passwordHash = (password == null || password.isEmpty()) ? null : passwordEncoder.encode(password);

        Map<String, Object> canonical = new LinkedHashMap<>();
        canonical.put("phone", text(address, "phone"));
        canonical.put("address_line1", line1);
        canonical.put("city", city);
        canonical.put("state", state);
        canonical.put("pincode", pincode);
        canonical.put("latitude", decimalText(address.get("latitude")));
        canonical.put("longitude", decimalText(address.get("longitude")));
        canonical.put("location_accuracy", decimalText(address.get("location_accuracy")));
        String addressJson;
        try {
            addressJson = objectMapper.writeValueAsString(canonical);
        } catch (JsonProcessingException e) {
            throw ApiException.badRequest("Invalid address");
        }

        String otpCode = generateOtp();
        LocalDateTime expiresAt = LocalDateTime.now().plusMinutes(10);

        tx.executeWithoutResult(status -> {
            pendingRegistrationRepository.deleteByEmailAddress(email);
            otpRepository.deleteByEmailAndType(email, OTP_REGISTER);

            PendingRegistration pending = new PendingRegistration();
            pending.setFirstName(request.firstName());
            pending.setLastName(request.lastName());
            pending.setEmail(email);
            pending.setPasswordHash(passwordHash);
            pending.setAddress(addressJson);
            pending.setExpiresAt(expiresAt);
            pendingRegistrationRepository.save(pending);

            otpRepository.save(newOtp(email, OTP_REGISTER, otpCode, expiresAt));
        });

        try {
            emailService.sendEmailOtp(email, request.firstName(), request.lastName(), otpCode);
        } catch (RuntimeException e) {
            tx.executeWithoutResult(status -> {
                pendingRegistrationRepository.deleteByEmailAddress(email);
                otpRepository.deleteByEmailAndType(email, OTP_REGISTER);
            });
            throw ApiException.badRequest("Unable to send registration OTP");
        }

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("message", "Registration OTP sent successfully");
        result.put("channel", "email");
        result.put("expires_in", "10 minutes");
        return result;
    }

    @Transactional
    public Map<String, Object> verifyRegisterOtp(VerifyOtpRequest request) {
        if (isBlank(request.email()) || isBlank(request.otp())) {
            throw ApiException.badRequest("Email and OTP are required");
        }
        String email = request.email().trim().toLowerCase();
        LocalDateTime now = LocalDateTime.now();

        Otp otp = otpRepository.findValidForUpdate(email, request.otp(), OTP_REGISTER, now)
                .orElseThrow(() -> ApiException.badRequest("Invalid or expired OTP"));

        PendingRegistration pending = pendingRegistrationRepository.findActiveForUpdate(email, now)
                .orElseThrow(() -> ApiException.badRequest(
                        "Registration session expired. Please request a new OTP."));

        if (userRepository.existsByEmail(email)) {
            throw ApiException.badRequest("User already exists");
        }

        User user = new User();
        user.setFirstName(pending.getFirstName());
        user.setLastName(pending.getLastName());
        user.setEmail(pending.getEmail());
        user.setPasswordHash(pending.getPasswordHash());
        user.setEmailVerified(true);
        user.setStatus("active");
        user = userRepository.save(user);

        JsonNode addr;
        try {
            addr = objectMapper.readTree(pending.getAddress());
        } catch (JsonProcessingException e) {
            throw ApiException.badRequest("Stored registration address is invalid");
        }
        Address address = new Address();
        address.setUser(user);
        address.setFullName(pending.getFirstName() + " " + pending.getLastName());
        address.setAddressLine1(addr.path("address_line1").asText(null));
        address.setCity(addr.path("city").asText(null));
        address.setState(addr.path("state").asText(null));
        address.setPincode(addr.path("pincode").asText(null));
        address.setLatitude(decimalOrNull(addr.path("latitude")));
        address.setLongitude(decimalOrNull(addr.path("longitude")));
        address.setLocationAccuracy(decimalOrNull(addr.path("location_accuracy")));
        address.setIsDefault(true);
        address = addressRepository.save(address);

        Role customerRole = roleRepository.findByRoleName("CUSTOMER")
                .orElseThrow(() -> ApiException.badRequest("CUSTOMER role not found in database"));
        UserRole userRole = new UserRole();
        userRole.setUser(user);
        userRole.setRole(customerRole);
        userRoleRepository.save(userRole);

        otp.setIsUsed(true);
        pendingRegistrationRepository.delete(pending);

        // Registration tokens carry a single role string; login tokens carry a list (same as Node).
        AuthTokens tokens = tokenService.generateAuthTokens(user, "CUSTOMER");

        Map<String, Object> userView = new LinkedHashMap<>();
        userView.put("id", user.getId());
        userView.put("first_name", user.getFirstName());
        userView.put("last_name", user.getLastName());
        userView.put("email", user.getEmail());
        userView.put("role", "CUSTOMER");

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("message", "Registration successful");
        result.put("user", userView);
        result.put("address", AddressDto.from(address));
        putTokens(result, tokens);
        return result;
    }

    // ------------------------------------------------------------------ OTP login

    public Map<String, Object> sendLoginOtp(EmailRequest request) {
        if (isBlank(request.email())) {
            throw ApiException.badRequest("Email is required");
        }
        User user = userRepository.findByEmailAndStatus(request.email(), "active").orElse(null);
        Map<String, Object> result = new LinkedHashMap<>();
        if (user == null) {
            result.put("status", "NOT_REGISTERED");
            result.put("message", "Account does not exist.");
            return result;
        }

        List<String> roles = userRoleRepository.findWithRoleByUserId(user.getId()).stream()
                .map(ur -> ur.getRole().getRoleName())
                .toList();
        if (roles.isEmpty()) {
            throw ApiException.badRequest("No role assigned to this account");
        }

        String otpCode = generateOtp();
        otpRepository.save(newOtp(user.getEmail(), OTP_LOGIN, otpCode, LocalDateTime.now().plusMinutes(5)));
        emailService.sendEmailOtp(user.getEmail(), user.getFirstName(), user.getLastName(), otpCode);

        result.put("status", "OTP_SENT");
        result.put("message", "Login OTP sent successfully");
        result.put("email", user.getEmail());
        result.put("roles", roles);
        return result;
    }

    @Transactional
    public Map<String, Object> verifyLoginOtp(VerifyOtpRequest request) {
        if (isBlank(request.email()) || isBlank(request.otp())) {
            throw ApiException.badRequest("Email and OTP are required");
        }
        String email = request.email();

        User user = userRepository.findByEmailAndStatus(email, "active")
                .orElseThrow(() -> ApiException.badRequest("Account not found or inactive"));

        Otp otp = otpRepository.findValidForUpdate(email, request.otp(), OTP_LOGIN, LocalDateTime.now())
                .orElseThrow(() -> ApiException.badRequest("Invalid or expired OTP"));

        List<AddressSummaryDto> addresses = addressRepository.findByUserIdOrdered(user.getId()).stream()
                .map(AddressSummaryDto::from)
                .toList();

        List<UserRole> userRoles = userRoleRepository.findWithRoleByUserId(user.getId());
        List<String> roles = userRoles.stream().map(ur -> ur.getRole().getRoleName()).toList();
        if (roles.isEmpty()) {
            throw ApiException.badRequest("No role assigned to this account");
        }
        List<Long> roleIds = userRoles.stream().map(ur -> ur.getRole().getId()).toList();

        Map<Long, Map<String, Object>> permissions = new LinkedHashMap<>();
        for (RolePermission rp : rolePermissionRepository.findWithPermissionByRoleIds(roleIds)) {
            permissions.computeIfAbsent(rp.getPermission().getId(), id -> {
                Map<String, Object> p = new LinkedHashMap<>();
                p.put("id", id);
                p.put("permission_name", rp.getPermission().getPermissionName());
                p.put("module", rp.getPermission().getModule());
                return p;
            });
        }

        otp.setIsUsed(true);
        AuthTokens tokens = tokenService.generateAuthTokens(user, roles);

        Map<String, Object> userView = new LinkedHashMap<>();
        userView.put("id", user.getId());
        userView.put("first_name", user.getFirstName());
        userView.put("last_name", user.getLastName());
        userView.put("email", user.getEmail());
        userView.put("phone", user.getPhone());
        userView.put("roles", roles);

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("message", "Login successful");
        result.put("user", userView);
        result.put("addresses", addresses);
        result.put("permissions", List.copyOf(permissions.values()));
        putTokens(result, tokens);
        return result;
    }

    // ------------------------------------------------------------------ password login

    @Transactional
    public Map<String, Object> loginWithPassword(LoginWithPasswordRequest request) {
        if (isBlank(request.email()) || isBlank(request.password())) {
            throw ApiException.badRequest("Email and password are required");
        }
        User user = userRepository.findByEmailAndStatus(request.email().toLowerCase().trim(), "active")
                .filter(u -> u.getPasswordHash() != null && !u.getPasswordHash().isEmpty())
                .orElseThrow(() -> ApiException.badRequest("Invalid email or password"));

        if (!passwordEncoder.matches(request.password(), user.getPasswordHash())) {
            throw ApiException.badRequest("Invalid email or password");
        }

        List<String> roles = userRoleRepository.findWithRoleByUserId(user.getId()).stream()
                .map(ur -> ur.getRole().getRoleName())
                .toList();
        if (roles.isEmpty()) {
            throw ApiException.badRequest("No role assigned to this account");
        }

        AuthTokens tokens = tokenService.generateAuthTokens(user, roles);

        Map<String, Object> userView = new LinkedHashMap<>();
        userView.put("id", user.getId());
        userView.put("first_name", user.getFirstName());
        userView.put("last_name", user.getLastName());
        userView.put("email", user.getEmail());
        userView.put("roles", roles);

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("message", "Login successful");
        result.put("user", userView);
        putTokens(result, tokens);
        return result;
    }

    @Transactional
    public Map<String, Object> setPassword(Long userId, String password) {
        if (password == null || password.length() < 8) {
            throw ApiException.badRequest("Password must contain at least 8 characters");
        }
        User user = userRepository.findById(userId)
                .orElseThrow(() -> ApiException.badRequest("User not found"));
        user.setPasswordHash(passwordEncoder.encode(password));

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("message", "Password set successfully");
        return result;
    }

    // ------------------------------------------------------------------ addresses / logout

    @Transactional(readOnly = true)
    public Map<String, Object> getCustomerAddresses(Long userId) {
        List<AddressDto> addresses = addressRepository.findByUserIdOrdered(userId).stream()
                .map(AddressDto::from)
                .toList();
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("message", "Customer addresses fetched successfully");
        result.put("addresses", addresses);
        return result;
    }

    @Transactional
    public Map<String, Object> logout(String refreshToken) {
        if (isBlank(refreshToken)) {
            throw ApiException.unauthorized("Refresh token is required");
        }
        DecodedJWT decoded;
        try {
            decoded = jwtService.verifyRefreshToken(refreshToken);
        } catch (JWTVerificationException e) {
            throw ApiException.unauthorized("Invalid or expired refresh token");
        }
        Long userId = decoded.getClaim("user_id").asLong();
        RefreshToken stored = (userId == null ? java.util.Optional.<RefreshToken>empty()
                : refreshTokenRepository.findActiveForUpdate(userId, TokenService.hashToken(refreshToken)))
                .orElseThrow(() -> ApiException.unauthorized("Refresh token already revoked or not found"));
        stored.setRevokedAt(LocalDateTime.now());

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("message", "Logout successful");
        return result;
    }

    // ------------------------------------------------------------------ helpers

    private static String generateOtp() {
        return String.valueOf(100000 + RANDOM.nextInt(900000));
    }

    private static Otp newOtp(String email, String type, String code, LocalDateTime expiresAt) {
        Otp otp = new Otp();
        otp.setEmail(email);
        otp.setOtpType(type);
        otp.setOtpCode(code);
        otp.setChannel("email");
        otp.setExpiresAt(expiresAt);
        otp.setIsUsed(false);
        otp.setAttempts(0);
        return otp;
    }

    private static void putTokens(Map<String, Object> target, AuthTokens tokens) {
        target.put("accessToken", tokens.accessToken());
        target.put("refreshToken", tokens.refreshToken());
    }

    private static boolean isBlank(String value) {
        return value == null || value.isBlank();
    }

    /** First non-blank value among the given keys, or null. */
    private static String text(Map<String, Object> map, String... keys) {
        for (String key : keys) {
            Object value = map.get(key);
            if (value != null && !value.toString().isBlank()) {
                return value.toString().trim();
            }
        }
        return null;
    }

    private static String decimalText(Object value) {
        if (value == null || value.toString().isBlank()) {
            return null;
        }
        try {
            return new BigDecimal(value.toString().trim()).toPlainString();
        } catch (NumberFormatException e) {
            throw ApiException.badRequest("Invalid location value: " + value);
        }
    }

    private static BigDecimal decimalOrNull(JsonNode node) {
        if (node == null || node.isMissingNode() || node.isNull() || node.asText().isBlank()) {
            return null;
        }
        return new BigDecimal(node.asText());
    }
}
