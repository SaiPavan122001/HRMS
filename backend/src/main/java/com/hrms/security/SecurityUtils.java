package com.hrms.security;

import com.hrms.common.exception.ApiException;
import java.util.UUID;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

public final class SecurityUtils {

  private SecurityUtils() {}

  public static UserPrincipal currentUser() {
    Authentication auth = SecurityContextHolder.getContext().getAuthentication();
    if (auth != null && auth.getPrincipal() instanceof UserPrincipal principal) {
      return principal;
    }
    throw ApiException.unauthorized("No authenticated user");
  }

  public static UUID currentUserId() {
    return currentUser().userId();
  }

  public static String currentUserEmail() {
    return currentUser().email();
  }
}
