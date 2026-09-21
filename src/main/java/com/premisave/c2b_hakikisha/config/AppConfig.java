package com.premisave.c2b_hakikisha.config;

import com.premisave.c2b_hakikisha.security.BearerAuthInterceptor;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;
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
}