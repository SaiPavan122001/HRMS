package com.hrms;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.hrms.auth.AuthService;
import com.hrms.auth.dto.AuthDtos.*;
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
import com.hrms.security.JwtTokenProvider;
import java.time.Duration;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

class AuthServiceTest {

  private UserRepository users;
  private RefreshTokenRepository refreshTokens;
  private OneTimeTokenRepository oneTimeTokens;
  private ProfileRepository profiles;
  private RbacService rbac;
  private OutboxService outbox;
  private AuthService service;

  @BeforeEach
  void setUp() {
    users = Mockito.mock(UserRepository.class);
    refreshTokens = Mockito.mock(RefreshTokenRepository.class);
    oneTimeTokens = Mockito.mock(OneTimeTokenRepository.class);
    profiles = Mockito.mock(ProfileRepository.class);
    rbac = Mockito.mock(RbacService.class);
    outbox = Mockito.mock(OutboxService.class);
    AppProperties props = new AppProperties(
        new AppProperties.Jwt("0123456789abcdef0123456789abcdef0123456789abcdef0123456789abcdef",
            Duration.ofMinutes(15), Duration.ofHours(168)),
        "http://localhost:8080", new AppProperties.Cors(List.of()), new AppProperties.Mail(true));
    JwtTokenProvider tokenProvider = new JwtTokenProvider(props);
    service = new AuthService(users, refreshTokens, oneTimeTokens, profiles,
        new BCryptPasswordEncoder(), tokenProvider, rbac, outbox, props, new ObjectMapper());
  }

  private User activeUser(String email) {
    User u = new User();
    u.setId(UUID.randomUUID());
    u.setEmail(email);
    u.setPassword(new BCryptPasswordEncoder().encode("Password123"));
    u.setEmailVerified(true);
    u.setStatus(User.Status.ACTIVE);
    return u;
  }

  @Test
  void signUpRejectsDuplicateEmail() {
    when(users.existsByEmailIgnoreCase("a@b.com")).thenReturn(true);
    assertThrows(ApiException.class, () ->
        service.signUp(new SignUpRequest("a@b.com", "Password123", "A", "B")));
  }

  @Test
  void signUpCreatesUserAndProfileAndSendsVerification() {
    when(users.existsByEmailIgnoreCase("new@b.com")).thenReturn(false);
    when(users.save(any())).thenAnswer(inv -> { User u = inv.getArgument(0); if (u.getId() == null) u.setId(java.util.UUID.randomUUID()); return u; });
    when(profiles.save(any())).thenAnswer(inv -> inv.getArgument(0));
    MessageResponse response = service.signUp(new SignUpRequest("new@b.com", "Password123", "New", "User"));
    assertTrue(response.message().contains("verify"));
    verify(rbac).assignDefaultRole(any(), any());
    verify(outbox).append(eq("EMAIL_VERIFICATION"), eq("user"), any(), any());
  }

  @Test
  void signInFailsWithWrongPassword() {
    User user = activeUser("a@b.com");
    when(users.findByEmailIgnoreCase("a@b.com")).thenReturn(Optional.of(user));
    assertThrows(org.springframework.security.authentication.BadCredentialsException.class,
        () -> service.signIn(new SignInRequest("a@b.com", "WrongPass123"), "127.0.0.1", "test"));
    assertEquals(1, user.getFailedLoginAttempts());
  }

  @Test
  void signInLocksAccountAfterFiveFailures() {
    User user = activeUser("a@b.com");
    user.setFailedLoginAttempts(4);
    when(users.findByEmailIgnoreCase("a@b.com")).thenReturn(Optional.of(user));
    assertThrows(org.springframework.security.authentication.BadCredentialsException.class,
        () -> service.signIn(new SignInRequest("a@b.com", "WrongPass123"), "127.0.0.1", "test"));
    assertNotNull(user.getLockedUntil());
  }

  @Test
  void signInRejectsUnverifiedEmail() {
    User user = activeUser("a@b.com");
    user.setEmailVerified(false);
    when(users.findByEmailIgnoreCase("a@b.com")).thenReturn(Optional.of(user));
    ApiException ex = assertThrows(ApiException.class,
        () -> service.signIn(new SignInRequest("a@b.com", "Password123"), "127.0.0.1", "test"));
    assertEquals("EMAIL_NOT_VERIFIED", ex.getCode());
  }

  @Test
  void forgotPasswordNeverThrowsForUnknownEmail() {
    when(users.findByEmailIgnoreCase("ghost@b.com")).thenReturn(Optional.empty());
    MessageResponse r = service.forgotPassword("ghost@b.com");
    assertTrue(r.message().contains("If an account exists"));
    verify(outbox, never()).append(any(), any(), any(), any());
  }

  @Test
  void refreshRotatesTokens() {
    User user = activeUser("a@b.com");
    com.hrms.auth.entity.RefreshToken stored = new com.hrms.auth.entity.RefreshToken();
    stored.setUserId(user.getId());
    stored.setTokenHash("abc");
    stored.setExpiresAt(java.time.Instant.now().plusSeconds(3600));
    when(refreshTokens.findByTokenHash(any())).thenReturn(Optional.of(stored));
    when(users.findById(user.getId())).thenReturn(Optional.of(user));
    when(profiles.findByUserId(any())).thenReturn(Optional.of(new Profile()));
    when(rbac.effectiveRole(any())).thenReturn("employee");
    when(refreshTokens.save(any())).thenAnswer(inv -> inv.getArgument(0));
    when(refreshTokens.findByUserIdAndRevokedFalse(any())).thenReturn(List.of());
    AuthResponse response = service.refresh(new TokenRefreshRequest("raw"), "127.0.0.1", "test");
    assertTrue(stored.isRevoked());
    assertNotNull(response.accessToken());
    assertNotNull(response.refreshToken());
    assertNotEquals("raw", response.refreshToken());
  }
}

