package com.ntdhtcct.domain.payments;

import com.ntdhtcct.auth.annotation.RequireProjectRole;
import com.ntdhtcct.auth.context.UserSecurityContext;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/projects/{projectId}/payment-requests")
@RequireProjectRole({"ADMIN", "PROJECT_MANAGER"})
public class PaymentRequestController {

    private final PaymentRequestService paymentRequestService;

    public PaymentRequestController(PaymentRequestService paymentRequestService) {
        this.paymentRequestService = paymentRequestService;
    }

    @GetMapping
    public List<PaymentRequestResponse> getPendingRequests(@PathVariable UUID projectId) {
        return paymentRequestService.getPendingRequests(projectId);
    }

    @GetMapping("/my-requests")
    @RequireProjectRole({"ADMIN", "PROJECT_MANAGER", "SITE_ENGINEER", "WORKER"})
    public List<PaymentRequestResponse> getMyRequests(@PathVariable UUID projectId) {
        return paymentRequestService.getMyRequests(
                projectId, UserSecurityContext.getUserId());
    }

    @PostMapping
    @RequireProjectRole({"ADMIN", "PROJECT_MANAGER", "SITE_ENGINEER", "WORKER"})
    public ResponseEntity<PaymentRequestResponse> submit(
            @PathVariable UUID projectId,
            @Valid @RequestBody PaymentRequestSubmission submission) {
        PaymentRequestResponse response = paymentRequestService.submit(
                projectId,
                UserSecurityContext.getUserId(),
                submission);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PostMapping("/{requestId}/approve")
    public ResponseEntity<PaymentRequestResponse> approve(
            @PathVariable UUID projectId,
            @PathVariable UUID requestId) {
        return ResponseEntity.ok(paymentRequestService.approve(
                projectId, requestId, UserSecurityContext.getUserId()));
    }

    @PostMapping("/{requestId}/reject")
    public ResponseEntity<PaymentRequestResponse> reject(
            @PathVariable UUID projectId,
            @PathVariable UUID requestId,
            @Valid @RequestBody PaymentRequestDecision decision) {
        return ResponseEntity.ok(paymentRequestService.reject(
                projectId,
                requestId,
                UserSecurityContext.getUserId(),
                decision.reason()));
    }
}