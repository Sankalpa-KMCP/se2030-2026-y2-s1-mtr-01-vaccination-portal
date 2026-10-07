package com.se2030.vaccinationportal.shared.auth.service;

import com.se2030.vaccinationportal.shared.auth.exception.AuthEmailDeliveryException;
import jakarta.mail.MessagingException;
import jakarta.mail.Session;
import jakarta.mail.internet.MimeMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.mail.MailException;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;
import org.springframework.web.util.HtmlUtils;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Objects;

/**
 * Production SMTP delivery implementation for verification and password reset links.
 * Active by default or when app.security.auth.delivery-mode=smtp.
 * Dispatches multipart HTML and plain-text emails.
 * Never logs raw tokens, token hashes, or links.
 */
@Service
@ConditionalOnProperty(
        name = "app.security.auth.delivery-mode",
        havingValue = "smtp",
        matchIfMissing = true
)
public class SmtpAuthLinkDeliveryService implements AuthLinkDeliveryService {

    private static final Logger log = LoggerFactory.getLogger(SmtpAuthLinkDeliveryService.class);

    private final JavaMailSender mailSender;
    private final String frontendBaseUrl;
    private final String mailFrom;
    private final int verificationTokenExpirationHours;
    private final int resetTokenExpirationMinutes;

    public SmtpAuthLinkDeliveryService(
            JavaMailSender mailSender,
            @Value("${app.auth.frontend-base-url:http://localhost:5173}") String frontendBaseUrl,
            @Value("${app.security.auth.mail-from:noreply@vaccinationportal.gov.lk}") String mailFrom,
            @Value("${app.security.auth.verification-token-expiration-hours:24}") int verificationTokenExpirationHours,
            @Value("${app.security.auth.reset-token-expiration-minutes:30}") int resetTokenExpirationMinutes
    ) {
        this.mailSender = Objects.requireNonNull(mailSender, "JavaMailSender must not be null");
        this.frontendBaseUrl = frontendBaseUrl == null ? "http://localhost:5173" : frontendBaseUrl.replaceAll("/+$", "");
        if (mailFrom == null || mailFrom.isBlank()) {
            throw new IllegalStateException("SMTP sender email address (app.security.auth.mail-from) must be configured");
        }
        this.mailFrom = mailFrom.trim();
        this.verificationTokenExpirationHours = verificationTokenExpirationHours;
        this.resetTokenExpirationMinutes = resetTokenExpirationMinutes;
    }

    @Override
    public void sendEmailVerificationLink(String email, String rawToken) {
        String encodedToken = URLEncoder.encode(rawToken, StandardCharsets.UTF_8);
        String verificationUrl = frontendBaseUrl + "/verify-email?token=" + encodedToken;

        String subject = "Vaccination Portal - Verify your email address";
        String plainTextBody = "Vaccination Portal\n"
                + "Verify your email address\n\n"
                + "Please verify your email address to secure your citizen vaccination account by clicking the link below:\n"
                + verificationUrl + "\n\n"
                + "This verification link expires in " + verificationTokenExpirationHours + " hours.\n\n"
                + "If you did not register for an account, please ignore this email.";

        String htmlBody = buildHtmlEmail(
                "Verify your email address",
                "Please verify your email address to secure your citizen vaccination account by clicking the button below:",
                "Verify Email Address",
                verificationUrl,
                "This verification link expires in " + verificationTokenExpirationHours + " hours.",
                "If you did not register for an account, please ignore this email."
        );

        try {
            sendMultipartEmail(email, subject, plainTextBody, htmlBody);
            log.info("[AUTH DELIVERY] Dispatched verification email to [{}] via SMTP", email);
        } catch (MessagingException | MailException ex) {
            log.error("[AUTH DELIVERY] Failed to deliver verification email to [{}] via SMTP: {}", email, ex.getMessage());
            throw new AuthEmailDeliveryException("Failed to deliver verification email. Please try again later.", ex);
        }
    }

