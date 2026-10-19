package com.ntdhtcct.domain.payments;

import com.ntdhtcct.common.exception.BadRequestException;
import com.ntdhtcct.domain.acceptance.AcceptanceItem;
import com.ntdhtcct.domain.acceptance.AcceptanceItemRepository;
import com.ntdhtcct.domain.acceptance.AcceptanceReport;
import com.ntdhtcct.domain.project.Project;
import com.ntdhtcct.domain.project.ProjectRepository;
import com.ntdhtcct.entity.User;
import com.ntdhtcct.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;
import java.util.UUID;
import java.util.List;
import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PaymentRequestServiceTest {

    @Mock
    private PaymentRequestRepository paymentRequestRepository;

    @Mock
    private ProjectRepository projectRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private AcceptanceItemRepository acceptanceItemRepository;

    @InjectMocks
    private PaymentRequestService paymentRequestService;

    @Test
    void submitsRequestAsPendingForAuthenticatedUser() {
        UUID projectId = UUID.randomUUID();
        UUID requesterId = UUID.randomUUID();
        Project project = org.mockito.Mockito.mock(Project.class);
        User requester = org.mockito.Mockito.mock(User.class);
        when(projectRepository.findById(projectId)).thenReturn(Optional.of(project));
        when(userRepository.findById(requesterId)).thenReturn(Optional.of(requester));
        when(paymentRequestRepository.save(org.mockito.ArgumentMatchers.any(PaymentRequest.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        paymentRequestService.submit(
                projectId,
                requesterId,
                new PaymentRequestSubmission("  Tạm ứng vật tư  ", new java.math.BigDecimal("125000.50")));

        org.mockito.ArgumentCaptor<PaymentRequest> requestCaptor =
                org.mockito.ArgumentCaptor.forClass(PaymentRequest.class);
        verify(paymentRequestRepository).save(requestCaptor.capture());
        PaymentRequest savedRequest = requestCaptor.getValue();
        org.junit.jupiter.api.Assertions.assertEquals(project, savedRequest.getProject());
        org.junit.jupiter.api.Assertions.assertEquals(requester, savedRequest.getRequester());
        org.junit.jupiter.api.Assertions.assertEquals("Tạm ứng vật tư", savedRequest.getPurpose());
        org.junit.jupiter.api.Assertions.assertEquals(PaymentRequestStatus.PENDING, savedRequest.getStatus());
    }

    @Test
    void createsDraftUsingApprovedAcceptanceItemAndRecalculatesAmount() {
        UUID projectId = UUID.randomUUID();
        UUID requesterId = UUID.randomUUID();
        UUID acceptanceItemId = UUID.randomUUID();
        Project project = mock(Project.class);
        User requester = mock(User.class);
        AcceptanceReport report = mock(AcceptanceReport.class);
        AcceptanceItem acceptanceItem = mock(AcceptanceItem.class);
        when(projectRepository.findById(projectId)).thenReturn(Optional.of(project));
        when(userRepository.findById(requesterId)).thenReturn(Optional.of(requester));
        when(acceptanceItemRepository.findById(acceptanceItemId)).thenReturn(Optional.of(acceptanceItem));
        when(acceptanceItem.getAcceptanceReport()).thenReturn(report);
        when(report.getProjectId()).thenReturn(projectId);
        when(report.getStatus()).thenReturn("APPROVED");
        when(acceptanceItem.getAcceptedQuantity()).thenReturn(new BigDecimal("8"));
        when(acceptanceItem.getUnitPrice()).thenReturn(new BigDecimal("125.50"));
        when(acceptanceItem.getId()).thenReturn(acceptanceItemId);
        when(acceptanceItem.getItemName()).thenReturn("Bê tông");
        when(acceptanceItem.getUnit()).thenReturn("m3");
        when(paymentRequestRepository.save(any(PaymentRequest.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        PaymentRequestResponse response = paymentRequestService.createDraft(
                projectId,
                requesterId,
                new PaymentRequestSubmission(
                        "  Thanh toán khối lượng  ",
                        new BigDecimal("1.00"),
                        List.of(new PaymentRequestItemSubmission(
                                acceptanceItemId, new BigDecimal("2.50")))));

        ArgumentCaptor<PaymentRequest> requestCaptor = ArgumentCaptor.forClass(PaymentRequest.class);
        verify(paymentRequestRepository).save(requestCaptor.capture());
        PaymentRequest savedRequest = requestCaptor.getValue();
        assertEquals(PaymentRequestStatus.DRAFT, savedRequest.getStatus());
        assertEquals("Thanh toán khối lượng", savedRequest.getPurpose());
        assertEquals(new BigDecimal("313.75"), savedRequest.getAmount());
        assertEquals(new BigDecimal("313.75"), response.amount());
        assertEquals(1, response.items().size());
    }

    @Test
    void rejectsAcceptanceItemsFromAnotherProject() {
        UUID projectId = UUID.randomUUID();
        UUID acceptanceItemId = UUID.randomUUID();
        Project project = mock(Project.class);
        User requester = mock(User.class);
        AcceptanceReport report = mock(AcceptanceReport.class);
        AcceptanceItem acceptanceItem = mock(AcceptanceItem.class);
        when(projectRepository.findById(projectId)).thenReturn(Optional.of(project));
        when(userRepository.findById(any())).thenReturn(Optional.of(requester));
        when(acceptanceItemRepository.findById(acceptanceItemId)).thenReturn(Optional.of(acceptanceItem));
        when(acceptanceItem.getAcceptanceReport()).thenReturn(report);
        when(report.getProjectId()).thenReturn(UUID.randomUUID());

        assertThrows(BadRequestException.class, () -> paymentRequestService.createDraft(
                projectId,
                UUID.randomUUID(),
                new PaymentRequestSubmission(
                        "Thanh toán",
                        BigDecimal.ONE,
                        List.of(new PaymentRequestItemSubmission(acceptanceItemId, BigDecimal.ONE)))));

        verify(paymentRequestRepository, never()).save(any());
    }

    @Test
    void submittingDraftRevalidatesItemsAndTransitionsToPending() {
        UUID projectId = UUID.randomUUID();
        UUID requesterId = UUID.randomUUID();
        UUID requestId = UUID.randomUUID();
        UUID acceptanceItemId = UUID.randomUUID();
        Project project = mock(Project.class);
        User requester = mock(User.class);
        AcceptanceReport report = mock(AcceptanceReport.class);
        AcceptanceItem acceptanceItem = mock(AcceptanceItem.class);
        PaymentRequest request = new PaymentRequest(
                project, "PAY-TEST", "Thanh toán", BigDecimal.ONE, requester);
        request.replaceItems(List.of(new PaymentRequestItem(
                acceptanceItem,
                new BigDecimal("2"),
                new BigDecimal("50"),
                new BigDecimal("100"))));
        when(paymentRequestRepository.findByIdAndProject_IdAndRequester_Id(
                requestId, projectId, requesterId)).thenReturn(Optional.of(request));
        when(paymentRequestRepository.save(request)).thenReturn(request);
        when(project.getId()).thenReturn(projectId);
        when(acceptanceItemRepository.findById(acceptanceItemId)).thenReturn(Optional.of(acceptanceItem));
        when(acceptanceItem.getId()).thenReturn(acceptanceItemId);
        when(acceptanceItem.getAcceptanceReport()).thenReturn(report);
        when(report.getProjectId()).thenReturn(projectId);
        when(report.getStatus()).thenReturn("APPROVED");
        when(acceptanceItem.getAcceptedQuantity()).thenReturn(new BigDecimal("4"));
        when(acceptanceItem.getUnitPrice()).thenReturn(new BigDecimal("75"));
        when(acceptanceItem.getItemName()).thenReturn("Thép");
        when(acceptanceItem.getUnit()).thenReturn("kg");

        PaymentRequestResponse response = paymentRequestService.submitDraft(
                projectId, requestId, requesterId);

        assertEquals(PaymentRequestStatus.PENDING, response.status());
        assertEquals(new BigDecimal("150.00"), response.amount());
        verify(paymentRequestRepository).save(request);
    }

    @Test
    void approvesPendingRequestAndRecordsApprover() {
        UUID projectId = UUID.randomUUID();
        UUID requestId = UUID.randomUUID();
        UUID approverId = UUID.randomUUID();
        PaymentRequest request = org.mockito.Mockito.mock(PaymentRequest.class);
        Project project = org.mockito.Mockito.mock(Project.class);
        User approver = org.mockito.Mockito.mock(User.class);
        User requester = org.mockito.Mockito.mock(User.class);

        when(paymentRequestRepository.findByIdAndProject_Id(requestId, projectId))
                .thenReturn(Optional.of(request));
        when(request.getStatus()).thenReturn(PaymentRequestStatus.PENDING);
        when(userRepository.findById(approverId)).thenReturn(Optional.of(approver));
        when(request.getProject()).thenReturn(project);
        when(project.getId()).thenReturn(projectId);
        when(request.getRequester()).thenReturn(requester);
        when(paymentRequestRepository.save(request)).thenReturn(request);

        paymentRequestService.approve(projectId, requestId, approverId);

        verify(request).decide(PaymentRequestStatus.APPROVED, null, approver);
        verify(paymentRequestRepository).save(request);
    }

    @Test
    void rejectsPendingRequestWithTrimmedReason() {
        UUID projectId = UUID.randomUUID();
        UUID requestId = UUID.randomUUID();
        UUID approverId = UUID.randomUUID();
        PaymentRequest request = org.mockito.Mockito.mock(PaymentRequest.class);
        Project project = org.mockito.Mockito.mock(Project.class);
        User approver = org.mockito.Mockito.mock(User.class);
        User requester = org.mockito.Mockito.mock(User.class);

        when(paymentRequestRepository.findByIdAndProject_Id(requestId, projectId))
                .thenReturn(Optional.of(request));
        when(request.getStatus()).thenReturn(PaymentRequestStatus.PENDING);
        when(userRepository.findById(approverId)).thenReturn(Optional.of(approver));
        when(request.getProject()).thenReturn(project);
        when(project.getId()).thenReturn(projectId);
        when(request.getRequester()).thenReturn(requester);
        when(paymentRequestRepository.save(request)).thenReturn(request);

        paymentRequestService.reject(projectId, requestId, approverId, "  Thiếu hóa đơn  ");

        verify(request).decide(PaymentRequestStatus.REJECTED, "Thiếu hóa đơn", approver);
        verify(paymentRequestRepository).save(request);
    }

    @Test
    void rejectsBlankReasonWithoutLoadingRequest() {
        assertThrows(BadRequestException.class, () -> paymentRequestService.reject(
                UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(), "  "));

        verify(paymentRequestRepository, never()).findByIdAndProject_Id(
                org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.any());
    }

    @Test
    void doesNotAllowDecidingARequestMoreThanOnce() {
        UUID projectId = UUID.randomUUID();
        UUID requestId = UUID.randomUUID();
        PaymentRequest request = org.mockito.Mockito.mock(PaymentRequest.class);
        when(paymentRequestRepository.findByIdAndProject_Id(requestId, projectId))
                .thenReturn(Optional.of(request));
        when(request.getStatus()).thenReturn(PaymentRequestStatus.APPROVED);

        assertThrows(BadRequestException.class, () -> paymentRequestService.approve(
                projectId, requestId, UUID.randomUUID()));

        verify(userRepository, never()).findById(org.mockito.ArgumentMatchers.any());
        verify(paymentRequestRepository, never()).save(request);
    }
}