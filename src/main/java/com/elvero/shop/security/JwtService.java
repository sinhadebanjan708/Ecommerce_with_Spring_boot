package com.elvero.shop.security;

import com.auth0.jwt.JWT;
import com.auth0.jwt.JWTCreator;
import com.auth0.jwt.JWTVerifier;
import com.auth0.jwt.algorithms.Algorithm;
import com.auth0.jwt.interfaces.Claim;
import com.auth0.jwt.interfaces.DecodedJWT;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * Creates and verifies the same HS256 tokens the Node app issued:
 * payload { user_id, role, email, phone, jti }, access token signed with JWT_SECRET (15 min),
 * refresh token signed with JWT_REFRESH_SECRET (7 days). Existing tokens stay valid.
 */
@Component
public class JwtService {

    private final Algorithm accessAlgorithm;
    private final Algorithm refreshAlgorithm;
    private final JWTVerifier accessVerifier;
    private final JWTVerifier refreshVerifier;
    private final Duration accessTtl;
    private final Duration refreshTtl;

    public JwtService(@Value("${app.jwt.secret}") String secret,
                      @Value("${app.jwt.refresh-secret}") String refreshSecret,
                      @Value("${app.jwt.access-ttl-minutes:15}") long accessMinutes,
                      @Value("${app.jwt.refresh-ttl-days:7}") long refreshDays) {
        this.accessAlgorithm = Algorithm.HMAC256(secret);
        this.refreshAlgorithm = Algorithm.HMAC256(refreshSecret);
        this.accessVerifier = JWT.require(accessAlgorithm).ignoreIssuedAt().build();
        this.refreshVerifier = JWT.require(refreshAlgorithm).ignoreIssuedAt().build();
        this.accessTtl = Duration.ofMinutes(accessMinutes);
        this.refreshTtl = Duration.ofDays(refreshDays);
    }

    public Duration refreshTtl() {
        return refreshTtl;
    }

    /** @param role a String (registration tokens) or a List of Strings (login tokens), like the Node app. */
    public String createAccessToken(Long userId, Object role, String email, String phone, String jti) {
        return sign(accessAlgorithm, accessTtl, userId, role, email, phone, jti);
    }

    public String createRefreshToken(Long userId, Object role, String email, String phone, String jti) {
        return sign(refreshAlgorithm, refreshTtl, userId, role, email, phone, jti);
    }

    /** @throws com.auth0.jwt.exceptions.JWTVerificationException (TokenExpiredException when expired) */
    public DecodedJWT verifyAccessToken(String token) {
        return accessVerifier.verify(token);
    }

    /** @throws com.auth0.jwt.exceptions.JWTVerificationException */
    public DecodedJWT verifyRefreshToken(String token) {
        return refreshVerifier.verify(token);
    }

    /** The raw "role" claim: a List&lt;String&gt;, a String, or null. */
    public static Object roleClaim(DecodedJWT jwt) {
        Claim claim = jwt.getClaim("role");
        if (claim.isMissing() || claim.isNull()) {
            return null;
        }
        List<String> list = claim.asList(String.class);
        return list != null ? list : claim.asString();
    }

    public static List<String> roles(DecodedJWT jwt) {
        Object role = roleClaim(jwt);
        if (role instanceof List<?> list) {
            List<String> out = new ArrayList<>();
            for (Object o : list) {
                out.add(String.valueOf(o));
            }
            return out;
        }
        return role == null ? List.of() : List.of(role.toString());
    }

    private String sign(Algorithm algorithm, Duration ttl, Long userId, Object role,
                        String email, String phone, String jti) {
        Instant now = Instant.now();
        JWTCreator.Builder builder = JWT.create()
                .withClaim("user_id", userId)
                .withJWTId(jti)
                .withIssuedAt(now)
                .withExpiresAt(now.plus(ttl));
        if (role instanceof Collection<?> collection) {
            builder.withClaim("role", new ArrayList<Object>(collection));
        } else if (role != null) {
            builder.withClaim("role", role.toString());
        }
        if (email != null) {
            builder.withClaim("email", email);
        }
        if (phone != null) {
            builder.withClaim("phone", phone);
        }
        return builder.sign(algorithm);
    }
}