    @Override
    public void sendPasswordResetLink(String email, String rawToken) {
        String encodedToken = URLEncoder.encode(rawToken, StandardCharsets.UTF_8);
        String resetUrl = frontendBaseUrl + "/reset-password?token=" + encodedToken;

        String subject = "Vaccination Portal - Password Reset Request";
        String plainTextBody = "Vaccination Portal\n"
                + "Reset your password\n\n"
                + "A password reset request was received for your account. You can set a new password by clicking the link below:\n"
                + resetUrl + "\n\n"
                + "This password reset link expires in " + resetTokenExpirationMinutes + " minutes.\n\n"
                + "If you did not request a password reset, please ignore this email.";

        String htmlBody = buildHtmlEmail(
                "Reset your password",
                "A password reset request was received for your account. You can set a new password by clicking the button below:",
                "Reset Password",
                resetUrl,
                "This password reset link expires in " + resetTokenExpirationMinutes + " minutes.",
                "If you did not request a password reset, please ignore this email. Your password will not change until you access the link above and create a new one."
        );

        try {
            sendMultipartEmail(email, subject, plainTextBody, htmlBody);
            log.info("[AUTH DELIVERY] Dispatched password reset email to [{}] via SMTP", email);
        } catch (MessagingException | MailException ex) {
            log.error("[AUTH DELIVERY] Failed to deliver password reset email to [{}] via SMTP: {}", email, ex.getMessage());
            throw new AuthEmailDeliveryException("Failed to deliver password reset email. Please try again later.", ex);
        }
    }

    private void sendMultipartEmail(String recipient, String subject, String plainText, String html) throws MessagingException, MailException {
        MimeMessage mimeMessage = mailSender.createMimeMessage();
        if (mimeMessage == null) {
            mimeMessage = new MimeMessage((Session) null);
        }
        MimeMessageHelper helper = new MimeMessageHelper(mimeMessage, true, StandardCharsets.UTF_8.name());
        helper.setFrom(mailFrom);
        helper.setTo(recipient.trim());
        helper.setSubject(subject);
        helper.setText(plainText, html);
        mimeMessage.saveChanges();
        mailSender.send(mimeMessage);
    }

