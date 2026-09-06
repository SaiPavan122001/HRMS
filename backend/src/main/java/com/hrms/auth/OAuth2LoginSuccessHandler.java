package com.hrms.auth;

import com.hrms.auth.entity.User;
import com.hrms.auth.repo.UserRepository;
import com.hrms.config.AppProperties;
import com.hrms.employee.Profile;
import com.hrms.employee.ProfileRepository;
import com.hrms.role.RbacService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.List;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.security.web.authentication.SimpleUrlAuthenticationSuccessHandler;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.util.UriComponentsBuilder;

/**
 * OAuth2 (Google/GitHub) success handler:
 * find or provision the user, assign the default EMPLOYEE role, issue app JWT tokens
 * and redirect back to the React frontend with the tokens in the URL fragment
 * (fragments are not sent to servers, so they never appear in access logs).
 */
@Component
public class OAuth2LoginSuccessHandler extends SimpleUrlAuthenticationSuccessHandler {

  private static final Logger log = LoggerFactory.getLogger(OAuth2LoginSuccessHandler.class);

  private final UserRepository userRepository;
  private final ProfileRepository profileRepository;
  private final RbacService rbacService;
  private final com.hrms.security.JwtTokenProvider tokenProvider;
  private final AppProperties props;

  public OAuth2LoginSuccessHandler(UserRepository userRepository, ProfileRepository profileRepository,
                                   RbacService rbacService, com.hrms.security.JwtTokenProvider tokenProvider,
                                   AppProperties props) {
    this.userRepository = userRepository;
    this.profileRepository = profileRepository;
    this.rbacService = rbacService;
    this.tokenProvider = tokenProvider;
    this.props = props;
  }

  @Override
  @Transactional
  public void onAuthenticationSuccess(HttpServletRequest request, HttpServletResponse response,
                                      Authentication authentication) throws IOException {
    OAuth2AuthenticationToken oauth = (OAuth2AuthenticationToken) authentication;
    OAuth2User principal = oauth.getPrincipal();
    String provider = oauth.getAuthorizedClientRegistrationId().toUpperCase();
    String email = principal.getAttribute("hrms_email");
    String name = principal.getAttribute("hrms_name");
    String providerId = String.valueOf(principal.getAttributes().get("sub") != null
        ? principal.getAttributes().get("sub") : principal.getAttributes().get("id"));

    User user = userRepository.findByEmailIgnoreCase(email).orElse(null);
    if (user == null) {
      String[] parts = splitName(name);
      user = new User();
      user.setEmail(email.toLowerCase());
      user.setFirstName(parts[0]);
      user.setLastName(parts[1]);
      user.setEmailVerified(true); // OAuth providers verify email ownership
      user.setProvider(User.Provider.valueOf(provider));
      user.setProviderId(providerId);
      user = userRepository.save(user);
      Profile profile = new Profile();
      profile.setUserId(user.getId());
      profile.setEmail(user.getEmail());
      profile.setFirstName(parts[0]);
      profile.setLastName(parts[1]);
      profile.setEmployeeId("EMP-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase());
      profile = profileRepository.save(profile);
      rbacService.assignDefaultRole(user.getId(), profile.getId());
      log.info("Provisioned new {} user: {}", provider, email);
    } else {
      // Link the provider to an existing local account if not linked yet
      if (user.getProvider() == User.Provider.LOCAL) {
        user.setProvider(User.Provider.valueOf(provider));
        user.setProviderId(providerId);
        user.setEmailVerified(true);
        userRepository.save(user);
      }
    }

    if (user.getStatus() != User.Status.ACTIVE) {
      getRedirectStrategy().sendRedirect(request, response,
          props.frontendUrl() + "/login?error=account_inactive");
      return;
    }

    String role = rbacService.effectiveRole(user.getId());
    String accessToken = tokenProvider.createAccessToken(user.getId(), user.getEmail(), List.of(role));

    // Fragment-based redirect: tokens are not exposed to server-side logs
    String target = UriComponentsBuilder.fromUriString(props.frontendUrl())
        .path("/oauth/callback")
        .fragment("access_token=" + accessToken)
        .build().toUriString();
    clearAuthenticationAttributes(request);
    getRedirectStrategy().sendRedirect(request, response, target);
  }

  private String[] splitName(String name) {
    if (name == null || name.isBlank()) return new String[]{"User", ""};
    String trimmed = name.trim();
    int idx = trimmed.indexOf(' ');
    if (idx < 0) return new String[]{trimmed, ""};
    return new String[]{trimmed.substring(0, idx), trimmed.substring(idx + 1)};
  }
}
