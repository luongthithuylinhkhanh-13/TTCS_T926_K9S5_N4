package com.ntdhtcct.domain.payments;

import com.ntdhtcct.domain.project.Project;
import com.ntdhtcct.domain.project.ProjectRepository;
import com.ntdhtcct.entity.User;
import com.ntdhtcct.repository.UserRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

@Component
@Profile("!test")
@Order(9)
public class PaymentRequestDemoSeeder implements CommandLineRunner {

    private final ProjectRepository projectRepository;
    private final UserRepository userRepository;
    private final PaymentRequestRepository paymentRequestRepository;

    public PaymentRequestDemoSeeder(
            ProjectRepository projectRepository,
            UserRepository userRepository,
            PaymentRequestRepository paymentRequestRepository
    ) {
        this.projectRepository = projectRepository;
        this.userRepository = userRepository;
        this.paymentRequestRepository = paymentRequestRepository;
    }

    @Override
    public void run(String... args) {
        Project project = projectRepository.findByCode("DA-001").orElse(null);
        if (project == null) {
            return;
        }

        if (paymentRequestRepository.count() > 0) {
            return;
        }

        User requester = userRepository.findByEmailIgnoreCase("chinh.work@gmail.com")
                .or(() -> userRepository.findByEmailIgnoreCase("test@test.com"))
                .or(() -> userRepository.findByEmailIgnoreCase("admin@admin.com"))
                .or(() -> userRepository.findAll().stream().findFirst())
                .orElse(null);

        if (requester == null) {
            return;
        }

        User approver = userRepository.findByEmailIgnoreCase("admin@admin.com")
                .orElse(requester);

        // 1. Đề nghị thanh toán cọc bê tông D400 (Chờ duyệt)
        PaymentRequest req1 = new PaymentRequest(
                project,
                "DNTT-001",
                "Đề nghị thanh toán chi phí ép cọc bê tông ly tâm D400 đợt 1 (1.500 md cọc)",
                new BigDecimal("285000000.00"),
                requester
        );
        paymentRequestRepository.save(req1);

        // 2. Đề nghị thanh toán máy đào và nhân công (Chờ duyệt)
        PaymentRequest req2 = new PaymentRequest(
                project,
                "DNTT-002",
                "Thanh toán tiền thuê máy đào bánh xích và nhân công đào đất hố móng",
                new BigDecimal("48000000.00"),
                requester
        );
        paymentRequestRepository.save(req2);

        // 3. Đề nghị mua thép dầm móng (Đã duyệt)
        PaymentRequest req3 = new PaymentRequest(
                project,
                "DNTT-003",
                "Tạm ứng mua thép xây dựng Hòa Phát cho dầm và đài móng trục 1-6",
                new BigDecimal("185000000.00"),
                requester
        );
        req3.decide(PaymentRequestStatus.APPROVED, "Hồ sơ khối lượng và hóa đơn hợp lệ, đồng ý phê duyệt giải ngân.", approver);
        paymentRequestRepository.save(req3);
    }
}
