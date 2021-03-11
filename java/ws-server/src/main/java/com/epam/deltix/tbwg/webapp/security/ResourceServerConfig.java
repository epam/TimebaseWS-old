package com.epam.deltix.tbwg.webapp.security;

import com.epam.deltix.tbwg.webapp.config.WebMvcConfig;
import com.epam.deltix.tbwg.webapp.security.jwt.AudienceValidator;
import com.epam.deltix.tbwg.webapp.security.jwt.JwtAuthenticationConverterImpl;
import com.epam.deltix.tbwg.webapp.settings.SecurityOauth2ProviderSettings;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.autoconfigure.security.oauth2.resource.OAuth2ResourceServerProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.builders.WebSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configuration.WebSecurityConfigurerAdapter;
import org.springframework.security.oauth2.core.DelegatingOAuth2TokenValidator;
import org.springframework.security.oauth2.core.OAuth2TokenValidator;
import org.springframework.security.oauth2.jwt.*;
import org.springframework.security.web.firewall.DefaultHttpFirewall;
import org.springframework.security.web.firewall.HttpFirewall;

import static com.epam.deltix.tbwg.webapp.config.WebMvcConfig.GRAFANA_API_PREFIX;
import static com.epam.deltix.tbwg.webapp.config.WebMvcConfig.MAIN_API_PREFIX;

@EnableWebSecurity
@ConditionalOnProperty(value = "security.oauth2.provider.providerType", havingValue = "SSO", matchIfMissing = true)
public class ResourceServerConfig extends WebSecurityConfigurerAdapter {

    private final OAuth2ResourceServerProperties.Jwt jwtConfig;
    private final SecurityOauth2ProviderSettings providerConfig;
    private final JwtAuthenticationConverterImpl jwtAuthenticationConverter;

    @Autowired
    public ResourceServerConfig(OAuth2ResourceServerProperties config,
                                SecurityOauth2ProviderSettings providerConfig,
                                JwtAuthenticationConverterImpl jwtAuthenticationConverter)
    {
        this.jwtConfig = config.getJwt();
        this.providerConfig = providerConfig;
        this.jwtAuthenticationConverter = jwtAuthenticationConverter;

        if (jwtConfig == null) {
            throw new RuntimeException("Jwt config is missing. Please specify spring.security.oauth2.resourceserver.jwt config.");
        }
    }

    @Override
    protected void configure(HttpSecurity http) throws Exception {
        http
            //.cors().and()
            .authorizeRequests()
                .antMatchers("/ws/v0/**").permitAll()
                .antMatchers("/ping").permitAll()
                .antMatchers("/api/v0/docs/**").permitAll()
                .antMatchers("/api/v0/authInfo").permitAll()
                .antMatchers(MAIN_API_PREFIX + "/**").fullyAuthenticated()
                .antMatchers(GRAFANA_API_PREFIX + "/**").fullyAuthenticated()
            .and()
                .oauth2ResourceServer()
                    .jwt().jwtAuthenticationConverter(jwtAuthenticationConverter);
        http.headers()
            .frameOptions().sameOrigin();
    }

    @Bean
    public JwtDecoder jwtDecoder() {
        NimbusJwtDecoder jwtDecoder = (NimbusJwtDecoder) JwtDecoders.fromIssuerLocation(jwtConfig.getIssuerUri());

        OAuth2TokenValidator<Jwt> validator;
        if (providerConfig.isValidateIssuer()) {
            validator = JwtValidators.createDefaultWithIssuer(jwtConfig.getIssuerUri());
        } else {
            validator = JwtValidators.createDefault();
        }

        if (providerConfig.getAudience() != null && !providerConfig.getAudience().isEmpty()) {
            OAuth2TokenValidator<Jwt> audienceValidator = new AudienceValidator(providerConfig.getAudience());
            validator = new DelegatingOAuth2TokenValidator<>(validator, audienceValidator);
        }

        jwtDecoder.setJwtValidator(validator);

        return jwtDecoder;
    }

    // Encoded slashes fix

    @Bean
    public HttpFirewall allowUrlEncodedSlashHttpFirewall() {
        DefaultHttpFirewall firewall = new DefaultHttpFirewall();
        firewall.setAllowUrlEncodedSlash(true);
        return firewall;
    }

    @Override
    public void configure(WebSecurity web) throws Exception {
        web.httpFirewall(allowUrlEncodedSlashHttpFirewall());
    }

}
