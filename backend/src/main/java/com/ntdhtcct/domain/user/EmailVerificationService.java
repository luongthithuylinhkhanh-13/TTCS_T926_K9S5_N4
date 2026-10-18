package com.ntdhtcct.domain.user;

import com.ntdhtcct.entity.User;
import com.ntdhtcct.repository.UserRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.UUID;

@Service
public class EmailVerificationService {

    private static final long TOKEN_LIFETIME_HOURS = 24;

    private final EmailVerificationTokenRepository tokenRepository;
    private final UserRepository userRepository;
    private final JavaMailSender mailSender;
    private final String backendUrl;
    private final String mailFrom;

    public EmailVerificationService(
            EmailVerificationTokenRepository tokenRepository,
            UserRepository userRepository,
            JavaMailSender mailSender,
            @Value("${app.backend-url}") String backendUrl,
            @Value("${app.mail-from}") String mailFrom
    ) {
        this.tokenRepository = tokenRepository;
        this.userRepository = userRepository;
        this.mailSender = mailSender;
        this.backendUrl = backendUrl;
        this.mailFrom = mailFrom;
    }

    @Transactional
    public void sendVerificationEmail(User user) {
        tokenRepository.deleteByUserId(user.getId());

        String tokenValue = UUID.randomUUID().toString();
        EmailVerificationToken token = new EmailVerificationToken(
                tokenValue,
                user,
                OffsetDateTime.now().plusHours(TOKEN_LIFETIME_HOURS)
        );
        tokenRepository.save(token);

        SimpleMailMessage message = new SimpleMailMessage();
        message.setFrom(mailFrom);
        message.setTo(user.getEmail());
        message.setSubject("Xác thực tài khoản ConstructFlow");
        message.setText("Xin chào " + user.getFullName() + ",\n\n"
                + "Bấm vào liên kết dưới đây để xác thực email và kích hoạt tài khoản:\n"
                + backendUrl + "/api/auth/verify-email?token=" + tokenValue + "\n\n"
                + "Liên kết có hiệu lực trong 24 giờ và chỉ dùng một lần.\n\n"
                + "Nếu bạn không yêu cầu đăng ký, hãy bỏ qua email này.");
        mailSender.send(message);
    }

    @Transactional
    public void verifyEmail(String tokenValue) {
        EmailVerificationToken token = tokenRepository.findByToken(tokenValue)
                .orElseThrow(() -> new IllegalArgumentException("Liên kết xác thực không hợp lệ."));

        if (token.getUsedAt() != null) {
            throw new IllegalArgumentException("Liên kết xác thực đã được sử dụng.");
        }
        if (token.getExpiresAt().isBefore(OffsetDateTime.now())) {
            throw new IllegalArgumentException("Liên kết xác thực đã hết hạn.");
        }

        User user = token.getUser();
        user.setEmailVerified(true);
        userRepository.save(user);

        token.setUsedAt(OffsetDateTime.now());
        tokenRepository.save(token);
    }
}
