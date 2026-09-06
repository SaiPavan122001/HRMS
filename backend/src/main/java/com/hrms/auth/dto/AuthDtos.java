package com.hrms.auth.dto;

import jakarta.validation.constraints.*;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public final class AuthDtos {

  private AuthDtos() {}

  public record SignUpRequest(
      @NotBlank @Email String email,
      @NotBlank @Size(min = 8, max = 72, message = "Password must be between 8 and 72 characters") String password,
      @NotBlank @Size(max = 64) String firstName,
      @NotBlank @Size(max = 64) String lastName) {}

  public record SignInRequest(@NotBlank @Email String email, @NotBlank String password) {}

  public record UserDto(UUID id, String email, String firstName, String lastName,
                        boolean emailVerified, List<String> roles) {}

  public record ProfileDto(UUID id, UUID userId, String email, String firstName, String lastName,
                           String employeeId, String department, String jobTitle, String phone,
                           UUID managerId, String hireDate, boolean active) {}

  public record AuthResponse(String accessToken, String refreshToken, String tokenType,
                             long expiresIn, String role, UserDto user, ProfileDto profile,
                             List<Map<String, Object>> permissions) {}

  public record TokenRefreshRequest(@NotBlank String refreshToken) {}

  public record ForgotPasswordRequest(@NotBlank @Email String email) {}

  public record ResetPasswordRequest(@NotBlank String token,
      @NotBlank @Size(min = 8, max = 72) String newPassword) {}

  public record ChangePasswordRequest(@NotBlank String currentPassword,
      @NotBlank @Size(min = 8, max = 72) String newPassword) {}

  public record MessageResponse(String message) {}

  public record MeResponse(UserDto user, ProfileDto profile, String role,
                           List<Map<String, Object>> permissions) {}
}
