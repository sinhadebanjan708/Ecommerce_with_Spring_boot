package com.elvero.shop.service;

import com.elvero.shop.entity.RefreshToken;
import com.elvero.shop.entity.User;
import com.elvero.shop.repository.RefreshTokenRepository;
import com.elvero.shop.security.JwtService;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.LocalDateTime;
import java.util.HexFormat;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Port of token.service.js: issues an access + refresh pair and stores the refresh token's SHA-256 hash. */
@Service
public class TokenService {

    private final JwtService jwtService;
    private final RefreshTokenRepository refreshTokenRepository;

    public TokenService(JwtService jwtService, RefreshTokenRepository refreshTokenRepository) {
        this.jwtService = jwtService;
        this.refreshTokenRepository = refreshTokenRepository;
    }

    public static String hashToken(String token) {
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256").digest(token.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(digest);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException(e);
        }
    }

    /** @param role a String ("CUSTOMER", registration) or a List of role names (login), as in Node. */
    @Transactional
    public AuthTokens generateAuthTokens(User user, Object role) {
        String jti = UUID.randomUUID().toString();
        String accessToken = jwtService.createAccessToken(user.getId(), role, user.getEmail(), user.getPhone(), jti);
        String refreshToken = jwtService.createRefreshToken(user.getId(), role, user.getEmail(), user.getPhone(), jti);

        RefreshToken stored = new RefreshToken();
        stored.setUser(user);
        stored.setTokenHash(hashToken(refreshToken));
        stored.setExpiresAt(LocalDateTime.now().plus(jwtService.refreshTtl()));
        refreshTokenRepository.save(stored);

        return new AuthTokens(accessToken, refreshToken);
    }
}
