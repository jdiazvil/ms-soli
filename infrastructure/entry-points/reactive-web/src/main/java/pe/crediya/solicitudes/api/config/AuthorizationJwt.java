package pe.crediya.solicitudes.api.config;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.convert.converter.Converter;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.config.annotation.method.configuration.EnableReactiveMethodSecurity;
import org.springframework.security.config.annotation.web.reactive.EnableWebFluxSecurity;
import org.springframework.security.config.web.server.ServerHttpSecurity;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.core.DelegatingOAuth2TokenValidator;
import org.springframework.security.oauth2.core.OAuth2TokenValidator;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtClaimValidator;
import org.springframework.security.oauth2.jwt.JwtValidators;
import org.springframework.security.oauth2.jwt.NimbusReactiveJwtDecoder;
import org.springframework.security.oauth2.jwt.ReactiveJwtDecoder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.oauth2.server.resource.authentication.ReactiveJwtAuthenticationConverterAdapter;
import org.springframework.security.web.server.SecurityWebFilterChain;
import org.springframework.util.StringUtils;
import org.springframework.web.reactive.config.WebFluxConfigurer;
import reactor.core.publisher.Mono;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;
import lombok.extern.log4j.Log4j2;

import javax.crypto.spec.SecretKeySpec;

@Log4j2
@Configuration
@EnableWebFluxSecurity
@EnableReactiveMethodSecurity
public class AuthorizationJwt implements WebFluxConfigurer {

    private final String issuerUri;
    private final String clientId;
    private final String jsonExpRoles;

    private final String localSecret;

    private final ObjectMapper mapper;
    private static final String ROLE = "ROLE_";
    private static final String AZP = "azp";

    public AuthorizationJwt(@Value("${spring.security.oauth2.resourceserver.jwt.issuer-uri}") String issuerUri,
                         @Value("${spring.security.oauth2.resourceserver.jwt.client-id}") String clientId,
                         @Value("${security.jwt.secret:}") String localSecret,
                         @Value("${jwt.json-exp-roles}") String jsonExpRoles,
                         ObjectMapper mapper) {
        this.issuerUri = issuerUri;
        this.clientId = clientId;
        this.localSecret = localSecret;
        this.jsonExpRoles = jsonExpRoles;
        this.mapper = mapper;
    }

    @Bean
    public SecurityWebFilterChain filterChain(ServerHttpSecurity http) {
        http
            .csrf(ServerHttpSecurity.CsrfSpec::disable)
            .authorizeExchange(ex -> ex
                    .pathMatchers(HttpMethod.OPTIONS,"/**").permitAll()
                    .pathMatchers("/actuator/health/**","/v3/api-docs/**",
                            "/swagger-ui.html","/swagger-ui/**").permitAll()
                    .pathMatchers(HttpMethod.POST,"/api/v1/solicitudes").permitAll()
                    .pathMatchers(HttpMethod.GET,"/api/v1/solicitudes").hasRole("ASESOR")
                    .anyExchange().authenticated())
            .oauth2ResourceServer(oauth2 ->
                    oauth2.jwt(jwtSpec ->
                            jwtSpec
                            .jwtDecoder(jwtDecoder())
                            .jwtAuthenticationConverter(grantedAuthoritiesExtractor())
                    )
            );
        return http.build();
    }

    /*
    public ReactiveJwtDecoder jwtDecoder() {
        var defaultValidator = JwtValidators.createDefaultWithIssuer(issuerUri);
        var audienceValidator = new JwtClaimValidator<String>(AZP,
                azp -> azp != null && !azp.isEmpty() && azp.equals(clientId));
        var tokenValidator = new DelegatingOAuth2TokenValidator<>(defaultValidator, audienceValidator);
        var jwtDecoder = NimbusReactiveJwtDecoder
                .withIssuerLocation(issuerUri)
                .build();

        jwtDecoder.setJwtValidator(tokenValidator);
        return jwtDecoder;
    }*/

    public ReactiveJwtDecoder jwtDecoder() {
        // Si hay secreto local → HS256
        if (StringUtils.hasText(localSecret)) {
            var key = new SecretKeySpec(localSecret.getBytes(StandardCharsets.UTF_8), "HmacSHA256");
            return NimbusReactiveJwtDecoder.withSecretKey(key)
                    .macAlgorithm(MacAlgorithm.HS256)
                    .build();
        }

        // Si no hay secreto, usa issuer-uri (IdP externo)
        if (StringUtils.hasText(issuerUri)) {
            NimbusReactiveJwtDecoder decoder = NimbusReactiveJwtDecoder.withIssuerLocation(issuerUri).build();

            // Validación por defecto con issuer
            OAuth2TokenValidator<Jwt> validator = JwtValidators.createDefaultWithIssuer(issuerUri);

            // Si definiste clientId, valida azp == clientId
            if (StringUtils.hasText(clientId)) {
                var azpValidator = new JwtClaimValidator<String>(AZP, azp -> azp != null && azp.equals(clientId));
                validator = new DelegatingOAuth2TokenValidator<>(validator, azpValidator);
            }

            decoder.setJwtValidator(validator);
            return decoder;
        }

        // Si no hay ni secreto ni issuer, falla claramente
        throw new IllegalStateException("Configura 'security.jwt.secret' (modo local HS256) o 'spring.security.oauth2.resourceserver.jwt.issuer-uri' (modo externo).");
    }

    public Converter<Jwt, Mono<AbstractAuthenticationToken>> grantedAuthoritiesExtractor() {
        var jwtConverter = new JwtAuthenticationConverter();
        jwtConverter.setJwtGrantedAuthoritiesConverter(jwt ->
                getRoles(jwt.getClaims(), jsonExpRoles)
                .stream()
                .filter(Objects::nonNull)
                .map(r -> r.toUpperCase(Locale.ROOT))
                .map(r -> r.startsWith(ROLE) ? r : ROLE + r)
                //.map(ROLE::concat)
                .map(SimpleGrantedAuthority::new)
                .collect(Collectors.toList()));
        return new ReactiveJwtAuthenticationConverterAdapter(jwtConverter);
    }

    private List<String> getRoles(Map<String, Object> claims, String jsonExpClaim){
        List<String> roles = List.of();
        try {
            var json = mapper.writeValueAsString(claims);
            var chunk = mapper.readTree(json).at(jsonExpClaim);
            return mapper.readerFor(new TypeReference<List<String>>() {})
                    .readValue(chunk);
        } catch (IOException e) {
            log.error("Error leyendo roles del token:{}",e.getMessage());
            return roles;
        }
    }
}
