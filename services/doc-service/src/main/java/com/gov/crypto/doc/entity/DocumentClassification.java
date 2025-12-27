package com.gov.crypto.doc.entity;

public enum DocumentClassification {
    PUBLIC, // Laws, announcements, regulations
    PERSONAL, // Citizen's private signed docs
    INTERNAL, // Org-specific documents
    RESTRICTED, // Requires explicit grant
    CONFIDENTIAL // Sensitive documents
}
