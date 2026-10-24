package com.premisave.c2b_hakikisha.config;

import com.premisave.c2b_hakikisha.security.BearerAuthInterceptor;
import java.util.List;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import org.springframework.web.filter.CorsFilter;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
@EnableScheduling
@EnableConfigurationProperties(HakikishaProperties.class)
public class AppConfig implements WebMvcConfigurer {

    /** Name lookup endpoints; require a Bearer access token. */
    public static final String API_PATTERN = "/api/**";

    /** Saved-accounts listing; requires the same Bearer access token. */
    public static final String INTERNAL_PATTERN = "/internal/**";

    private final BearerAuthInterceptor bearerAuthInterceptor;

    public AppConfig(BearerAuthInterceptor bearerAuthInterceptor) {
        this.bearerAuthInterceptor = bearerAuthInterceptor;
    }

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(bearerAuthInterceptor).addPathPatterns(API_PATTERN, INTERNAL_PATTERN);
    }

    /**
     * Lets a browser page (such as the HTML test console) call this service. It is a servlet filter
     * rather than addCorsMappings so that preflight OPTIONS requests are answered before the Bearer
     * interceptor runs; otherwise the preflight for /api/** and /internal/** would be rejected with 401.
     * Server to server callers such as Safaricom are not affected.
     */
    @Bean
    CorsFilter corsFilter(HakikishaProperties properties) {
        CorsConfiguration config = new CorsConfiguration();
        config.setAllowedOrigins(properties.cors().allowedOrigins());
        config.setAllowedMethods(List.of("GET", "POST", "OPTIONS"));
        config.setAllowedHeaders(List.of("Authorization", "Content-Type"));
        config.setMaxAge(3600L);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", config);
        return new CorsFilter(source);
    }
}