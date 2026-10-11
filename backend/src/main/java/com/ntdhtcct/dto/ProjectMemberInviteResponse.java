package com.ntdhtcct.dto;

import java.time.OffsetDateTime;
import java.util.UUID;

public record ProjectMemberInviteResponse(
        String status,
        String email,
        String roleCode,
        UUID userId,
        String invitationToken,
        OffsetDateTime expiresAt
) {
}
