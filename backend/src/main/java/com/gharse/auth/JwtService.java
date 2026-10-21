package com.gharse.auth;

import com.gharse.common.AppProperties;
import com.gharse.user.User;
import com.gharse.user.UserRole;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Date;
import java.util.Optional;
import java.util.UUID;
import javax.crypto.SecretKey;
import org.springframework.stereotype.Service;

/** Issues and validates HS256 access/refresh tokens. The secret comes from the JWT_SECRET environment variable. */
@Service
public class JwtService {

    private static final String TYPE = "typ";
    private static final String ACCESS = "access";
    private static final String REFRESH = "refresh";

    private final SecretKey key;
    private final AppProperties.Jwt props;
    private final Clock clock;

    public JwtService(AppProperties appProps, Clock clock) {
        this.props = appProps.jwt();
        this.clock = clock;
        String secret = props.secret();
        if (secret == null || secret.getBytes(StandardCharsets.UTF_8).length < 32) {
            throw new IllegalStateException("JWT_SECRET must be set and at least 32 characters long");
        }
        this.key = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
    }

    public String accessToken(User u) {
        return build(u, ACCESS, Duration.ofMinutes(props.accessTokenMinutes()));
    }

    public String refreshToken(User u) {
        return build(u, REFRESH, Duration.ofDays(props.refreshTokenDays()));
    }

    public long accessTokenSeconds() {
        return Duration.ofMinutes(props.accessTokenMinutes()).toSeconds();
    }

    public Optional<AuthUser> parseAccess(String token) {
        return parse(token, ACCESS);
    }

    public Optional<AuthUser> parseRefresh(String token) {
        return parse(token, REFRESH);
    }

    private String build(User u, String type, Duration ttl) {
        Instant now = clock.instant();
        return Jwts.builder()
                .subject(u.getId().toString())
                .claim("email", u.getEmail())
                .claim("role", u.getRole().name())
                .claim("credentialVersion", u.getCredentialVersion())
                .claim(TYPE, type)
                .issuedAt(Date.from(now))
                .expiration(Date.from(now.plus(ttl)))
                .signWith(key)
                .compact();
    }

    private Optional<AuthUser> parse(String token, String expectedType) {
        try {
            Claims c = Jwts.parser().verifyWith(key).clock(() -> Date.from(clock.instant())).build()
                    .parseSignedClaims(token).getPayload();
            if (!expectedType.equals(c.get(TYPE, String.class))) return Optional.empty();
            return Optional.of(new AuthUser(UUID.fromString(c.getSubject()), c.get("email", String.class),
                    UserRole.valueOf(c.get("role", String.class)),
                    Optional.ofNullable(c.get("credentialVersion", Integer.class)).orElse(0)));
        } catch (JwtException | IllegalArgumentException e) {
            return Optional.empty();
        }
    }
}