    private String buildHtmlEmail(
            String heading,
            String introParagraph,
            String buttonText,
            String actionUrl,
            String expiryText,
            String securityNote
    ) {
        String safeHeading = HtmlUtils.htmlEscape(heading);
        String safeIntro = HtmlUtils.htmlEscape(introParagraph);
        String safeButtonText = HtmlUtils.htmlEscape(buttonText);
        String safeActionUrl = HtmlUtils.htmlEscape(actionUrl);
        String safeExpiryText = HtmlUtils.htmlEscape(expiryText);
        String safeSecurityNote = HtmlUtils.htmlEscape(securityNote);

        return "<!DOCTYPE html>\n"
                + "<html lang=\"en\">\n"
                + "<head>\n"
                + "  <meta charset=\"UTF-8\">\n"
                + "  <meta name=\"viewport\" content=\"width=device-width, initial-scale=1.0\">\n"
                + "  <meta http-equiv=\"X-UA-Compatible\" content=\"IE=edge\">\n"
                + "  <title>" + safeHeading + "</title>\n"
                + "</head>\n"
                + "<body style=\"margin:0;padding:0;background-color:#f3f5f7;font-family:Arial,Helvetica,sans-serif;-webkit-font-smoothing:antialiased;\">\n"
                + "  <table role=\"presentation\" width=\"100%\" cellspacing=\"0\" cellpadding=\"0\" border=\"0\" bgcolor=\"#f3f5f7\" style=\"background-color:#f3f5f7;margin:0;padding:32px 16px;\">\n"
                + "    <tr>\n"
                + "      <td align=\"center\">\n"
                + "        <table role=\"presentation\" width=\"100%\" cellspacing=\"0\" cellpadding=\"0\" border=\"0\" style=\"max-width:600px;width:100%;margin:0 auto;background-color:#ffffff;border-radius:8px;border:1px solid #e2e8f0;overflow:hidden;box-shadow:0 2px 4px rgba(0,0,0,0.05);\">\n"
                + "          <tr>\n"
                + "            <td style=\"background-color:#0B6E99;padding:24px 32px;text-align:left;\">\n"
                + "              <span style=\"font-size:20px;font-weight:bold;color:#ffffff;letter-spacing:0.5px;font-family:Arial,Helvetica,sans-serif;\">Vaccination Portal</span>\n"
                + "            </td>\n"
                + "          </tr>\n"
                + "          <tr>\n"
                + "            <td style=\"padding:32px;text-align:left;color:#1e293b;font-size:15px;line-height:1.6;\">\n"
                + "              <h1 style=\"margin:0 0 16px 0;font-size:22px;font-weight:700;color:#0f172a;line-height:1.3;\">" + safeHeading + "</h1>\n"
                + "              <p style=\"margin:0 0 20px 0;color:#334155;font-size:15px;line-height:1.6;\">" + safeIntro + "</p>\n"
                + "              <table role=\"presentation\" cellspacing=\"0\" cellpadding=\"0\" border=\"0\" style=\"margin:28px 0;\">\n"
                + "                <tr>\n"
                + "                  <td align=\"center\" bgcolor=\"#0B6E99\" style=\"border-radius:6px;background-color:#0B6E99;\">\n"
                + "                    <a href=\"" + safeActionUrl + "\" target=\"_blank\" rel=\"noopener noreferrer\" style=\"display:inline-block;padding:14px 28px;font-family:Arial,Helvetica,sans-serif;font-size:15px;font-weight:bold;color:#ffffff;text-decoration:none;border-radius:6px;background-color:#0B6E99;\">" + safeButtonText + "</a>\n"
                + "                  </td>\n"
                + "                </tr>\n"
                + "              </table>\n"
                + "              <table role=\"presentation\" width=\"100%\" cellspacing=\"0\" cellpadding=\"0\" border=\"0\" style=\"margin:24px 0 0 0;background-color:#f8fafc;border:1px solid #e2e8f0;border-radius:6px;\">\n"
                + "                <tr>\n"
                + "                  <td style=\"padding:16px;font-size:13px;line-height:1.5;color:#475569;\">\n"
                + "                    <p style=\"margin:0 0 8px 0;font-weight:bold;color:#1e293b;\">Security Notice</p>\n"
                + "                    <p style=\"margin:0 0 8px 0;\">" + safeExpiryText + "</p>\n"
                + "                    <p style=\"margin:0;color:#64748b;\">" + safeSecurityNote + "</p>\n"
                + "                  </td>\n"
                + "                </tr>\n"
                + "              </table>\n"
                + "              <div style=\"margin-top:28px;padding-top:20px;border-top:1px solid #e2e8f0;font-size:13px;color:#64748b;line-height:1.5;\">\n"
                + "                <p style=\"margin:0 0 8px 0;\">If the button above does not work, copy and paste the following link into your web browser:</p>\n"
                + "                <p style=\"margin:0;word-break:break-all;\"><a href=\"" + safeActionUrl + "\" style=\"color:#0B6E99;text-decoration:underline;\">" + safeActionUrl + "</a></p>\n"
                + "              </div>\n"
                + "            </td>\n"
                + "          </tr>\n"
                + "          <tr>\n"
                + "            <td style=\"background-color:#f8fafc;border-top:1px solid #e2e8f0;padding:20px 32px;text-align:center;font-size:12px;color:#94a3b8;line-height:1.5;\">\n"
                + "              <p style=\"margin:0 0 4px 0;\">Vaccination Portal &bull; Automated security notification.</p>\n"
                + "              <p style=\"margin:0;\">Please do not reply to this email.</p>\n"
                + "            </td>\n"
                + "          </tr>\n"
                + "        </table>\n"
                + "      </td>\n"
                + "    </tr>\n"
                + "  </table>\n"
                + "</body>\n"
                + "</html>";
    }
}
