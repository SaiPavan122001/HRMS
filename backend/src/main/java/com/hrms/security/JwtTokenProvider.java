package com.hrms.security;

import com.hrms.config.AppProperties;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import java.time.Instant;
import java.util.Date;
import java.util.List;
import javax.crypto.SecretKey;
import org.springframework.stereotype.Component;

@Component
public class JwtTokenProvider {

  private final SecretKey key;
  private final AppProperties props;

  public JwtTokenProvider(AppProperties props) {
    this.props = props;
    this.key = Keys.hmacShaKeyFor(props.jwt().secret().getBytes());
  }

  public String createAccessToken(java.util.UUID userId, String email, List<String> roles) {
    Instant now = Instant.now();
    return Jwts.builder()
        .subject(userId.toString())
        .claim("email", email)
        .claim("roles", roles)
        .claim("type", "access")
        .issuedAt(Date.from(now))
        .expiration(Date.from(now.plus(props.jwt().accessExpiration())))
        .signWith(key)
        .compact();
  }

  public Claims parseToken(String token) {
    return Jwts.parser().verifyWith(key).build().parseSignedClaims(token).getPayload();
  }

  public boolean isValid(String token) {
    try {
      parseToken(token);
      return true;
    } catch (JwtException | IllegalArgumentException e) {
      return false;
    }
  }

  public java.util.UUID getUserId(String token) {
    return java.util.UUID.fromString(parseToken(token).getSubject());
  }

  @SuppressWarnings("unchecked")
  public List<String> getRoles(String token) {
    Object roles = parseToken(token).get("roles");
    return roles instanceof List ? (List<String>) roles : List.of();
  }
}
