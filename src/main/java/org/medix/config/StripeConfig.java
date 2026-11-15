package org.medix.config;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.annotation.PostConstruct;

import com.stripe.Stripe;

import org.eclipse.microprofile.config.inject.ConfigProperty;

@ApplicationScoped
public class StripeConfig {

    @ConfigProperty(name = "stripe.secret-key")
    String secretKey;

    @PostConstruct
    void initialize() {
        Stripe.apiKey = secretKey;
    }
}