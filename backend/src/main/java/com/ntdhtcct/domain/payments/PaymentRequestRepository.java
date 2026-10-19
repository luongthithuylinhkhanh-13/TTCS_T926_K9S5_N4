package com.ntdhtcct.domain.payments;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface PaymentRequestRepository extends JpaRepository<PaymentRequest, UUID> {

    @EntityGraph(attributePaths = {"requester", "items"})
    List<PaymentRequest> findByProject_IdAndStatusOrderBySubmittedAtAsc(
            UUID projectId,
            PaymentRequestStatus status);

    @EntityGraph(attributePaths = {"requester", "decidedBy", "items"})
    Optional<PaymentRequest> findByIdAndProject_Id(UUID id, UUID projectId);

    @EntityGraph(attributePaths = {"requester", "decidedBy", "items"})
    List<PaymentRequest> findByProject_IdAndRequester_IdOrderBySubmittedAtDesc(
            UUID projectId,
            UUID requesterId);

    Optional<PaymentRequest> findByIdAndProject_IdAndRequester_Id(
            UUID id,
            UUID projectId,
            UUID requesterId);
}