package com.hrms.security;

import java.util.List;
import java.util.UUID;

public record UserPrincipal(UUID userId, String email, List<String> roles) {
  public boolean hasRole(String role) {
    return roles != null && roles.stream().anyMatch(r -> r.equalsIgnoreCase(role));
  }
}
