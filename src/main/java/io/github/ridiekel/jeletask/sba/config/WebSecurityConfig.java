package io.github.ridiekel.jeletask.sba.config;

import de.codecentric.boot.admin.server.config.AdminServerProperties;
import de.codecentric.boot.admin.server.domain.entities.Instance;
import de.codecentric.boot.admin.server.domain.values.InstanceId;
import org.awaitility.Awaitility;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.security.web.csrf.CookieCsrfTokenRepository;
import org.springframework.security.web.servlet.util.matcher.PathPatternRequestMatcher;

import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

@Configuration
@EnableWebSecurity
public class WebSecurityConfig {

    private final AdminServerProperties adminServer;

    public WebSecurityConfig(AdminServerProperties adminServer) {
        this.adminServer = adminServer;
    }

    @Bean
    SecurityFilterChain security(HttpSecurity http, de.codecentric.boot.admin.server.services.InstanceRegistry registry) throws Exception {
        var successHandler = (AuthenticationSuccessHandler) (request, response, authentication) -> {
            // Vind de (enige) 'UP' instance en redirect erheen; anders fallback naar de dashboard.
            String target = "/"; // of bijv. "/applications"
            try {
                AtomicReference<InstanceId> instanceId = new AtomicReference<>();
                Awaitility.await("teletask2mqtt is up").atMost(1, TimeUnit.MINUTES).until(() -> {
                    return Optional.ofNullable(registry.getInstances().collectList().block(java.time.Duration.ofSeconds(2))).map(instances -> {
                        var id = instances.stream()
                                .filter(i -> "UP".equalsIgnoreCase(i.getStatusInfo().getStatus()))
                                .findFirst()
                                .map(Instance::getId)
                                .orElse(null);
                        instanceId.set(id);
                        return id != null;
                    }).orElse(false);
                });
                target = "/instances/" + instanceId.get().getValue() + "/traces";
            } catch (Exception ignored) {
            }
            response.sendRedirect(request.getContextPath() + target);
        };

        http.authorizeHttpRequests(req -> req
                        .requestMatchers(this.adminServer.getContextPath() + "/assets/**", "/actuator/health").permitAll()
                        .requestMatchers(this.adminServer.getContextPath() + "/login").permitAll()
                        .anyRequest().authenticated())
                .formLogin(formLogin -> formLogin.loginPage(this.adminServer.getContextPath() + "/login")
                        .successHandler(successHandler))
                .logout((logout) -> logout.logoutUrl(this.adminServer.getContextPath() + "/logout"))
                .httpBasic(Customizer.withDefaults())
                .csrf(csrf -> csrf.csrfTokenRepository(CookieCsrfTokenRepository.withHttpOnlyFalse())
                        .ignoringRequestMatchers(
                                PathPatternRequestMatcher.withDefaults().matcher(this.adminServer.getContextPath() + "/ws/*"),
                                PathPatternRequestMatcher.withDefaults().matcher(this.adminServer.getContextPath() + "/instances"),
                                PathPatternRequestMatcher.withDefaults().matcher(this.adminServer.getContextPath() + "/instances/*"),
                                PathPatternRequestMatcher.withDefaults().matcher(this.adminServer.getContextPath() + "/actuator/**")))
                .rememberMe(rememberMe -> rememberMe.key(UUID.randomUUID().toString())
                        .tokenValiditySeconds(1209600));
        return http.build();
    }
}
