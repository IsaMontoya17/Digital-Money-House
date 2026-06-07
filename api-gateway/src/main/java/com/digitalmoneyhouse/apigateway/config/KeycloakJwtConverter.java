package com.digitalmoneyhouse.apigateway.config;

import org.springframework.core.convert.converter.Converter;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;
import reactor.core.publisher.Flux;

import java.util.List;
import java.util.Map;

public class KeycloakJwtConverter implements Converter<Jwt, Flux<GrantedAuthority>> {

    @Override
    public Flux<GrantedAuthority> convert(Jwt jwt) {
        Map<String, Object> realmAccess = jwt.getClaimAsMap("realm_access");
        if (realmAccess == null) return Flux.empty();
        List<String> roles = (List<String>) realmAccess.get("roles");
        if (roles == null) return Flux.empty();
        return Flux.fromIterable(roles)
                .map(SimpleGrantedAuthority::new);
    }
}