package com.ntdhtcct.domain.payments;

import com.ntdhtcct.common.exception.BadRequestException;
import com.ntdhtcct.common.exception.ResourceNotFoundException;
import com.ntdhtcct.domain.project.Project;
import com.ntdhtcct.domain.project.ProjectRepository;
import com.ntdhtcct.entity.User;
import com.ntdhtcct.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
public class PaymentRequestService {

    private final PaymentRequestRepository paymentRequestRepository;
        private final ProjectRepository projectRepository;
    private final UserRepository userRepository;

    public PaymentRequestService(
            PaymentRequestRepository paymentRequestRepository,
                        ProjectRepository projectRepository,
            UserRepository userRepository) {
        this.paymentRequestRepository = paymentRequestRepository;
                this.projectRepository = projectRepository;
        this.userRepository = userRepository;
    }

    @Transactional(readOnly = true)
    public List<PaymentRequestResponse> getPendingRequests(UUID projectId) {
        return paymentRequestRepository
                .findByProject_IdAndStatusOrderBySubmittedAtAsc(
                        projectId, PaymentRequestStatus.PENDING)
                .stream()
                .map(PaymentRequestResponse::new)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<PaymentRequestResponse> getMyRequests(UUID projectId, UUID requesterId) {
        return paymentRequestRepository
                .findByProject_IdAndRequester_IdOrderBySubmittedAtDesc(projectId, requesterId)
                .stream()
                .map(PaymentRequestResponse::new)
                .toList();
    }

    @Transactional
    public PaymentRequestResponse submit(
            UUID projectId,
            UUID requesterId,
            PaymentRequestSubmission submission) {
        Project project = projectRepository.findById(projectId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "PROJECT_NOT_FOUND", "Không tìm thấy công trình."));
        User requester = userRepository.findById(requesterId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "USER_NOT_FOUND", "Không tìm thấy người gửi đề nghị."));
        String requestCode = "PAY-" + UUID.randomUUID().toString().toUpperCase();
        PaymentRequest request = new PaymentRequest(
                project,
                requestCode,
                submission.purpose().trim(),
                submission.amount(),
                requester);
        return new PaymentRequestResponse(paymentRequestRepository.save(request));
    }

    @Transactional
    public PaymentRequestResponse approve(UUID projectId, UUID requestId, UUID approverId) {
        return decide(projectId, requestId, approverId, PaymentRequestStatus.APPROVED, null);
    }

    @Transactional
    public PaymentRequestResponse reject(
            UUID projectId,
            UUID requestId,
            UUID approverId,
            String reason) {
        if (reason == null || reason.isBlank()) {
            throw new BadRequestException(
                    "PAYMENT_REJECTION_REASON_REQUIRED",
                    "Vui lòng ghi rõ lý do trả lại đề nghị.");
        }
        return decide(
                projectId,
                requestId,
                approverId,
                PaymentRequestStatus.REJECTED,
                reason.trim());
    }

    private PaymentRequestResponse decide(
            UUID projectId,
            UUID requestId,
            UUID approverId,
            PaymentRequestStatus decision,
            String reason) {
        PaymentRequest request = paymentRequestRepository
                .findByIdAndProject_Id(requestId, projectId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "PAYMENT_REQUEST_NOT_FOUND",
                        "Không tìm thấy đề nghị thanh toán trong công trình này."));

        if (request.getStatus() != PaymentRequestStatus.PENDING) {
            throw new BadRequestException(
                    "PAYMENT_REQUEST_ALREADY_DECIDED",
                    "Đề nghị thanh toán không còn ở trạng thái chờ duyệt.");
        }

        User approver = userRepository.findById(approverId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "USER_NOT_FOUND", "Không tìm thấy người duyệt."));
        request.decide(decision, reason, approver);
        return new PaymentRequestResponse(paymentRequestRepository.save(request));
    }
}