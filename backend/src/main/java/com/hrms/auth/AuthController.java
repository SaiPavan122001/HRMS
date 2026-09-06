package com.hrms.auth;

import com.hrms.auth.dto.AuthDtos.*;
import com.hrms.security.SecurityUtils;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import java.util.UUID;
import org.slf4j.MDC;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/auth")
@Tag(name = "Authentication", description = "Sign up, sign in, tokens, password flows")
public class AuthController {

  private final AuthService authService;

  public AuthController(AuthService authService) {
    this.authService = authService;
  }

  private String clientIp(HttpServletRequest request) {
    String forwarded = request.getHeader("X-Forwarded-For");
    return forwarded != null ? forwarded.split(",")[0].trim() : request.getRemoteAddr();
  }

  @PostMapping("/signup")
  @Operation(summary = "Create a new account (sends a real verification email)")
  @ResponseStatus(HttpStatus.CREATED)
  public MessageResponse signUp(@Valid @RequestBody SignUpRequest request) {
    return authService.signUp(request);
  }

  @PostMapping("/login")
  @Operation(summary = "Sign in with email and password")
  public AuthResponse login(@Valid @RequestBody SignInRequest request, HttpServletRequest httpRequest) {
    return authService.signIn(request, clientIp(httpRequest), httpRequest.getHeader("User-Agent"));
  }

  @PostMapping("/refresh")
  @Operation(summary = "Exchange a refresh token for a new token pair (rotation)")
  public AuthResponse refresh(@Valid @RequestBody TokenRefreshRequest request, HttpServletRequest httpRequest) {
    return authService.refresh(request, clientIp(httpRequest), httpRequest.getHeader("User-Agent"));
  }

  @PostMapping("/logout")
  @Operation(summary = "Revoke the refresh token (sign out)")
  public MessageResponse logout(@Valid @RequestBody TokenRefreshRequest request) {
    return authService.logout(request.refreshToken());
  }

  @PostMapping("/forgot-password")
  @Operation(summary = "Request a password reset email")
  public MessageResponse forgotPassword(@Valid @RequestBody ForgotPasswordRequest request) {
    return authService.forgotPassword(request.email());
  }

  @PostMapping("/reset-password")
  @Operation(summary = "Set a new password using a reset token")
  public MessageResponse resetPassword(@Valid @RequestBody ResetPasswordRequest request) {
    return authService.resetPassword(request);
  }

  @PostMapping("/change-password")
  @Operation(summary = "Change password while signed in")
  public MessageResponse changePassword(@Valid @RequestBody ChangePasswordRequest request) {
    return authService.changePassword(SecurityUtils.currentUserId(), request);
  }

  @PostMapping("/verify-email")
  @Operation(summary = "Verify email address with the token from the verification email")
  public MessageResponse verifyEmail(@RequestParam("token") String token) {
    return authService.verifyEmail(token);
  }

  @PostMapping("/resend-verification")
  @Operation(summary = "Resend the verification email")
  public MessageResponse resendVerification(@Valid @RequestBody ForgotPasswordRequest request) {
    authService.resendVerification(request.email());
    return new MessageResponse("If the account exists and is unverified, a new verification email has been sent.");
  }

  @GetMapping("/me")
  @Operation(summary = "Current user, profile, role and permissions")
  public MeResponse me() {
    UUID userId = SecurityUtils.currentUserId();
    return authService.me(userId);
  }
}
