package com.ntdhtcct.domain.payments;

import com.ntdhtcct.common.exception.BadRequestException;
import com.ntdhtcct.common.exception.ResourceNotFoundException;
import com.ntdhtcct.domain.acceptance.AcceptanceItem;
import com.ntdhtcct.domain.acceptance.AcceptanceItemRepository;
import com.ntdhtcct.domain.project.Project;
import com.ntdhtcct.domain.project.ProjectRepository;
import com.ntdhtcct.entity.User;
import com.ntdhtcct.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;
import java.math.BigDecimal;
import java.math.RoundingMode;

@Service
public class PaymentRequestService {

    private final PaymentRequestRepository paymentRequestRepository;
    private final ProjectRepository projectRepository;
    private final UserRepository userRepository;
    private final AcceptanceItemRepository acceptanceItemRepository;

    public PaymentRequestService(
            PaymentRequestRepository paymentRequestRepository,
            ProjectRepository projectRepository,
            UserRepository userRepository,
            AcceptanceItemRepository acceptanceItemRepository) {
        this.paymentRequestRepository = paymentRequestRepository;
        this.projectRepository = projectRepository;
        this.userRepository = userRepository;
        this.acceptanceItemRepository = acceptanceItemRepository;
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

    @Transactional(readOnly = true)
    public List<PaymentRequestEligibleItemResponse> getEligibleItems(UUID projectId) {
        return acceptanceItemRepository.findApprovedByProjectId(projectId).stream()
                .map(PaymentRequestEligibleItemResponse::new)
                .toList();
    }

    @Transactional
    public PaymentRequestResponse createDraft(
            UUID projectId,
            UUID requesterId,
            PaymentRequestSubmission submission) {
        Project project = findProject(projectId);
        User requester = findRequester(requesterId);
        List<PaymentRequestItem> items = buildItems(projectId, submission.items(), true);
        PaymentRequest request = newRequest(project, requester, submission.purpose());
        request.update(submission.purpose().trim(), submission.amount(), items);
        return new PaymentRequestResponse(paymentRequestRepository.save(request));
    }

    @Transactional
    public PaymentRequestResponse updateDraft(
            UUID projectId,
            UUID requestId,
            UUID requesterId,
            PaymentRequestSubmission submission) {
        PaymentRequest request = findOwnedDraft(projectId, requestId, requesterId);
        List<PaymentRequestItem> items = buildItems(projectId, submission.items(), true);
        request.update(submission.purpose().trim(), submission.amount(), items);
        return new PaymentRequestResponse(paymentRequestRepository.save(request));
    }

    @Transactional
    public PaymentRequestResponse submitDraft(UUID projectId, UUID requestId, UUID requesterId) {
        PaymentRequest request = findOwnedDraft(projectId, requestId, requesterId);
        List<PaymentRequestItemSubmission> submissions = request.getItems().stream()
                .map(item -> new PaymentRequestItemSubmission(
                        item.getAcceptanceItem().getId(), item.getQuantity()))
                .toList();
        List<PaymentRequestItem> currentItems = buildItems(projectId, submissions, true);
        request.update(request.getPurpose(), request.getAmount(), currentItems);
        request.submit();
        return new PaymentRequestResponse(paymentRequestRepository.save(request));
    }

    @Transactional
    public PaymentRequestResponse submit(
            UUID projectId,
            UUID requesterId,
            PaymentRequestSubmission submission) {
        Project project = findProject(projectId);
        User requester = findRequester(requesterId);
        List<PaymentRequestItem> items = buildItems(projectId, submission.items(), false);
        PaymentRequest request = newRequest(project, requester, submission.purpose());
        request.update(submission.purpose().trim(), submission.amount(), items);
        request.submit();
        return new PaymentRequestResponse(paymentRequestRepository.save(request));
    }

    private Project findProject(UUID projectId) {
        return projectRepository.findById(projectId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "PROJECT_NOT_FOUND", "Không tìm thấy công trình."));
    }

