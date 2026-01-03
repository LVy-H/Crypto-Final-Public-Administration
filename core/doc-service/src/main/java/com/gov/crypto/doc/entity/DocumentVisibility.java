package com.gov.crypto.doc.entity;

/**
 * Document visibility for ABAC access control.
 * 
 * PRIVATE: Only owner and assigned countersigner can access
 * PUBLIC: All authenticated users can access (typically after approval)
 */
public enum DocumentVisibility {
    PRIVATE, // Owner + assigned countersigner only
    PUBLIC // All authenticated users
}
