package com.premisave.c2b_hakikisha.config;

import com.premisave.c2b_hakikisha.security.BearerAuthInterceptor;
import com.premisave.c2b_hakikisha.security.InternalApiKeyInterceptor;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
@EnableScheduling
@EnableConfigurationProperties(HakikishaProperties.class)
public class AppConfig implements WebMvcConfigurer {

    /** Every endpoint under /api/** requires a Bearer access token. */
    public static final String API_PATTERN = "/api/**";

    /** Every endpoint under /internal/** requires the shared X-API-Key. */
    public static final String INTERNAL_PATTERN = "/internal/**";

    private final BearerAuthInterceptor bearerAuthInterceptor;
    private final InternalApiKeyInterceptor internalApiKeyInterceptor;

    public AppConfig(BearerAuthInterceptor bearerAuthInterceptor,
                     InternalApiKeyInterceptor internalApiKeyInterceptor) {
        this.bearerAuthInterceptor = bearerAuthInterceptor;
        this.internalApiKeyInterceptor = internalApiKeyInterceptor;
    }

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(bearerAuthInterceptor).addPathPatterns(API_PATTERN);
        registry.addInterceptor(internalApiKeyInterceptor).addPathPatterns(INTERNAL_PATTERN);
    }
}