package com.gov.crypto.common.util;

import org.bouncycastle.asn1.x500.X500NameBuilder;
import org.bouncycastle.asn1.x500.style.BCStyle;

/**
 * Utility class for building X.500 Distinguished Names (DN).
 * Standardizes DN creation across the platform.
 */
public final class DnUtils {

    private DnUtils() {
        // Prevent instantiation
    }

    /**
     * Builds a standard X.500 Subject DN string.
     * Uses Bouncy Castle X500NameBuilder to ensure correct formatting and escaping.
     *
     * @param serialNumber User's Serial Number (e.g., CCCD)
     * @param commonName   User's Full Name
     * @param email        User's Email
     * @param locality     Locality (District/City)
     * @param state        State (Province)
     * @param organization Organization Name (e.g., "Citizen", "Gov", or specific
     *                     Org)
     * @param country      Country Code (e.g., "VN")
     * @return Formatted DN String (e.g., "C=VN,ST=Hanoi,CN=Test User")
     */
    public static String buildSubjectDn(String serialNumber, String commonName, String email,
            String locality, String state, String organization, String country) {

        X500NameBuilder builder = new X500NameBuilder(BCStyle.INSTANCE);

        if (serialNumber != null && !serialNumber.isBlank()) {
            builder.addRDN(BCStyle.SERIALNUMBER, serialNumber);
        }
        if (commonName != null && !commonName.isBlank()) {
            builder.addRDN(BCStyle.CN, commonName);
        }
        if (email != null && !email.isBlank()) {
            builder.addRDN(BCStyle.EmailAddress, email); // OID: 1.2.840.113549.1.9.1
        }
        if (locality != null && !locality.isBlank()) {
            builder.addRDN(BCStyle.L, locality);
        }
        if (state != null && !state.isBlank()) {
            builder.addRDN(BCStyle.ST, state);
        }
        if (organization != null && !organization.isBlank()) {
            builder.addRDN(BCStyle.O, organization);
        }
        if (country != null && !country.isBlank()) {
            builder.addRDN(BCStyle.C, country);
        }

        return builder.build().toString();
    }
}
