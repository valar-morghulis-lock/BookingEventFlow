package com.bookingeventflow.customer.config;

import com.nimbusds.jose.jwk.JWKSet;
import com.nimbusds.jose.jwk.RSAKey;
import com.nimbusds.jose.jwk.source.ImmutableJWKSet;
import com.nimbusds.jose.jwk.source.JWKSource;
import com.nimbusds.jose.proc.SecurityContext;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.Resource;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.oauth2.jwt.NimbusJwtEncoder;

import java.security.KeyFactory;
import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;
import java.util.Base64;

@Configuration
public class JwtKeyConfig {

    private static final String KEY_ID = "customer-service-key";

    private final RSAPublicKey publicKey;
    private final RSAPrivateKey privateKey;

    public JwtKeyConfig(
            @Value("${jwt.public-key-path}") Resource publicKeyResource,
            @Value("${jwt.private-key-path}") Resource privateKeyResource
    ) throws Exception {
        this.publicKey = readPublicKey(publicKeyResource);
        this.privateKey = readPrivateKey(privateKeyResource);
    }

    /**
     * The single source of truth for this service's RSA key pair,
     * including its key ID. Both the encoder (signs tokens) and the
     * JWKS controller (publishes the public key) must use this SAME
     * RSAKey instance, so the "kid" in a signed token's header always
     * matches the "kid" published at /.well-known/jwks.json.
     */
    @Bean
    public RSAKey rsaKey() {
        return new RSAKey.Builder(publicKey)
                .privateKey(privateKey)
                .keyID(KEY_ID)
                .build();
    }

    @Bean
    public JwtEncoder jwtEncoder(RSAKey rsaKey) {

        JWKSource<SecurityContext> jwkSource = new ImmutableJWKSet<>(new JWKSet(rsaKey));

        return new NimbusJwtEncoder(jwkSource);
    }

    @Bean
    public JwtDecoder jwtDecoder() {
        return NimbusJwtDecoder.withPublicKey(publicKey).build();
    }

    private RSAPublicKey readPublicKey(Resource resource) throws Exception {

        String key = new String(resource.getInputStream().readAllBytes())
                .replace("-----BEGIN PUBLIC KEY-----", "")
                .replace("-----END PUBLIC KEY-----", "")
                .replaceAll("\\s", "");

        byte[] decoded = Base64.getDecoder().decode(key);
        X509EncodedKeySpec spec = new X509EncodedKeySpec(decoded);

        return (RSAPublicKey) KeyFactory.getInstance("RSA").generatePublic(spec);
    }

    private RSAPrivateKey readPrivateKey(Resource resource) throws Exception {

        String key = new String(resource.getInputStream().readAllBytes())
                .replace("-----BEGIN PRIVATE KEY-----", "")
                .replace("-----END PRIVATE KEY-----", "")
                .replaceAll("\\s", "");

        byte[] decoded = Base64.getDecoder().decode(key);
        PKCS8EncodedKeySpec spec = new PKCS8EncodedKeySpec(decoded);

        return (RSAPrivateKey) KeyFactory.getInstance("RSA").generatePrivate(spec);
    }
}