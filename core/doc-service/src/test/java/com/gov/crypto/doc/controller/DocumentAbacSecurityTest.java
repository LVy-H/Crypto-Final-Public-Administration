package com.gov.crypto.doc.controller;

import com.gov.crypto.doc.dto.DocumentDto;
import com.gov.crypto.doc.entity.*;
import com.gov.crypto.doc.service.DocumentService;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.UUID;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * ABAC (Attribute-Based Access Control) security tests for DocumentController.
 * Verifies that access control is properly enforced based on document
 * visibility
 * and user attributes.
 */
@WebMvcTest(DocumentController.class)
class DocumentAbacSecurityTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private DocumentService documentService;

    private UUID ownerId;
    private UUID assignedOfficerId;
    private UUID randomUserId;
    private UUID documentId;

    @BeforeEach
    void setUp() {
        ownerId = UUID.randomUUID();
        assignedOfficerId = UUID.randomUUID();
        randomUserId = UUID.randomUUID();
        documentId = UUID.randomUUID();
    }

    // === Helper Methods ===

    private Document createPrivateDocument() {
        Document doc = new Document();
        doc.setId(documentId);
        doc.setName("private-document.pdf");
        doc.setOwnerId(ownerId);
        doc.setVisibility(DocumentVisibility.PRIVATE);
        doc.setClassification(DocumentClassification.CONFIDENTIAL);
        doc.setAssignedCountersignerId(assignedOfficerId);
        doc.setApprovalStatus(ApprovalStatus.PENDING_COUNTERSIGN);
        return doc;
    }

    private Document createPublicDocument() {
        Document doc = new Document();
        doc.setId(documentId);
        doc.setName("public-document.pdf");
        doc.setOwnerId(ownerId);
        doc.setVisibility(DocumentVisibility.PUBLIC);
        doc.setClassification(DocumentClassification.PUBLIC);
        doc.setApprovalStatus(ApprovalStatus.APPROVED);
        return doc;
    }

    private DocumentDto toDto(Document doc) {
        return new DocumentDto(
                doc.getId(), doc.getName(), doc.getOwnerId(), doc.getOrgId(),
                doc.getClassification(), doc.getVisibility(), doc.getContentHash(),
                doc.getContentType(), doc.getFileSize(), doc.getSignatureId(),
                false, null, false, null,
                doc.getAssignedCountersignerId(), doc.getCountersignatureId(),
                doc.getApprovalStatus(), doc.getApprovedAt(), null,
                doc.getSignedAt(), doc.getCreatedAt());
    }

    // === PRIVATE Document Access Tests ===

    @Nested
    @DisplayName("PRIVATE Document Access Control")
    class PrivateDocumentAccess {

        @Test
        @DisplayName("Owner should access their PRIVATE document")
        void ownerShouldAccessPrivateDocument() throws Exception {
            Document doc = createPrivateDocument();
            when(documentService.getEntityById(documentId)).thenReturn(doc);
            when(documentService.toDto(doc)).thenReturn(toDto(doc));

            mockMvc.perform(get("/api/v1/doc/{id}", documentId)
                    .header("X-User-Id", ownerId.toString()))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.id").value(documentId.toString()));
        }

        @Test
        @DisplayName("Assigned officer should access PRIVATE document")
        void assignedOfficerShouldAccessPrivateDocument() throws Exception {
            Document doc = createPrivateDocument();
            when(documentService.getEntityById(documentId)).thenReturn(doc);
            when(documentService.toDto(doc)).thenReturn(toDto(doc));

            mockMvc.perform(get("/api/v1/doc/{id}", documentId)
                    .header("X-User-Id", assignedOfficerId.toString()))
                    .andExpect(status().isOk());
        }

        @Test
        @DisplayName("Random user should NOT access PRIVATE document - FORBIDDEN")
        void randomUserShouldNotAccessPrivateDocument() throws Exception {
            Document doc = createPrivateDocument();
            when(documentService.getEntityById(documentId)).thenReturn(doc);

            mockMvc.perform(get("/api/v1/doc/{id}", documentId)
                    .header("X-User-Id", randomUserId.toString()))
                    .andExpect(status().isForbidden());
        }

        @Test
        @DisplayName("Unassigned officer should NOT access PRIVATE document")
        void unassignedOfficerShouldNotAccessPrivateDocument() throws Exception {
            Document doc = createPrivateDocument();
            UUID wrongOfficerId = UUID.randomUUID();
            when(documentService.getEntityById(documentId)).thenReturn(doc);

            mockMvc.perform(get("/api/v1/doc/{id}", documentId)
                    .header("X-User-Id", wrongOfficerId.toString()))
                    .andExpect(status().isForbidden());
        }
    }

    // === PUBLIC Document Access Tests ===

    @Nested
    @DisplayName("PUBLIC Document Access Control")
    class PublicDocumentAccess {

        @Test
        @DisplayName("Any authenticated user should access PUBLIC document")
        void anyUserShouldAccessPublicDocument() throws Exception {
            Document doc = createPublicDocument();
            when(documentService.getEntityById(documentId)).thenReturn(doc);
            when(documentService.toDto(doc)).thenReturn(toDto(doc));

            mockMvc.perform(get("/api/v1/doc/{id}", documentId)
                    .header("X-User-Id", randomUserId.toString()))
                    .andExpect(status().isOk());
        }
    }

    // === Countersign ABAC Tests ===

    @Nested
    @DisplayName("Countersign Access Control")
    class CountersignAccess {

        @Test
        @DisplayName("Assigned officer CAN countersign document")
        void assignedOfficerCanCountersign() throws Exception {
            Document doc = createPrivateDocument();
            DocumentDto dto = toDto(doc);

            when(documentService.saveCountersign(eq(documentId), eq(assignedOfficerId), any()))
                    .thenReturn(dto);

            mockMvc.perform(post("/api/v1/doc/{id}/countersign", documentId)
                    .header("X-User-Id", assignedOfficerId.toString())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("{\"countersignatureId\":\"" + UUID.randomUUID() +
                            "\",\"approved\":true}"))
                    .andExpect(status().isOk());
        }

        @Test
        @DisplayName("Wrong officer should NOT countersign - service throws")
        void wrongOfficerCannotCountersign() throws Exception {
            UUID wrongOfficerId = UUID.randomUUID();

            when(documentService.saveCountersign(eq(documentId), eq(wrongOfficerId), any()))
                    .thenThrow(new IllegalArgumentException(
                            "Only assigned officer can countersign this document"));

            mockMvc.perform(post("/api/v1/doc/{id}/countersign", documentId)
                    .header("X-User-Id", wrongOfficerId.toString())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("{\"countersignatureId\":\"" + UUID.randomUUID() +
                            "\",\"approved\":true}"))
                    .andExpect(status().isBadRequest());
        }
    }

    // === Make Public ABAC Tests ===

    @Nested
    @DisplayName("Make Public Access Control")
    class MakePublicAccess {

        @Test
        @DisplayName("Owner CAN make approved document public")
        void ownerCanMakeApprovedDocumentPublic() throws Exception {
            Document doc = createPrivateDocument();
            doc.setApprovalStatus(ApprovalStatus.APPROVED);
            DocumentDto dto = toDto(doc);
            dto = new DocumentDto(dto.id(), dto.name(), dto.ownerId(), dto.orgId(),
                    dto.classification(), DocumentVisibility.PUBLIC, dto.contentHash(),
                    dto.contentType(), dto.fileSize(), dto.signatureId(), dto.signed(),
                    dto.signatureAlgorithm(), dto.encrypted(), dto.encryptionAlgorithm(),
                    dto.assignedCountersignerId(), dto.countersignatureId(),
                    dto.approvalStatus(), dto.approvedAt(), dto.rejectionReason(),
                    dto.signedAt(), dto.createdAt());

            when(documentService.makePublic(documentId, ownerId)).thenReturn(dto);

            mockMvc.perform(post("/api/v1/doc/{id}/make-public", documentId)
                    .header("X-User-Id", ownerId.toString()))
                    .andExpect(status().isOk());
        }

        @Test
        @DisplayName("Non-owner should NOT make document public")
        void nonOwnerCannotMakeDocumentPublic() throws Exception {
            when(documentService.makePublic(documentId, randomUserId))
                    .thenThrow(new IllegalArgumentException(
                            "Only document owner can change visibility"));

            mockMvc.perform(post("/api/v1/doc/{id}/make-public", documentId)
                    .header("X-User-Id", randomUserId.toString()))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("Should NOT make unapproved document public")
        void cannotMakeUnapprovedDocumentPublic() throws Exception {
            when(documentService.makePublic(documentId, ownerId))
                    .thenThrow(new IllegalStateException(
                            "Document must be approved before making public"));

            mockMvc.perform(post("/api/v1/doc/{id}/make-public", documentId)
                    .header("X-User-Id", ownerId.toString()))
                    .andExpect(status().isBadRequest());
        }
    }

    // === Download ABAC Tests ===

    @Nested
    @DisplayName("Download Access Control")
    class DownloadAccess {

        @Test
        @DisplayName("Owner CAN download their PRIVATE document")
        void ownerCanDownloadPrivateDocument() throws Exception {
            Document doc = createPrivateDocument();
            when(documentService.getEntityById(documentId)).thenReturn(doc);
            when(documentService.downloadContent(documentId)).thenReturn("content".getBytes());

            mockMvc.perform(get("/api/v1/doc/{id}/download", documentId)
                    .header("X-User-Id", ownerId.toString()))
                    .andExpect(status().isOk());
        }

        @Test
        @DisplayName("Random user should NOT download PRIVATE document")
        void randomUserCannotDownloadPrivateDocument() throws Exception {
            Document doc = createPrivateDocument();
            when(documentService.getEntityById(documentId)).thenReturn(doc);

            mockMvc.perform(get("/api/v1/doc/{id}/download", documentId)
                    .header("X-User-Id", randomUserId.toString()))
                    .andExpect(status().isForbidden());
        }
    }
}
