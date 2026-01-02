package com.gov.crypto.common.x509;

import org.bouncycastle.asn1.x509.AccessDescription;
import org.bouncycastle.asn1.x509.AuthorityInformationAccess;
import org.bouncycastle.asn1.x509.BasicConstraints;
import org.bouncycastle.asn1.x509.CRLDistPoint;
import org.bouncycastle.asn1.x509.DistributionPoint;
import org.bouncycastle.asn1.x509.DistributionPointName;
import org.bouncycastle.asn1.x509.Extension;
import org.bouncycastle.asn1.x509.GeneralName;
import org.bouncycastle.asn1.x509.GeneralNames;
import org.bouncycastle.asn1.x509.KeyUsage;
import org.bouncycastle.cert.CertIOException;
import org.bouncycastle.cert.X509v3CertificateBuilder;

/**
 * Utility class for adding RFC 5280 compliant extensions to X.509 certificates.
 */
public final class X509ExtensionUtils {

    private X509ExtensionUtils() {
        // Prevent instantiation
    }

    /**
     * Adds BasicConstraints extension.
     * 
     * @param builder The certificate builder
     * @param isCa    True if the subject is a CA
     * @throws CertIOException If extension cannot be added
     */
    public static void addBasicConstraints(X509v3CertificateBuilder builder, boolean isCa) throws CertIOException {
        if (isCa) {
            // Default to unlimited path length for CA
            builder.addExtension(Extension.basicConstraints, true, new BasicConstraints(true));
        } else {
            builder.addExtension(Extension.basicConstraints, true, new BasicConstraints(false));
        }
    }

    /**
     * Adds BasicConstraints extension with specific path length constraint.
     * Implies CA=true.
     * 
     * @param builder           The certificate builder
     * @param pathLenConstraint The maximum number of non-self-issued intermediate
     *                          certificates that may follow this certificate in a
     *                          valid certification path.
     * @throws CertIOException If extension cannot be added
     */
    public static void addBasicConstraints(X509v3CertificateBuilder builder, int pathLenConstraint)
            throws CertIOException {
        builder.addExtension(Extension.basicConstraints, true, new BasicConstraints(pathLenConstraint));
    }

    /**
     * Adds KeyUsage extension.
     * 
     * @param builder     The certificate builder
     * @param usageBitmap Bitwise OR of KeyUsage constants (e.g.
     *                    KeyUsage.digitalSignature)
     * @throws CertIOException If extension cannot be added
     */
    public static void addKeyUsage(X509v3CertificateBuilder builder, int usageBitmap) throws CertIOException {
        builder.addExtension(Extension.keyUsage, true, new KeyUsage(usageBitmap));
    }

    /**
     * Adds CRL Distribution Point extension.
     * 
     * @param builder The certificate builder
     * @param crlUrl  The HTTP URL to the CRL
     * @throws CertIOException If extension cannot be added
     */
    public static void addCrlDistributionPoint(X509v3CertificateBuilder builder, String crlUrl) throws CertIOException {
        if (crlUrl == null || crlUrl.isBlank())
            return;

        GeneralName gn = new GeneralName(GeneralName.uniformResourceIdentifier, crlUrl);
        DistributionPointName dpn = new DistributionPointName(new GeneralNames(gn));
        DistributionPoint dp = new DistributionPoint(dpn, null, null);

        builder.addExtension(Extension.cRLDistributionPoints, false, new CRLDistPoint(new DistributionPoint[] { dp }));
    }

    /**
     * Adds Authority Information Access extension (for CA Issuers URI).
     * 
     * @param builder      The certificate builder
     * @param caIssuersUrl The HTTP URL to the issuer's certificate
     * @throws CertIOException If extension cannot be added
     */
    public static void addAuthorityInfoAccess(X509v3CertificateBuilder builder, String caIssuersUrl)
            throws CertIOException {
        if (caIssuersUrl == null || caIssuersUrl.isBlank())
            return;

        GeneralName gn = new GeneralName(GeneralName.uniformResourceIdentifier, caIssuersUrl);
        AccessDescription ad = new AccessDescription(AccessDescription.id_ad_caIssuers, gn);

        builder.addExtension(Extension.authorityInfoAccess, false, new AuthorityInformationAccess(ad));
    }
}
