package com.elvero.shop.service;

import com.elvero.shop.exception.ApiException;
import jakarta.mail.internet.InternetAddress;
import jakarta.mail.internet.MimeMessage;
import java.time.Year;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;
import org.springframework.web.util.HtmlUtils;

/** Port of email.service.js using Spring Mail (SMTP configured through the EMAIL_* variables). */
@Service
public class EmailService {

    private static final Logger log = LoggerFactory.getLogger(EmailService.class);

    private final JavaMailSender mailSender;
    private final String from;
    private final String senderName;

    public EmailService(JavaMailSender mailSender,
                        @Value("${app.mail.from}") String from,
                        @Value("${app.mail.sender-name}") String senderName) {
        this.mailSender = mailSender;
        this.from = from;
        this.senderName = senderName;
    }

    public void sendEmailOtp(String email, String firstName, String lastName, String otp) {
        String fullName = ((firstName == null ? "" : firstName) + " " + (lastName == null ? "" : lastName)).trim();
        String name = HtmlUtils.htmlEscape(fullName.isEmpty() ? "Customer" : fullName);

        String text = """
                Hello %s,

                Your OTP for registration is %s.

                This OTP is valid for 10 minutes.

                Please do not share this OTP with anyone.

                Regards,
                %s
                """.formatted(name, otp, senderName);

        String html = """
                <!DOCTYPE html>
                <html>
                <body style="margin:0;padding:20px;background:#f4f4f4;font-family:Arial,sans-serif;">
                  <div style="max-width:500px;margin:auto;background:#ffffff;padding:30px;border-radius:10px;">
                    <h2 style="color:#222;text-align:center;">Verify Your Email</h2>
                    <p>Hello %s,</p>
                    <p>Thank you for choosing %s. Please use the OTP below to verify your email address.</p>
                    <div style="background:#f2f2f2;padding:20px;text-align:center;border-radius:8px;margin:25px 0;">
                      <h1 style="letter-spacing:8px;color:#2563eb;margin:0;">%s</h1>
                    </div>
                    <p style="color:#555;">This OTP is valid for <b>10 minutes</b>.</p>
                    <p style="color:#d9534f;">Please do not share this OTP with anyone.</p>
                    <hr />
                    <p style="font-size:12px;color:#888;text-align:center;">&copy; %d %s. All rights reserved.</p>
                  </div>
                </body>
                </html>
                """.formatted(name, HtmlUtils.htmlEscape(senderName), otp, Year.now().getValue(),
                HtmlUtils.htmlEscape(senderName));

        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
            helper.setFrom(new InternetAddress(from, senderName, "UTF-8"));
            helper.setTo(email);
            helper.setSubject("Your Registration OTP - Your Store");
            helper.setText(text, html);
            mailSender.send(message);
        } catch (Exception e) {
            log.error("Email OTP error", e);
            throw ApiException.badRequest("Failed to send email OTP");
        }
    }
}
