package com.cms.notification.service;

import com.cms.notification.domain.entity.Notification;
import com.cms.notification.domain.enums.NotificationStatus;
import com.cms.notification.repository.NotificationRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.mail.MailException;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;

import java.time.Instant;
import java.util.Map;

/**
 * Sends HTML emails via JavaMailSender (configured â†’ Mailhog in dev).
 *
 * <p>The method runs in its own transaction ({@code REQUIRES_NEW}) so that
 * a DB audit INSERT always commits even if the Kafka consumer's outer transaction
 * is rolling back for another reason.
 *
 * <p><b>Flow per send attempt:</b>
 * <ol>
 *   <li>INSERT notifications (status=PENDING)</li>
 *   <li>Render Thymeleaf template â†’ HTML body</li>
 *   <li>JavaMailSender.send() â†’ Mailhog SMTP</li>
 *   <li>UPDATE notifications status=SENT (or FAILED on exception)</li>
 * </ol>
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class EmailService {

    private final JavaMailSender       mailSender;
    private final TemplateEngine       templateEngine;
    private final NotificationRepository notificationRepository;

    /**
     * Send an HTML email and persist the audit row.
     *
     * @param to           recipient email address
     * @param subject      email subject (from notification_templates row)
     * @param templateName Thymeleaf template name stem (maps to email/{name}.html)
     * @param eventType    Kafka event type string â€” stored in the audit row
     * @param variables    template variables injected into the Thymeleaf context
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW, rollbackFor = Exception.class)
    public void send(String to, String subject, String templateName,
                     String eventType, Map<String, Object> variables) {

        // 1. Persist PENDING audit row before attempting send
        Notification notification = Notification.builder()
                .eventType(eventType)
                .recipientEmail(to)
                .subject(subject)
                .status(NotificationStatus.PENDING)
                .build();
        notificationRepository.save(notification);

        try {
            // 2. Render Thymeleaf template
            Context ctx = new Context();
            ctx.setVariables(variables);
            String htmlBody = templateEngine.process("email/" + templateName, ctx);

            // 3. Build and send MimeMessage
            mailSender.send(mime -> {
                MimeMessageHelper helper = new MimeMessageHelper(mime, true, "UTF-8");
                helper.setTo(to);
                helper.setSubject(subject);
                helper.setText(htmlBody, true); // true = HTML
            });

            // 4. Mark SENT
            notification.setStatus(NotificationStatus.SENT);
            notification.setSentAt(Instant.now());
            notificationRepository.save(notification);
            log.info("Email sent: eventType={}, to={}", eventType, to);

        } catch (MailException e) {
            notification.setStatus(NotificationStatus.FAILED);
            notification.setErrorMessage(e.getMessage());
            notificationRepository.save(notification);
            log.error("Email FAILED: eventType={}, to={}: {}", eventType, to, e.getMessage());
            throw e; // re-throw so Kafka's error handler can retry / DLT
        } catch (Exception e) {
            notification.setStatus(NotificationStatus.FAILED);
            notification.setErrorMessage(e.getMessage());
            notificationRepository.save(notification);
            log.error("Email FAILED (unexpected): eventType={}, to={}", eventType, to, e);
            throw new RuntimeException("Email send failed: " + e.getMessage(), e);
        }
    }
}
