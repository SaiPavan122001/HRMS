package com.hrms.auth;

import java.util.Map;
import org.springframework.security.oauth2.client.userinfo.DefaultOAuth2UserService;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserRequest;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.user.DefaultOAuth2User;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Service;

/** Normalizes Google and GitHub user attributes into a common shape (email, name, picture). */
@Service
public class CustomOAuth2UserService extends DefaultOAuth2UserService {

  @Override
  public OAuth2User loadUser(OAuth2UserRequest request) throws OAuth2AuthenticationException {
    OAuth2User user = super.loadUser(request);
    String registrationId = request.getClientRegistration().getRegistrationId();
    Map<String, Object> attrs = user.getAttributes();

    String email;
    String name;
    if ("google".equals(registrationId)) {
      email = (String) attrs.get("email");
      name = (String) attrs.getOrDefault("name", "");
    } else {
      email = (String) attrs.get("email");
      String login = (String) attrs.getOrDefault("login", "user");
      if (email == null || email.isBlank()) {
        email = login + "@users.noreply.github.com";
      }
      name = (String) attrs.getOrDefault("name", login);
    }
    Map<String, Object> normalized = new java.util.HashMap<>(attrs);
    normalized.put("hrms_email", email);
    normalized.put("hrms_name", name == null || name.isBlank() ? email : name);
    normalized.put("hrms_provider", registrationId.toUpperCase());
    return new DefaultOAuth2User(user.getAuthorities(), normalized, "hrms_email");
  }
}