    private User findRequester(UUID requesterId) {
        return userRepository.findById(requesterId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "USER_NOT_FOUND", "Không tìm thấy người gửi đề nghị."));
    }

    private PaymentRequest newRequest(Project project, User requester, String purpose) {
        String requestCode = "PAY-" + UUID.randomUUID().toString().toUpperCase();
        return new PaymentRequest(project, requestCode, purpose.trim(), BigDecimal.ZERO, requester);
    }

    private PaymentRequest findOwnedDraft(UUID projectId, UUID requestId, UUID requesterId) {
        PaymentRequest request = paymentRequestRepository
                .findByIdAndProject_IdAndRequester_Id(requestId, projectId, requesterId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "PAYMENT_REQUEST_NOT_FOUND",
                        "Không tìm thấy bản nháp đề nghị thanh toán của bạn."));
        if (request.getStatus() != PaymentRequestStatus.DRAFT) {
            throw new BadRequestException(
                    "PAYMENT_REQUEST_NOT_DRAFT",
                    "Chỉ có thể sửa hoặc gửi đề nghị đang ở trạng thái bản nháp.");
        }
        return request;
    }

    private List<PaymentRequestItem> buildItems(
            UUID projectId,
            List<PaymentRequestItemSubmission> submissions,
            boolean required) {
        if (submissions == null || submissions.isEmpty()) {
            if (required) {
                throw new BadRequestException(
                        "PAYMENT_REQUEST_ITEMS_REQUIRED",
                        "Vui lòng chọn ít nhất một hạng mục nghiệm thu đã được duyệt.");
            }
            return List.of();
        }

        Set<UUID> seenIds = new HashSet<>();
        List<PaymentRequestItem> items = new ArrayList<>();
        for (PaymentRequestItemSubmission submission : submissions) {
            if (submission == null
                    || submission.acceptanceItemId() == null
                    || submission.quantity() == null) {
                throw new BadRequestException(
                        "PAYMENT_REQUEST_ITEM_INVALID",
                        "Thông tin hạng mục hoặc khối lượng đề nghị không hợp lệ.");
            }
            if (!seenIds.add(submission.acceptanceItemId())) {
                throw new BadRequestException(
                        "PAYMENT_REQUEST_DUPLICATE_ITEM",
                        "Không thể chọn trùng hạng mục nghiệm thu.");
            }
            AcceptanceItem sourceItem = acceptanceItemRepository.findById(submission.acceptanceItemId())
                    .orElseThrow(() -> new ResourceNotFoundException(
                            "ACCEPTANCE_ITEM_NOT_FOUND",
                            "Không tìm thấy hạng mục nghiệm thu đã chọn."));
            if (sourceItem.getAcceptanceReport() == null
                    || !sourceItem.getAcceptanceReport().getProjectId().equals(projectId)
                    || !"APPROVED".equals(sourceItem.getAcceptanceReport().getStatus())) {
                throw new BadRequestException(
                        "PAYMENT_REQUEST_ITEM_NOT_APPROVED",
                        "Chỉ được đề nghị thanh toán cho hạng mục thuộc phiếu nghiệm thu đã duyệt của công trình.");
            }
            BigDecimal acceptedQuantity = sourceItem.getAcceptedQuantity();
            BigDecimal quantity = submission.quantity();
            BigDecimal unitPrice = sourceItem.getUnitPrice();
            if (quantity.signum() <= 0
                    || acceptedQuantity == null
                    || unitPrice == null
                    || unitPrice.signum() < 0
                    || quantity.compareTo(acceptedQuantity) > 0) {
                throw new BadRequestException(
                        "PAYMENT_REQUEST_INVALID_QUANTITY",
                        "Khối lượng đề nghị phải lớn hơn 0 và không vượt quá khối lượng đã nghiệm thu.");
            }
            BigDecimal amount = quantity.multiply(unitPrice).setScale(2, RoundingMode.HALF_UP);
            if (amount.precision() - amount.scale() > 17) {
                throw new BadRequestException(
                        "PAYMENT_REQUEST_AMOUNT_TOO_LARGE",
                        "Giá trị của hạng mục vượt quá giới hạn đề nghị thanh toán.");
            }
            items.add(new PaymentRequestItem(sourceItem, quantity, unitPrice, amount));
        }
        BigDecimal total = items.stream()
                .map(PaymentRequestItem::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        if (total.signum() <= 0) {
            throw new BadRequestException(
                    "PAYMENT_REQUEST_AMOUNT_REQUIRED",
                    "Tổng giá trị đề nghị thanh toán phải lớn hơn 0.");
        }
        if (total.precision() - total.scale() > 17) {
            throw new BadRequestException(
                    "PAYMENT_REQUEST_AMOUNT_TOO_LARGE",
                    "Tổng giá trị đề nghị vượt quá giới hạn cho phép.");
        }
        return items;
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