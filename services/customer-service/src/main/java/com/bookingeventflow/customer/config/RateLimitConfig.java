package com.bookingeventflow.customer.config;

import io.github.bucket4j.Bandwidth;
import io.github.bucket4j.Bucket;
import io.github.bucket4j.Refill;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Duration;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Supplier;

@Configuration
public class RateLimitConfig {

    // ===== Login: IP + email (existing — tightest, per-account-per-source) =====

    @Bean
    public ConcurrentHashMap<String, Bucket> loginRateLimitBuckets() {
        return new ConcurrentHashMap<>();
    }

    @Bean
    public Supplier<Bucket> loginBucketSupplier() {
        return () -> Bucket.builder()
                .addLimit(Bandwidth.classic(5, Refill.greedy(5, Duration.ofMinutes(1))))
                .build();
    }

    // ===== Login: email only (stops brute-force across rotating IPs) =====

    @Bean
    public ConcurrentHashMap<String, Bucket> loginEmailRateLimitBuckets() {
        return new ConcurrentHashMap<>();
    }

    @Bean
    public Supplier<Bucket> loginEmailBucketSupplier() {
        // Slightly looser than the IP+email bucket, since legitimate
        // users occasionally mistype passwords across app restarts/
        // devices; still tight enough to block sustained brute force.
        return () -> Bucket.builder()
                .addLimit(Bandwidth.classic(10, Refill.greedy(10, Duration.ofMinutes(1))))
                .build();
    }

    // ===== Login: IP only (stops flooding many accounts from one source) =====

    @Bean
    public ConcurrentHashMap<String, Bucket> loginIpRateLimitBuckets() {
        return new ConcurrentHashMap<>();
    }

    @Bean
    public Supplier<Bucket> loginIpBucketSupplier() {
        // Looser still — this is a broad "don't flood the server"
        // ceiling, not an account-specific protection.
        return () -> Bucket.builder()
                .addLimit(Bandwidth.classic(30, Refill.greedy(30, Duration.ofMinutes(1))))
                .build();
    }

    // ===== Register: IP only (existing) =====

    @Bean
    public ConcurrentHashMap<String, Bucket> registerRateLimitBuckets() {
        return new ConcurrentHashMap<>();
    }

    @Bean
    public Supplier<Bucket> registerBucketSupplier() {
        return () -> Bucket.builder()
                .addLimit(Bandwidth.classic(3, Refill.greedy(3, Duration.ofMinutes(10))))
                .build();
    }
}