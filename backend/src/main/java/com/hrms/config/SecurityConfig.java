package com.hrms.config;

import com.hrms.auth.OAuth2LoginSuccessHandler;
import com.hrms.auth.CustomOAuth2UserService;
import com.hrms.security.JwtAuthenticationFilter;
import com.hrms.security.RateLimitFilter;
import java.util.Arrays;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

@Configuration
@EnableMethodSecurity
public class SecurityConfig {

  private final AppProperties props;
  private final JwtAuthenticationFilter jwtAuthenticationFilter;
  private final RateLimitFilter rateLimitFilter;
  private final OAuth2LoginSuccessHandler oAuth2LoginSuccessHandler;
  private final CustomOAuth2UserService customOAuth2UserService;

  public SecurityConfig(AppProperties props, JwtAuthenticationFilter jwtAuthenticationFilter,
                        RateLimitFilter rateLimitFilter, OAuth2LoginSuccessHandler oAuth2LoginSuccessHandler,
                        CustomOAuth2UserService customOAuth2UserService) {
    this.props = props;
    this.jwtAuthenticationFilter = jwtAuthenticationFilter;
    this.rateLimitFilter = rateLimitFilter;
    this.oAuth2LoginSuccessHandler = oAuth2LoginSuccessHandler;
    this.customOAuth2UserService = customOAuth2UserService;
  }

  @Bean
  public PasswordEncoder passwordEncoder() {
    return new BCryptPasswordEncoder(12);
  }

  @Bean
  public CorsConfigurationSource corsConfigurationSource() {
    CorsConfiguration config = new CorsConfiguration();
    config.setAllowedOrigins(props.cors().allowedOrigins());
    config.setAllowedMethods(Arrays.asList("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
    config.setAllowedHeaders(Arrays.asList("*"));
    config.setAllowCredentials(true);
    config.setMaxAge(3600L);
    UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
    source.registerCorsConfiguration("/**", config);
    return source;
  }

  @Bean
  public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
    http
        .csrf(csrf -> csrf.disable())
        .cors(cors -> cors.configurationSource(corsConfigurationSource()))
        .sessionManagement(sm -> sm.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
        .authorizeHttpRequests(auth -> auth
            .requestMatchers("/api/v1/auth/**", "/oauth2/**", "/login/**", "/error").permitAll()
            .requestMatchers("/actuator/health", "/actuator/health/**", "/actuator/prometheus", "/actuator/info").permitAll()
            .requestMatchers("/swagger-ui/**", "/swagger-ui.html", "/api-docs/**").permitAll()
            .requestMatchers("/ws/**").permitAll()
            .anyRequest().authenticated())
        .exceptionHandling(ex -> ex
            .authenticationEntryPoint((request, response, authException) -> {
              response.setStatus(401);
              response.setContentType("application/json");
              response.getWriter().write("{\"code\":\"UNAUTHORIZED\",\"message\":\"Authentication required\"}");
            })
            .accessDeniedHandler((request, response, accessDeniedException) -> {
              response.setStatus(403);
              response.setContentType("application/json");
              response.getWriter().write("{\"code\":\"ACCESS_DENIED\",\"message\":\"You do not have permission to perform this action\"}");
            }))
        .oauth2Login(oauth -> oauth
            .successHandler(oAuth2LoginSuccessHandler)
            .userInfoEndpoint(userInfo -> userInfo.userService(customOAuth2UserService)))
        .addFilterBefore(rateLimitFilter, UsernamePasswordAuthenticationFilter.class)
        .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);
    return http.build();
  }
}
