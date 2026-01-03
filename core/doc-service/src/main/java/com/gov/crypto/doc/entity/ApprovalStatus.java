package com.gov.crypto.doc.entity;

/**
 * Approval status for document countersign workflow.
 */
public enum ApprovalStatus {
    DRAFT, // Not submitted for approval
    PENDING_COUNTERSIGN, // Awaiting officer approval
    APPROVED, // Countersigned by officer
    REJECTED // Officer rejected
}
