package com.hrms.security;

import java.io.IOException;
import java.time.Duration;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

/**
 * Redis-backed sliding-window rate limiter.
 * - Auth endpoints: 20 requests/minute per IP (brute-force protection)
 * - All other API endpoints: 300 requests/minute per IP
 */
@Component
public class RateLimitFilter extends OncePerRequestFilter {

  private final StringRedisTemplate redis;

  public RateLimitFilter(StringRedisTemplate redis) {
    this.redis = redis;
  }

  @Override
  protected void doFilterInternal(jakarta.servlet.http.HttpServletRequest request,
                                  jakarta.servlet.http.HttpServletResponse response,
                                  jakarta.servlet.FilterChain chain)
      throws jakarta.servlet.ServletException, IOException {

    String uri = request.getRequestURI();
    int limit;
    String bucket;
    if (uri.startsWith("/api/v1/auth/")) {
      limit = 20;
      bucket = "auth";
    } else if (uri.startsWith("/api/")) {
      limit = 300;
      bucket = "api";
    } else {
      chain.doFilter(request, response);
      return;
    }

    String clientIp = clientIp(request);
    String key = "hrms:ratelimit:" + bucket + ":" + clientIp;
    Long count = redis.opsForValue().increment(key);
    if (count != null && count == 1L) {
      redis.expire(key, Duration.ofMinutes(1));
    }
    response.setHeader("X-RateLimit-Limit", String.valueOf(limit));
    if (count != null && count > limit) {
      response.setStatus(429);
      response.setContentType(MediaType.APPLICATION_JSON_VALUE);
      response.getWriter().write(
          "{\"code\":\"RATE_LIMIT_EXCEEDED\",\"message\":\"Too many requests. Please slow down.\"}");
      return;
    }
    chain.doFilter(request, response);
  }

  private String clientIp(jakarta.servlet.http.HttpServletRequest request) {
    String forwarded = request.getHeader("X-Forwarded-For");
    if (forwarded != null && !forwarded.isBlank()) {
      return forwarded.split(",")[0].trim();
    }
    return request.getRemoteAddr();
  }

  @Override
  protected boolean shouldNotFilter(jakarta.servlet.http.HttpServletRequest request) {
    return request.getRequestURI().startsWith("/ws/") || request.getRequestURI().startsWith("/actuator");
  }
}
