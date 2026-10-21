package com.ntdhtcct;

import com.ntdhtcct.auth.service.AuthorizationService;
import com.ntdhtcct.domain.auth.AuthTokenService;
import com.ntdhtcct.service.ImageUploadService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.util.UUID;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class ImageUploadControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void uploadImageWithoutAuthenticationReturns401() throws Exception {
        UUID projectId = UUID.randomUUID();

        MockMultipartFile file = new MockMultipartFile(
                "file",
                "test.png",
                MediaType.IMAGE_PNG_VALUE,
                new byte[]{1, 2, 3}
        );

        mockMvc.perform(
                multipart("/api/projects/{projectId}/images", projectId)
                        .file(file)
        ).andExpect(status().isUnauthorized());
    }
}
