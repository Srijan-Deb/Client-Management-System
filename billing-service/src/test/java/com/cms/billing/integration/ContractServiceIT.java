package com.cms.billing.integration;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.cms.billing.domain.dto.ContractRequest;
import com.cms.billing.domain.dto.ContractResponse;
import com.cms.billing.domain.entity.AuditLog;
import com.cms.billing.domain.entity.Contract;
import com.cms.billing.domain.entity.Product;
import com.cms.billing.domain.entity.ProductCategory;
import com.cms.billing.repository.AuditLogRepository;
import com.cms.billing.repository.ContractRepository;
import com.cms.billing.repository.ProductCategoryRepository;
import com.cms.billing.repository.ProductRepository;
import com.cms.billing.service.ContractService;
import java.math.BigDecimal;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest(properties = {
        "spring.datasource.url=jdbc:h2:mem:testdb;MODE=MYSQL",
        "spring.datasource.driver-class-name=org.h2.Driver",
        "spring.jpa.hibernate.ddl-auto=create-drop",
        "spring.flyway.enabled=false",
        "spring.cache.type=simple",
        "spring.security.oauth2.resourceserver.jwt.issuer-uri=http://localhost:19999/realms/test",
        "spring.security.oauth2.resourceserver.jwt.jwk-set-uri=http://localhost:19999/realms/test/protocol/openid-connect/certs"
})
@ActiveProfiles("test")
@Transactional
public class ContractServiceIT {

    @Autowired
    private ContractService contractService;

    @Autowired
    private ContractRepository contractRepository;

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private ProductCategoryRepository productCategoryRepository;

    @Autowired
    private AuditLogRepository auditLogRepository;

    private Product testProduct;

    @BeforeEach
    void setup() {
        ProductCategory category = new ProductCategory();
        category.setName("SaaS");
        category.setDescription("Software as a Service");
        category = productCategoryRepository.save(category);

        testProduct = new Product();
        testProduct.setCategory(category);
        testProduct.setName("Premium Plan");
        testProduct.setPrice(new BigDecimal("100.00"));
        testProduct.setBillingCycle("MONTHLY");
        testProduct.setIsActive(true);
        testProduct = productRepository.save(testProduct);
    }

    @Test
    void testCreateContract() {
        ContractRequest request = new ContractRequest();
        request.setClientId(1L);
        request.setAccountId(1L);
        request.setRecipientEmail("test@example.com");
        request.setProductIds(List.of(testProduct.getId()));

        ContractResponse response = contractService.createContract(request, "test_user");

        assertNotNull(response);
        assertNotNull(response.getContractId());
        assertNotNull(response.getInvoiceId());
        assertNotNull(response.getPdfUrl());
        assertTrue(response.getPdfUrl().startsWith("invoices/1/INV-"));
        // 100 + 18% tax = 118.00
        assertEquals(new BigDecimal("118.00"), response.getTotalValue());

        Contract dbContract = contractRepository.findById(response.getContractId()).orElseThrow();
        assertEquals(1, dbContract.getSubscriptions().size());
        assertEquals(1, dbContract.getInvoices().size());

        List<AuditLog> logs = auditLogRepository.findAll();
        boolean foundAudit = logs.stream().anyMatch(l -> 
                l.getEntityName().equals("Contract") && 
                l.getEntityId().equals(response.getContractId())
        );
        assertTrue(foundAudit, "Audit log should be created");
    }
}
