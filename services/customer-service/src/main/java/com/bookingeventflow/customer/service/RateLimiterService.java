package com.bookingeventflow.customer.service;

import io.github.bucket4j.Bucket;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.function.Supplier;

@Service
public class RateLimiterService {

    private final Map<String, Bucket> loginBuckets;
    private final Map<String, Bucket> loginEmailBuckets;
    private final Map<String, Bucket> loginIpBuckets;
    private final Map<String, Bucket> registerBuckets;

    private final Supplier<Bucket> loginBucketSupplier;
    private final Supplier<Bucket> loginEmailBucketSupplier;
    private final Supplier<Bucket> loginIpBucketSupplier;
    private final Supplier<Bucket> registerBucketSupplier;

    public RateLimiterService(
            Map<String, Bucket> loginRateLimitBuckets,
            Map<String, Bucket> loginEmailRateLimitBuckets,
            Map<String, Bucket> loginIpRateLimitBuckets,
            Map<String, Bucket> registerRateLimitBuckets,
            Supplier<Bucket> loginBucketSupplier,
            Supplier<Bucket> loginEmailBucketSupplier,
            Supplier<Bucket> loginIpBucketSupplier,
            Supplier<Bucket> registerBucketSupplier
    ) {
        this.loginBuckets = loginRateLimitBuckets;
        this.loginEmailBuckets = loginEmailRateLimitBuckets;
        this.loginIpBuckets = loginIpRateLimitBuckets;
        this.registerBuckets = registerRateLimitBuckets;

        this.loginBucketSupplier = loginBucketSupplier;
        this.loginEmailBucketSupplier = loginEmailBucketSupplier;
        this.loginIpBucketSupplier = loginIpBucketSupplier;
        this.registerBucketSupplier = registerBucketSupplier;
    }

    /**
     * Tightest check — one bucket per (IP, email) pair. Stops
     * brute-forcing a single account from a single source.
     */
    public boolean tryConsumeLogin(String ipAndEmailKey) {
        Bucket bucket = loginBuckets.computeIfAbsent(ipAndEmailKey, k -> loginBucketSupplier.get());
        return bucket.tryConsume(1);
    }

    /**
     * One bucket per email, regardless of source IP. Stops
     * brute-forcing a single account across rotating IPs.
     */
    public boolean tryConsumeLoginByEmail(String email) {
        Bucket bucket = loginEmailBuckets.computeIfAbsent(email, k -> loginEmailBucketSupplier.get());
        return bucket.tryConsume(1);
    }

    /**
     * One bucket per IP, regardless of target email. Stops one
     * source from flooding many accounts / the server generally.
     */
    public boolean tryConsumeLoginByIp(String ip) {
        Bucket bucket = loginIpBuckets.computeIfAbsent(ip, k -> loginIpBucketSupplier.get());
        return bucket.tryConsume(1);
    }

    public boolean tryConsumeRegister(String key) {
        Bucket bucket = registerBuckets.computeIfAbsent(key, k -> registerBucketSupplier.get());
        return bucket.tryConsume(1);
    }
}