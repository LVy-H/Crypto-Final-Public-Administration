package com.gov.crypto.doc.entity;

public enum DocumentVisibility {
    OWNER_ONLY, // Only owner can access
    ORG, // Org members can access
    ORG_AND_CHILDREN, // Org and child orgs can access
    ALL // All authenticated users
}
