package io.github.ridiekel.jeletask;

import de.codecentric.boot.admin.server.config.AdminServerProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.SavedRequestAwareAuthenticationSuccessHandler;
import org.springframework.security.web.csrf.CookieCsrfTokenRepository;
import org.springframework.security.web.servlet.util.matcher.PathPatternRequestMatcher;

import java.util.UUID;

@Configuration
@EnableWebSecurity
public class WebSecurityConfig {

    private final AdminServerProperties adminServer;

    public WebSecurityConfig(AdminServerProperties adminServer) {
        this.adminServer = adminServer;
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        SavedRequestAwareAuthenticationSuccessHandler successHandler = new SavedRequestAwareAuthenticationSuccessHandler();
        successHandler.setTargetUrlParameter("redirectTo");
        successHandler.setDefaultTargetUrl(this.adminServer.getContextPath() + "/");

        http.authorizeHttpRequests(req -> req
                        .requestMatchers(this.adminServer.getContextPath() + "/assets/**", "/actuator/**").permitAll()
                        .requestMatchers(this.adminServer.getContextPath() + "/login").permitAll()
                        .anyRequest().authenticated())
                .formLogin(formLogin -> formLogin.loginPage(this.adminServer.getContextPath() + "/login")
                        .successHandler(successHandler))
                .logout((logout) -> logout.logoutUrl(this.adminServer.getContextPath() + "/logout"))
                .httpBasic(Customizer.withDefaults())
                .csrf(csrf -> csrf.csrfTokenRepository(CookieCsrfTokenRepository.withHttpOnlyFalse())
                        .ignoringRequestMatchers(
                                PathPatternRequestMatcher.withDefaults().matcher(this.adminServer.getContextPath() + "/instances"),
                                PathPatternRequestMatcher.withDefaults().matcher(this.adminServer.getContextPath() + "/instances/*"),
                                PathPatternRequestMatcher.withDefaults().matcher(this.adminServer.getContextPath() + "/actuator/**")))
                .rememberMe(rememberMe -> rememberMe.key(UUID.randomUUID().toString())
                        .tokenValiditySeconds(1209600));
        return http.build();
    }
}
