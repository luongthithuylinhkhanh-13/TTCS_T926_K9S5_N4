package com.ntdhtcct.domain.payments;

import com.ntdhtcct.common.exception.BadRequestException;
import com.ntdhtcct.domain.project.Project;
import com.ntdhtcct.domain.project.ProjectRepository;
import com.ntdhtcct.entity.User;
import com.ntdhtcct.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertThrows;
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