package com.cms.account;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.kafka.annotation.EnableKafka;

/**
 * CMS Account Service â€” entry point.
 *
 * <p><b>Responsibilities (by build phase):</b>
 * <ul>
 *   <li>Phase 3: Account management, {@code POST /accounts/link/{clientId}}
 *       to link accounts to clients (multi-tenant B2B model)</li>
 * </ul>
 *
 * <p><b>Port:</b> 8082 (see application.yml)
 */
@SpringBootApplication
@EnableCaching
@EnableKafka
public class AccountServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(AccountServiceApplication.class, args);
    }
}
