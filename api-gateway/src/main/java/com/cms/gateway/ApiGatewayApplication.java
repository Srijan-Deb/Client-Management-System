package com.cms.gateway;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * CMS API Gateway â€” Spring Cloud Gateway (reactive / WebFlux).
 *
 * <p>Runs on port 8090 and routes all inbound requests to backend services:
 * <ul>
 *   <li>/api/clients/**      â†’ client-service   :8081</li>
 *   <li>/api/accounts/**     â†’ account-service  :8082</li>
 *   <li>/api/billing/**      â†’ billing-service  :8083</li>
 *   <li>/api/notifications/** â†’ notification-service :8084</li>
 * </ul>
 *
 * <p>JWT validation is performed at the gateway level. Backend services
 * independently re-validate JWTs (Defence-in-depth â€” Decision 1 from Phase 1 plan).
 */
@SpringBootApplication
public class ApiGatewayApplication {

    public static void main(String[] args) {
        SpringApplication.run(ApiGatewayApplication.class, args);
    }
}
