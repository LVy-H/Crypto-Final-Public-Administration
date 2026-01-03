package com.gov.crypto.doc.service;

import com.gov.crypto.doc.client.IdentityServiceClient;
import com.gov.crypto.doc.dto.CountersignRequest;
import com.gov.crypto.doc.dto.CreateDocumentRequest;
import com.gov.crypto.doc.dto.DocumentDto;
import com.gov.crypto.doc.entity.*;
import com.gov.crypto.doc.repository.DocumentRepository;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Workflow integration tests for DocumentService.
 * Tests the complete document approval workflow: create → sign → submit →
 * countersign → public.
 */
@ExtendWith(MockitoExtension.class)
class DocumentWorkflowTest {

    @Mock
    private DocumentRepository repository;

    @Mock
    private FileStorageService fileStorageService;

    @Mock
    private IdentityServiceClient identityClient;

    @InjectMocks
    private DocumentService documentService;

    private UUID ownerId;
    private UUID officerId;
    private UUID documentId;
    private UUID caId;

    @BeforeEach
    void setUp() {
        ownerId = UUID.randomUUID();
        officerId = UUID.randomUUID();
        documentId = UUID.randomUUID();
        caId = UUID.randomUUID();
    }

    // === Workflow State Transitions ===

    @Nested
    @DisplayName("Document Lifecycle Workflow")
    class WorkflowTests {

        @Test
        @DisplayName("New document should have DRAFT status")
        void newDocumentShouldHaveDraftStatus() {
            // Given
            CreateDocumentRequest request = new CreateDocumentRequest(
                    "test.pdf", "hash123", "application/pdf", 1024L, null,
                    DocumentClassification.INTERNAL, DocumentVisibility.PRIVATE, false);

            when(repository.save(any(Document.class))).thenAnswer(inv -> {
                Document doc = inv.getArgument(0);
                doc.setId(documentId);
                return doc;
            });

            // When
            DocumentDto result = documentService.create(request, ownerId);

            // Then
            assertEquals(ApprovalStatus.DRAFT, result.approvalStatus());
        }

        @Test
        @DisplayName("Submit for approval should change status to PENDING_COUNTERSIGN")
        void submitShouldChangeToPending() {
            // Given
            Document doc = createSignedDocument();
            when(repository.findById(documentId)).thenReturn(Optional.of(doc));
            when(repository.save(any(Document.class))).thenAnswer(inv -> inv.getArgument(0));

            // Mock officer assignment
            when(identityClient.getOfficersForCa(caId)).thenReturn(
                    List.of(new IdentityServiceClient.OfficerInfo(officerId, "officer1", "RA")));

            // When
            DocumentDto result = documentService.submitForApproval(documentId, ownerId, caId);

            // Then
            assertEquals(ApprovalStatus.PENDING_COUNTERSIGN, result.approvalStatus());
            assertNotNull(result.assignedCountersignerId());
        }

        @Test
        @DisplayName("Approval should change status to APPROVED")
        void approvalShouldChangeToApproved() {
            // Given
            Document doc = createPendingDocument();
            doc.setAssignedCountersignerId(officerId);
            UUID countersignId = UUID.randomUUID();

            when(repository.findById(documentId)).thenReturn(Optional.of(doc));
            when(repository.save(any(Document.class))).thenAnswer(inv -> inv.getArgument(0));

            CountersignRequest request = new CountersignRequest(countersignId, true, null);

            // When
            DocumentDto result = documentService.saveCountersign(documentId, officerId, request);

            // Then
            assertEquals(ApprovalStatus.APPROVED, result.approvalStatus());
            assertNotNull(result.approvedAt());
            assertEquals(countersignId, result.countersignatureId());
        }

        @Test
        @DisplayName("Rejection should change status to REJECTED with reason")
        void rejectionShouldChangeToRejected() {
            // Given
            Document doc = createPendingDocument();
            doc.setAssignedCountersignerId(officerId);
            String reason = "Missing required attachments";

            when(repository.findById(documentId)).thenReturn(Optional.of(doc));
            when(repository.save(any(Document.class))).thenAnswer(inv -> inv.getArgument(0));

            CountersignRequest request = new CountersignRequest(null, false, reason);

            // When
            DocumentDto result = documentService.saveCountersign(documentId, officerId, request);

            // Then
            assertEquals(ApprovalStatus.REJECTED, result.approvalStatus());
            assertEquals(reason, result.rejectionReason());
        }

