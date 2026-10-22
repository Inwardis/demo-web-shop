package com.webshop.payments;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * The Payments container: the only service that talks to Stripe and the only one in
 * PCI scope. Deployed, credentialed and audited on its own — see ADR-003.
 *
 * @inwardis 43878454-abed-4e95-8595-a7b1cb05059c Payments
 */
@SpringBootApplication
public class PaymentsApplication {
    public static void main(String[] args) {
        SpringApplication.run(PaymentsApplication.class, args);
    }
}
