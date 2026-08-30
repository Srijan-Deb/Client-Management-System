package com.cms.billing.integration;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.cms.billing.domain.dto.PaymentRequest;
import com.cms.billing.domain.dto.PaymentResponse;
import com.cms.billing.domain.entity.Invoice;
import com.cms.billing.repository.InvoiceRepository;
import com.cms.billing.repository.PaymentRepository;
import com.cms.billing.service.PaymentService;
import com.cms.billing.service.StripeGateway;
import com.stripe.model.Charge;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.ActiveProfiles;
import org.junit.jupiter.api.AfterEach;

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
public class PaymentServiceIT {

    @Autowired
    private PaymentService paymentService;

    @Autowired
    private PaymentRepository paymentRepository;

    @Autowired
    private InvoiceRepository invoiceRepository;

    @MockBean
    private StripeGateway stripeService;

    private Invoice testInvoice;

    @AfterEach
    void tearDown() {
        paymentRepository.deleteAll();
        invoiceRepository.deleteAll();
    }

    @BeforeEach
    void setup() {
        testInvoice = new Invoice();
        testInvoice.setClientId(1L);
        testInvoice.setAccountId(1L);
        testInvoice.setInvoiceNumber("INV-TEST-123");
        testInvoice.setSubtotal(new BigDecimal("100.00"));
        testInvoice.setTaxRate(new BigDecimal("18.00"));
        testInvoice.setTaxAmount(new BigDecimal("18.00"));
        testInvoice.setTotalAmount(new BigDecimal("118.00"));
        testInvoice.setCurrency("INR");
        testInvoice.setDueDate(LocalDate.now().plusDays(30));
        testInvoice.setStatus("PENDING");
        testInvoice = invoiceRepository.save(testInvoice);
    }

    @Test
    void testSuccessfulPaymentAndIdempotency() throws Exception {
        // Mock Stripe success
        Charge mockCharge = new Charge();
        mockCharge.setId("ch_mock_123");
        when(stripeService.createCharge(anyString(), any(), anyString(), anyString())).thenReturn(mockCharge);

        String idempotencyKey = UUID.randomUUID().toString();

        PaymentRequest request = new PaymentRequest();
        request.setInvoiceId(testInvoice.getId());
        request.setAmount(new BigDecimal("118.00"));
        request.setPaymentMethodToken("tok_visa");
        request.setIdempotencyKey(idempotencyKey);
        request.setRecipientEmail("test@example.com");

        // 1. First Call -> Should process and charge
        PaymentResponse response1 = paymentService.processPayment(request, "test_user");
        assertEquals("PAID", response1.getStatus());
        assertEquals("ch_mock_123", response1.getStripePaymentId());

        Invoice dbInvoice = invoiceRepository.findById(testInvoice.getId()).orElseThrow();
        assertEquals("PAID", dbInvoice.getStatus());

        verify(stripeService, times(1)).createCharge(anyString(), any(), anyString(), anyString());

        // 2. Second Call (Idempotent Retry) -> Should NOT charge again, just return existing
        PaymentResponse response2 = paymentService.processPayment(request, "test_user");
        assertEquals("PAID", response2.getStatus());
        assertEquals("ch_mock_123", response2.getStripePaymentId());
        
        // Still only 1 call to stripe
        verify(stripeService, times(1)).createCharge(anyString(), any(), anyString(), anyString());
        
        assertEquals(1, paymentRepository.findAll().size());
    }

    @Test
    void testFailedPayment() throws Exception {
        // Mock Stripe failure
        when(stripeService.createCharge(anyString(), any(), anyString(), anyString()))
                .thenThrow(new RuntimeException("Stripe card declined"));

        PaymentRequest request = new PaymentRequest();
        request.setInvoiceId(testInvoice.getId());
        request.setAmount(new BigDecimal("118.00"));
        request.setPaymentMethodToken("tok_chargeCustomerFail");
        request.setIdempotencyKey(UUID.randomUUID().toString());
        request.setRecipientEmail("test@example.com");

        PaymentResponse response = paymentService.processPayment(request, "test_user");
        assertEquals("FAILED", response.getStatus());

        // Invoice status remains pending
        Invoice dbInvoice = invoiceRepository.findById(testInvoice.getId()).orElseThrow();
        assertEquals("PENDING", dbInvoice.getStatus());
    }

    @Test
    void testConcurrentIdempotency() throws Exception {
        // Mock Stripe success
        Charge mockCharge = new Charge();
        mockCharge.setId("ch_mock_concurrent");
        when(stripeService.createCharge(anyString(), any(), anyString(), anyString())).thenReturn(mockCharge);

        String idempotencyKey = UUID.randomUUID().toString();

        PaymentRequest request = new PaymentRequest();
        request.setInvoiceId(testInvoice.getId());
        request.setAmount(new BigDecimal("118.00"));
        request.setPaymentMethodToken("tok_visa");
        request.setIdempotencyKey(idempotencyKey);
        request.setRecipientEmail("test@example.com");

        int threadCount = 2;
        ExecutorService executorService = Executors.newFixedThreadPool(threadCount);
        CountDownLatch startLatch = new CountDownLatch(1);
        CountDownLatch endLatch = new CountDownLatch(threadCount);
        AtomicReference<Exception> exceptionRef = new AtomicReference<>();

        CompletableFuture<PaymentResponse> future1 = CompletableFuture.supplyAsync(() -> {
            try {
                startLatch.await();
                return paymentService.processPayment(request, "test_user1");
            } catch (Exception e) {
                exceptionRef.set(e);
                throw new RuntimeException(e);
            } finally {
                endLatch.countDown();
            }
        }, executorService);

        CompletableFuture<PaymentResponse> future2 = CompletableFuture.supplyAsync(() -> {
            try {
                startLatch.await();
                return paymentService.processPayment(request, "test_user2");
            } catch (Exception e) {
                exceptionRef.set(e);
                throw new RuntimeException(e);
            } finally {
                endLatch.countDown();
            }
        }, executorService);

        // Start both threads at the same time
        startLatch.countDown();
        endLatch.await();

        if (exceptionRef.get() != null) {
            throw new Exception("Exception occurred during concurrent execution", exceptionRef.get());
        }

        PaymentResponse response1 = future1.get();
        PaymentResponse response2 = future2.get();

        assertNotNull(response1);
        assertNotNull(response2);
        
        // Both responses should return the SAME ID without throwing exceptions
        assertEquals(response1.getPaymentId(), response2.getPaymentId());
        org.junit.jupiter.api.Assertions.assertTrue(
            "PAID".equals(response1.getStatus()) || "PROCESSING".equals(response1.getStatus()),
            "response1 status was: " + response1.getStatus());
        org.junit.jupiter.api.Assertions.assertTrue(
            "PAID".equals(response2.getStatus()) || "PROCESSING".equals(response2.getStatus()),
            "response2 status was: " + response2.getStatus());
        
        verify(stripeService, times(1)).createCharge(anyString(), any(), anyString(), anyString());
        executorService.shutdown();
    }
}