        @Test
        @DisplayName("Make public should change visibility after approval")
        void makePublicShouldChangeVisibilityAfterApproval() {
            // Given
            Document doc = createApprovedDocument();
            when(repository.findById(documentId)).thenReturn(Optional.of(doc));
            when(repository.save(any(Document.class))).thenAnswer(inv -> inv.getArgument(0));

            // When
            DocumentDto result = documentService.makePublic(documentId, ownerId);

            // Then
            assertEquals(DocumentVisibility.PUBLIC, result.visibility());
        }
    }

    // === Security Constraint Tests ===

    @Nested
    @DisplayName("Workflow Security Constraints")
    class SecurityConstraints {

        @Test
        @DisplayName("Only owner can submit for approval")
        void onlyOwnerCanSubmitForApproval() {
            // Given
            Document doc = createSignedDocument();
            UUID wrongUserId = UUID.randomUUID();
            when(repository.findById(documentId)).thenReturn(Optional.of(doc));

            // When/Then
            assertThrows(IllegalArgumentException.class,
                    () -> documentService.submitForApproval(documentId, wrongUserId, caId),
                    "Only document owner can submit for approval");
        }

        @Test
        @DisplayName("Document must be signed before submission")
        void documentMustBeSignedBeforeSubmission() {
            // Given
            Document doc = createUnsignedDocument();
            when(repository.findById(documentId)).thenReturn(Optional.of(doc));

            // When/Then
            assertThrows(IllegalStateException.class,
                    () -> documentService.submitForApproval(documentId, ownerId, caId),
                    "Document must be signed before submitting for approval");
        }

        @Test
        @DisplayName("Cannot submit already pending document")
        void cannotSubmitAlreadyPendingDocument() {
            // Given
            Document doc = createPendingDocument();
            when(repository.findById(documentId)).thenReturn(Optional.of(doc));

            // When/Then
            assertThrows(IllegalStateException.class,
                    () -> documentService.submitForApproval(documentId, ownerId, caId),
                    "Document already submitted for approval");
        }

        @Test
        @DisplayName("Only assigned officer can countersign")
        void onlyAssignedOfficerCanCountersign() {
            // Given
            Document doc = createPendingDocument();
            doc.setAssignedCountersignerId(officerId);
            UUID wrongOfficerId = UUID.randomUUID();

            when(repository.findById(documentId)).thenReturn(Optional.of(doc));

            CountersignRequest request = new CountersignRequest(UUID.randomUUID(), true, null);

            // When/Then
            assertThrows(IllegalArgumentException.class,
                    () -> documentService.saveCountersign(documentId, wrongOfficerId, request),
                    "Only assigned officer can countersign this document");
        }

        @Test
        @DisplayName("Only owner can make document public")
        void onlyOwnerCanMakePublic() {
            // Given
            Document doc = createApprovedDocument();
            UUID wrongUserId = UUID.randomUUID();
            when(repository.findById(documentId)).thenReturn(Optional.of(doc));

            // When/Then
            assertThrows(IllegalArgumentException.class, () -> documentService.makePublic(documentId, wrongUserId),
                    "Only document owner can change visibility");
        }

        @Test
        @DisplayName("Cannot make unapproved document public")
        void cannotMakeUnapprovedDocumentPublic() {
            // Given
            Document doc = createPendingDocument();
            when(repository.findById(documentId)).thenReturn(Optional.of(doc));

            // When/Then
            assertThrows(IllegalStateException.class, () -> documentService.makePublic(documentId, ownerId),
                    "Document must be approved before making public");
        }
    }

    // === Helper Methods ===

    private Document createUnsignedDocument() {
        Document doc = new Document();
        doc.setId(documentId);
        doc.setOwnerId(ownerId);
        doc.setApprovalStatus(ApprovalStatus.DRAFT);
        doc.setVisibility(DocumentVisibility.PRIVATE);
        return doc;
    }

    private Document createSignedDocument() {
        Document doc = createUnsignedDocument();
        doc.setSignatureBase64("signature-data");
        return doc;
    }

    private Document createPendingDocument() {
        Document doc = createSignedDocument();
        doc.setApprovalStatus(ApprovalStatus.PENDING_COUNTERSIGN);
        return doc;
    }

    private Document createApprovedDocument() {
        Document doc = createSignedDocument();
        doc.setApprovalStatus(ApprovalStatus.APPROVED);
        return doc;
    }
}
