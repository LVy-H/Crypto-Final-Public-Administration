import { ref } from 'vue';
import { api } from '@/services/api';
const file = ref(null);
const loading = ref(false);
const verificationResult = ref(null);
const showChain = ref(true);
const errorMessage = ref('');
async function handleVerify() {
    if (!file.value)
        return;
    loading.value = true;
    verificationResult.value = null;
    errorMessage.value = '';
    try {
        const result = await api.verifyAsic(file.value);
        if (result.success && result.data) {
            const data = result.data;
            // Transform API response to match existing UI structure
            verificationResult.value = {
                isValid: data.valid,
                verifiedAt: new Date().toLocaleString('vi-VN'),
                message: data.errorMessage,
                details: {
                    hash: 'SHA-256 verified',
                    documentName: data.documentName,
                    documentSize: data.documentSize,
                    chainStatus: data.valid ? `Verified (${data.signatureCount} signatures)` : 'Verification Failed',
                    signatures: data.signatures.map((sig, index) => ({
                        signer: sig.signerName || 'Unknown',
                        algorithm: 'ML-DSA (PQC)',
                        timestamp: sig.timestamp || new Date().toISOString(),
                        valid: sig.valid,
                        message: sig.message,
                        certificateSubject: sig.certificateSubject,
                        certificateIssuer: sig.certificateIssuer,
                        validFrom: sig.certificateNotBefore,
                        validTo: sig.certificateNotAfter
                    }))
                }
            };
        }
        else {
            errorMessage.value = result.error || 'Verification failed';
            verificationResult.value = {
                isValid: false,
                message: result.error || 'Verification request failed'
            };
        }
    }
    catch (e) {
        errorMessage.value = String(e);
        verificationResult.value = {
            isValid: false,
            message: String(e)
        };
    }
    finally {
        loading.value = false;
    }
}
const __VLS_ctx = {
    ...{},
    ...{},
};
let __VLS_components;
let __VLS_intrinsics;
let __VLS_directives;
/** @type {__VLS_StyleScopedClasses['col']} */ ;
/** @type {__VLS_StyleScopedClasses['btn-verify']} */ ;
/** @type {__VLS_StyleScopedClasses['result-box']} */ ;
/** @type {__VLS_StyleScopedClasses['result-box']} */ ;
/** @type {__VLS_StyleScopedClasses['section-title']} */ ;
/** @type {__VLS_StyleScopedClasses['chain-item']} */ ;
/** @type {__VLS_StyleScopedClasses['signatures-list']} */ ;
/** @type {__VLS_StyleScopedClasses['badge']} */ ;
__VLS_asFunctionalElement1(__VLS_intrinsics.div, __VLS_intrinsics.div)({
    ...{ class: "page-container" },
});
/** @type {__VLS_StyleScopedClasses['page-container']} */ ;
__VLS_asFunctionalElement1(__VLS_intrinsics.div, __VLS_intrinsics.div)({
    ...{ class: "panel" },
});
/** @type {__VLS_StyleScopedClasses['panel']} */ ;
__VLS_asFunctionalElement1(__VLS_intrinsics.h2, __VLS_intrinsics.h2)({});
__VLS_asFunctionalElement1(__VLS_intrinsics.div, __VLS_intrinsics.div)({
    ...{ class: "upload-section" },
});
/** @type {__VLS_StyleScopedClasses['upload-section']} */ ;
__VLS_asFunctionalElement1(__VLS_intrinsics.label, __VLS_intrinsics.label)({});
__VLS_asFunctionalElement1(__VLS_intrinsics.input)({
    ...{ onChange: (e => __VLS_ctx.file = e.target.files?.[0] || null) },
    type: "file",
    accept: ".asic,.zip",
});
__VLS_asFunctionalElement1(__VLS_intrinsics.button, __VLS_intrinsics.button)({
    ...{ onClick: (__VLS_ctx.handleVerify) },
    disabled: (__VLS_ctx.loading || !__VLS_ctx.file),
    ...{ class: "btn-verify" },
});
/** @type {__VLS_StyleScopedClasses['btn-verify']} */ ;
(__VLS_ctx.loading ? 'Đang kiểm tra...' : 'Xác thực ngay');
if (__VLS_ctx.errorMessage) {
    __VLS_asFunctionalElement1(__VLS_intrinsics.div, __VLS_intrinsics.div)({
        ...{ class: "error-message" },
    });
    /** @type {__VLS_StyleScopedClasses['error-message']} */ ;
    (__VLS_ctx.errorMessage);
}
if (__VLS_ctx.verificationResult) {
    __VLS_asFunctionalElement1(__VLS_intrinsics.div, __VLS_intrinsics.div)({
        ...{ class: "result-box" },
        ...{ class: ({ valid: __VLS_ctx.verificationResult.isValid, invalid: !__VLS_ctx.verificationResult.isValid }) },
    });
    /** @type {__VLS_StyleScopedClasses['result-box']} */ ;
    /** @type {__VLS_StyleScopedClasses['valid']} */ ;
    /** @type {__VLS_StyleScopedClasses['invalid']} */ ;
    __VLS_asFunctionalElement1(__VLS_intrinsics.div, __VLS_intrinsics.div)({
        ...{ class: "result-header" },
    });
    /** @type {__VLS_StyleScopedClasses['result-header']} */ ;
    if (__VLS_ctx.verificationResult.isValid) {
        __VLS_asFunctionalElement1(__VLS_intrinsics.h3, __VLS_intrinsics.h3)({});
    }
    else {
        __VLS_asFunctionalElement1(__VLS_intrinsics.h3, __VLS_intrinsics.h3)({});
    }
    if (__VLS_ctx.verificationResult.isValid) {
        __VLS_asFunctionalElement1(__VLS_intrinsics.p, __VLS_intrinsics.p)({
            ...{ class: "verify-time" },
        });
        /** @type {__VLS_StyleScopedClasses['verify-time']} */ ;
        (__VLS_ctx.verificationResult.verifiedAt);
    }
    if (__VLS_ctx.verificationResult.isValid && __VLS_ctx.verificationResult.details) {
        __VLS_asFunctionalElement1(__VLS_intrinsics.div, __VLS_intrinsics.div)({
            ...{ class: "result-details" },
        });
        /** @type {__VLS_StyleScopedClasses['result-details']} */ ;
        __VLS_asFunctionalElement1(__VLS_intrinsics.p, __VLS_intrinsics.p)({});
        __VLS_asFunctionalElement1(__VLS_intrinsics.strong, __VLS_intrinsics.strong)({});
        (__VLS_ctx.verificationResult.details.documentName);
        (__VLS_ctx.verificationResult.details.documentSize);
        if (__VLS_ctx.verificationResult.details.chainStatus) {
            __VLS_asFunctionalElement1(__VLS_intrinsics.p, __VLS_intrinsics.p)({});
            __VLS_asFunctionalElement1(__VLS_intrinsics.strong, __VLS_intrinsics.strong)({});
            __VLS_asFunctionalElement1(__VLS_intrinsics.span, __VLS_intrinsics.span)({
                ...{ class: "badge success" },
            });
            /** @type {__VLS_StyleScopedClasses['badge']} */ ;
            /** @type {__VLS_StyleScopedClasses['success']} */ ;
            (__VLS_ctx.verificationResult.details.chainStatus);
        }
        __VLS_asFunctionalElement1(__VLS_intrinsics.div, __VLS_intrinsics.div)({
            ...{ class: "signatures-list" },
        });
        /** @type {__VLS_StyleScopedClasses['signatures-list']} */ ;
        __VLS_asFunctionalElement1(__VLS_intrinsics.h4, __VLS_intrinsics.h4)({});
        (__VLS_ctx.verificationResult.details.signatures?.length || 0);
        for (const [sig, index] of __VLS_vFor((__VLS_ctx.verificationResult.details.signatures))) {
            __VLS_asFunctionalElement1(__VLS_intrinsics.div, __VLS_intrinsics.div)({
                key: (index),
                ...{ class: "sig-item" },
            });
            /** @type {__VLS_StyleScopedClasses['sig-item']} */ ;
            __VLS_asFunctionalElement1(__VLS_intrinsics.div, __VLS_intrinsics.div)({
                ...{ class: "sig-header" },
            });
            /** @type {__VLS_StyleScopedClasses['sig-header']} */ ;
            __VLS_asFunctionalElement1(__VLS_intrinsics.span, __VLS_intrinsics.span)({
                ...{ class: "sig-index" },
            });
            /** @type {__VLS_StyleScopedClasses['sig-index']} */ ;
            (Number(index) + 1);
            __VLS_asFunctionalElement1(__VLS_intrinsics.span, __VLS_intrinsics.span)({
                ...{ class: "sig-time" },
            });
            /** @type {__VLS_StyleScopedClasses['sig-time']} */ ;
            (sig.timestamp);
            __VLS_asFunctionalElement1(__VLS_intrinsics.span, __VLS_intrinsics.span)({
                ...{ class: "sig-algo badge" },
                ...{ class: ({ success: sig.valid, error: !sig.valid }) },
            });
            /** @type {__VLS_StyleScopedClasses['sig-algo']} */ ;
            /** @type {__VLS_StyleScopedClasses['badge']} */ ;
            /** @type {__VLS_StyleScopedClasses['success']} */ ;
            /** @type {__VLS_StyleScopedClasses['error']} */ ;
            (sig.valid ? '✓ Valid' : '✗ Invalid');
            __VLS_asFunctionalElement1(__VLS_intrinsics.div, __VLS_intrinsics.div)({
                ...{ class: "sig-body" },
            });
            /** @type {__VLS_StyleScopedClasses['sig-body']} */ ;
            __VLS_asFunctionalElement1(__VLS_intrinsics.p, __VLS_intrinsics.p)({});
            __VLS_asFunctionalElement1(__VLS_intrinsics.strong, __VLS_intrinsics.strong)({});
            (sig.signer);
            __VLS_asFunctionalElement1(__VLS_intrinsics.p, __VLS_intrinsics.p)({});
            __VLS_asFunctionalElement1(__VLS_intrinsics.strong, __VLS_intrinsics.strong)({});
            (sig.algorithm);
            __VLS_asFunctionalElement1(__VLS_intrinsics.p, __VLS_intrinsics.p)({});
            __VLS_asFunctionalElement1(__VLS_intrinsics.strong, __VLS_intrinsics.strong)({});
            (sig.message);
            if (sig.certificateSubject) {
                __VLS_asFunctionalElement1(__VLS_intrinsics.p, __VLS_intrinsics.p)({});
                __VLS_asFunctionalElement1(__VLS_intrinsics.strong, __VLS_intrinsics.strong)({});
                (sig.certificateSubject);
            }
            if (sig.validFrom) {
                __VLS_asFunctionalElement1(__VLS_intrinsics.p, __VLS_intrinsics.p)({});
                __VLS_asFunctionalElement1(__VLS_intrinsics.strong, __VLS_intrinsics.strong)({});
                (sig.validFrom);
                (sig.validTo);
            }
            // @ts-ignore
            [file, file, handleVerify, loading, loading, errorMessage, errorMessage, verificationResult, verificationResult, verificationResult, verificationResult, verificationResult, verificationResult, verificationResult, verificationResult, verificationResult, verificationResult, verificationResult, verificationResult, verificationResult, verificationResult,];
        }
    }
    else if (__VLS_ctx.verificationResult.message) {
        __VLS_asFunctionalElement1(__VLS_intrinsics.p, __VLS_intrinsics.p)({});
        (__VLS_ctx.verificationResult.message);
    }
}
// @ts-ignore
[verificationResult, verificationResult,];
const __VLS_export = (await import('vue')).defineComponent({});
export default {};
//# sourceMappingURL=VerifyView.vue.js.map