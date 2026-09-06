package com.hrms.auth;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.hrms.auth.dto.AuthDtos.*;
import com.hrms.auth.entity.OneTimeToken;
import com.hrms.auth.entity.RefreshToken;
import com.hrms.auth.entity.User;
import com.hrms.auth.repo.OneTimeTokenRepository;
import com.hrms.auth.repo.RefreshTokenRepository;
import com.hrms.auth.repo.UserRepository;
import com.hrms.common.exception.ApiException;
import com.hrms.config.AppProperties;
import com.hrms.employee.Profile;
import com.hrms.employee.ProfileRepository;
import com.hrms.events.OutboxService;
import com.hrms.role.RbacService;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Base64;
import java.util.HexFormat;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AuthService {

  private static final Logger log = LoggerFactory.getLogger(AuthService.class);
  private static final int MAX_FAILED_ATTEMPTS = 5;
  private static final int LOCK_MINUTES = 15;
  private static final SecureRandom RANDOM = new SecureRandom();

  private final UserRepository userRepository;
  private final RefreshTokenRepository refreshTokenRepository;
  private final OneTimeTokenRepository oneTimeTokenRepository;
  private final ProfileRepository profileRepository;
  private final PasswordEncoder passwordEncoder;
  private final com.hrms.security.JwtTokenProvider tokenProvider;
  private final RbacService rbacService;
  private final OutboxService outbox;
  private final AppProperties props;
  private final ObjectMapper mapper;

  public AuthService(UserRepository userRepository, RefreshTokenRepository refreshTokenRepository,
                     OneTimeTokenRepository oneTimeTokenRepository, ProfileRepository profileRepository,
                     PasswordEncoder passwordEncoder, com.hrms.security.JwtTokenProvider tokenProvider,
                     RbacService rbacService, OutboxService outbox, AppProperties props, ObjectMapper mapper) {
    this.userRepository = userRepository;
    this.refreshTokenRepository = refreshTokenRepository;
    this.oneTimeTokenRepository = oneTimeTokenRepository;
    this.profileRepository = profileRepository;
    this.passwordEncoder = passwordEncoder;
    this.tokenProvider = tokenProvider;
    this.rbacService = rbacService;
    this.outbox = outbox;
    this.props = props;
    this.mapper = mapper;
  }

  // ---------- Sign up ----------
  @Transactional
  public MessageResponse signUp(SignUpRequest request) {
    if (userRepository.existsByEmailIgnoreCase(request.email())) {
      throw ApiException.duplicate("An account with this email already exists");
    }
    User user = new User();
    user.setEmail(request.email().toLowerCase());
    user.setPassword(passwordEncoder.encode(request.password()));
    user.setFirstName(request.firstName());
    user.setLastName(request.lastName());
    user.setProvider(User.Provider.LOCAL);
    user = userRepository.save(user);

    Profile profile = createProfile(user, request.firstName(), request.lastName());
    rbacService.assignDefaultRole(user.getId(), profile.getId());

    issueEmailVerification(user);
    log.info("New user signed up: {}", user.getEmail());
    return new MessageResponse("Account created. Please check your email to verify your account before signing in.");
  }

  private Profile createProfile(User user, String firstName, String lastName) {
    Profile profile = new Profile();
    profile.setUserId(user.getId());
    profile.setEmail(user.getEmail());
    profile.setFirstName(firstName);
    profile.setLastName(lastName);
    profile.setEmployeeId("EMP-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase());
    return profileRepository.save(profile);
  }

  // ---------- Email verification ----------
  private void issueEmailVerification(User user) {
    String raw = randomToken();
    OneTimeToken token = new OneTimeToken();
    token.setUserId(user.getId());
    token.setTokenHash(sha256(raw));
    token.setPurpose(OneTimeToken.Purpose.EMAIL_VERIFICATION);
    token.setExpiresAt(Instant.now().plus(24, ChronoUnit.HOURS));
    oneTimeTokenRepository.save(token);
    String link = props.frontendUrl() + "/verify-email?token=" + raw;
    // Transactional outbox: the email consumer sends the real email via SMTP.
    outbox.append("EMAIL_VERIFICATION", "user", user.getId(),
        Map.of("email", user.getEmail(), "link", link, "userId", user.getId().toString()));
  }

  @Transactional
  public void resendVerification(String email) {
    userRepository.findByEmailIgnoreCase(email).ifPresent(user -> {
      if (!user.isEmailVerified()) issueEmailVerification(user);
    });
  }

  @Transactional
  public MessageResponse verifyEmail(String rawToken) {
    OneTimeToken token = oneTimeTokenRepository
        .findByTokenHashAndPurpose(sha256(rawToken), OneTimeToken.Purpose.EMAIL_VERIFICATION)
        .orElseThrow(() -> ApiException.badRequest("Invalid or expired verification link"));
    if (!token.isUsable()) throw ApiException.badRequest("Invalid or expired verification link");
    token.setUsedAt(Instant.now());
    oneTimeTokenRepository.save(token);
    User user = userRepository.findById(token.getUserId())
        .orElseThrow(() -> ApiException.notFound("User no longer exists"));
    user.setEmailVerified(true);
    userRepository.save(user);
    return new MessageResponse("Email verified. You can now sign in.");
  }

  // ---------- Sign in ----------
  @Transactional
  public AuthResponse signIn(SignInRequest request, String ip, String userAgent) {
    User user = userRepository.findByEmailIgnoreCase(request.email())
        .orElseThrow(() -> new BadCredentialsException("Invalid credentials"));
    if (user.isLocked()) {
      throw ApiException.businessRule("Account temporarily locked due to failed sign-in attempts. Try again later.");
    }
    if (user.getPassword() == null || !passwordEncoder.matches(request.password(), user.getPassword())) {
      registerFailedAttempt(user);
      throw new BadCredentialsException("Invalid credentials");
    }
    if (!user.isEmailVerified()) {
      throw new com.hrms.common.exception.ApiException(
          org.springframework.http.HttpStatus.FORBIDDEN, "EMAIL_NOT_VERIFIED",
          "Please verify your email address before signing in. Check your inbox for the verification link.");
    }
    if (user.getStatus() != User.Status.ACTIVE) {
      throw ApiException.forbidden("Your account is not active. Contact your administrator.");
    }
    user.setFailedLoginAttempts(0);
    user.setLockedUntil(null);
    user.setLastLoginAt(Instant.now());
    userRepository.save(user);
    return issueTokens(user, ip, userAgent);
  }

  private void registerFailedAttempt(User user) {
    int attempts = user.getFailedLoginAttempts() + 1;
    user.setFailedLoginAttempts(attempts);
    if (attempts >= MAX_FAILED_ATTEMPTS) {
      user.setLockedUntil(Instant.now().plus(LOCK_MINUTES, ChronoUnit.MINUTES));
      log.warn("Account locked for {} after {} failed attempts", user.getEmail(), attempts);
    }
    userRepository.save(user);
  }

  private AuthResponse issueTokens(User user, String ip, String userAgent) {
    String role = rbacService.effectiveRole(user.getId());
    String access = tokenProvider.createAccessToken(user.getId(), user.getEmail(), List.of(role));
    String refresh = createRefreshToken(user.getId(), ip, userAgent);
    Profile profile = profileRepository.findByUserId(user.getId()).orElse(null);
    var permissions = rbacService.permissionsOf(user.getId()).stream()
        .map(p -> Map.<String, Object>of("module", p.getModule(), "action", p.getAction())).toList();
    return new AuthResponse(access, refresh, "Bearer",
        props.jwt().accessExpiration().toSeconds(), role, toUserDto(user), toProfileDto(profile), permissions);
  }

  private String createRefreshToken(UUID userId, String ip, String userAgent) {
    String raw = randomToken();
    RefreshToken token = new RefreshToken();
    token.setUserId(userId);
    token.setTokenHash(sha256(raw));
    token.setExpiresAt(Instant.now().plus(props.jwt().refreshExpiration()));
    token.setIpAddress(ip);
    token.setUserAgent(userAgent != null && userAgent.length() > 255 ? userAgent.substring(0, 255) : userAgent);
    refreshTokenRepository.save(token);
    return raw;
  }

  // ---------- Refresh (with rotation) ----------
  @Transactional
  public AuthResponse refresh(TokenRefreshRequest request, String ip, String userAgent) {
    RefreshToken stored = refreshTokenRepository.findByTokenHash(sha256(request.refreshToken()))
        .orElseThrow(() -> ApiException.unauthorized("Invalid refresh token"));
    if (stored.isRevoked() || stored.isExpired()) {
      throw ApiException.unauthorized("Refresh token expired or revoked");
    }
    // Rotate: revoke old token, issue a new pair
    stored.setRevoked(true);
    stored.setRevokedAt(Instant.now());
    refreshTokenRepository.save(stored);
    User user = userRepository.findById(stored.getUserId())
        .orElseThrow(() -> ApiException.unauthorized("User no longer exists"));
    return issueTokens(user, ip, userAgent);
  }

  @Transactional
  public MessageResponse logout(String rawRefreshToken) {
    refreshTokenRepository.findByTokenHash(sha256(rawRefreshToken)).ifPresent(t -> {
      t.setRevoked(true);
      t.setRevokedAt(Instant.now());
      refreshTokenRepository.save(t);
    });
    return new MessageResponse("Signed out");
  }

  // ---------- Password flows ----------
  @Transactional
  public MessageResponse forgotPassword(String email) {
    // Deliberately do not reveal whether the account exists (anti-enumeration)
    userRepository.findByEmailIgnoreCase(email).ifPresent(user -> {
      String raw = randomToken();
      OneTimeToken token = new OneTimeToken();
      token.setUserId(user.getId());
      token.setTokenHash(sha256(raw));
      token.setPurpose(OneTimeToken.Purpose.PASSWORD_RESET);
      token.setExpiresAt(Instant.now().plus(1, ChronoUnit.HOURS));
      oneTimeTokenRepository.save(token);
      String link = props.frontendUrl() + "/reset-password?token=" + raw;
      outbox.append("PASSWORD_RESET_REQUESTED", "user", user.getId(),
          Map.of("email", user.getEmail(), "link", link, "userId", user.getId().toString()));
    });
    return new MessageResponse("If an account exists for that email, a reset link has been sent.");
  }

  @Transactional
  public MessageResponse resetPassword(ResetPasswordRequest request) {
    OneTimeToken token = oneTimeTokenRepository
        .findByTokenHashAndPurpose(sha256(request.token()), OneTimeToken.Purpose.PASSWORD_RESET)
        .orElseThrow(() -> ApiException.badRequest("Invalid or expired reset link"));
    if (!token.isUsable()) throw ApiException.badRequest("Invalid or expired reset link");
    token.setUsedAt(Instant.now());
    oneTimeTokenRepository.save(token);
    User user = userRepository.findById(token.getUserId())
        .orElseThrow(() -> ApiException.notFound("User no longer exists"));
    user.setPassword(passwordEncoder.encode(request.newPassword()));
    user.setFailedLoginAttempts(0);
    user.setLockedUntil(null);
    userRepository.save(user);
    revokeAllSessions(user.getId());
    return new MessageResponse("Password updated. Please sign in with your new password.");
  }

  @Transactional
  public MessageResponse changePassword(UUID userId, ChangePasswordRequest request) {
    User user = userRepository.findById(userId)
        .orElseThrow(() -> ApiException.notFound("User not found"));
    if (user.getPassword() != null && !passwordEncoder.matches(request.currentPassword(), user.getPassword())) {
      throw ApiException.badRequest("Current password is incorrect");
    }
    user.setPassword(passwordEncoder.encode(request.newPassword()));
    userRepository.save(user);
    return new MessageResponse("Password changed");
  }

  @Transactional
  public void revokeAllSessions(UUID userId) {
    refreshTokenRepository.findByUserIdAndRevokedFalse(userId).forEach(t -> {
      t.setRevoked(true);
      t.setRevokedAt(Instant.now());
      refreshTokenRepository.save(t);
    });
  }

  // ---------- Current user ----------
  @Transactional(readOnly = true)
  public MeResponse me(UUID userId) {
    User user = userRepository.findById(userId)
        .orElseThrow(() -> ApiException.notFound("User not found"));
    Profile profile = profileRepository.findByUserId(userId).orElse(null);
    String role = rbacService.effectiveRole(userId);
    var permissions = rbacService.permissionsOf(userId).stream()
        .map(p -> Map.<String, Object>of("module", p.getModule(), "action", p.getAction(),
            "description", p.getDescription() == null ? "" : p.getDescription())).toList();
    return new MeResponse(toUserDto(user), toProfileDto(profile), role, permissions);
  }

  public UserDto toUserDto(User user) {
    String role = rbacService.effectiveRole(user.getId());
    return new UserDto(user.getId(), user.getEmail(), user.getFirstName(), user.getLastName(),
        user.isEmailVerified(), List.of(role));
  }

  public ProfileDto toProfileDto(Profile profile) {
    if (profile == null) return null;
    return new ProfileDto(profile.getId(), profile.getUserId(), profile.getEmail(),
        profile.getFirstName(), profile.getLastName(), profile.getEmployeeId(), profile.getDepartment(),
        profile.getJobTitle(), profile.getPhone(),
        profile.getManager() != null ? profile.getManager().getId() : null,
        profile.getHireDate() == null ? null : profile.getHireDate().toString(), profile.isActive());
  }

  private String randomToken() {
    byte[] bytes = new byte[32];
    RANDOM.nextBytes(bytes);
    return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
  }

  private String sha256(String raw) {
    try {
      return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(raw.getBytes(StandardCharsets.UTF_8)));
    } catch (Exception e) {
      throw new IllegalStateException(e);
    }
  }
}
