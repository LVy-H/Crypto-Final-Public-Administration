import { ref, onMounted } from 'vue';
const certificates = ref([]);
const loading = ref(true);
function loadCertificates() {
    loading.value = true;
    // Mock GET /api/v1/certificates/my
    setTimeout(() => {
        certificates.value = [
            {
                id: 'uuid-1',
                serialNumber: '547823901238',
                subject: 'CN=Nguyen Van A, O=Gov, C=VN',
                algorithm: 'ML-DSA-44',
                status: 'ACTIVE',
                validUntil: '2026-12-31'
            },
            {
                id: 'uuid-2',
                serialNumber: '123890123890',
                subject: 'CN=Nguyen Van A (Signer), O=Gov, C=VN',
                algorithm: 'ML-DSA-65',
                status: 'REVOKED',
                validUntil: '2025-06-30'
            }
        ];
        loading.value = false;
    }, 800);
}
function requestNewCert() {
    alert('Called POST /api/v1/certificates/request');
    loadCertificates(); // Refresh mock
}
function downloadCert(id) {
    alert(`Called GET /api/v1/certificates/${id}/download`);
}
onMounted(() => {
    loadCertificates();
});
const __VLS_ctx = {
    ...{},
    ...{},
};
let __VLS_components;
let __VLS_intrinsics;
let __VLS_directives;
/** @type {__VLS_StyleScopedClasses['cert-table']} */ ;
/** @type {__VLS_StyleScopedClasses['cert-table']} */ ;
/** @type {__VLS_StyleScopedClasses['cert-table']} */ ;
/** @type {__VLS_StyleScopedClasses['status']} */ ;
/** @type {__VLS_StyleScopedClasses['status']} */ ;
/** @type {__VLS_StyleScopedClasses['btn-sm']} */ ;
__VLS_asFunctionalElement1(__VLS_intrinsics.div, __VLS_intrinsics.div)({
    ...{ class: "page-container" },
});
/** @type {__VLS_StyleScopedClasses['page-container']} */ ;
__VLS_asFunctionalElement1(__VLS_intrinsics.div, __VLS_intrinsics.div)({
    ...{ class: "header" },
});
/** @type {__VLS_StyleScopedClasses['header']} */ ;
__VLS_asFunctionalElement1(__VLS_intrinsics.h2, __VLS_intrinsics.h2)({});
__VLS_asFunctionalElement1(__VLS_intrinsics.button, __VLS_intrinsics.button)({
    ...{ onClick: (__VLS_ctx.requestNewCert) },
    ...{ class: "btn-new" },
});
/** @type {__VLS_StyleScopedClasses['btn-new']} */ ;
if (__VLS_ctx.loading) {
    __VLS_asFunctionalElement1(__VLS_intrinsics.div, __VLS_intrinsics.div)({
        ...{ class: "loading" },
    });
    /** @type {__VLS_StyleScopedClasses['loading']} */ ;
}
else {
    __VLS_asFunctionalElement1(__VLS_intrinsics.table, __VLS_intrinsics.table)({
        ...{ class: "cert-table" },
    });
    /** @type {__VLS_StyleScopedClasses['cert-table']} */ ;
    __VLS_asFunctionalElement1(__VLS_intrinsics.thead, __VLS_intrinsics.thead)({});
    __VLS_asFunctionalElement1(__VLS_intrinsics.tr, __VLS_intrinsics.tr)({});
    __VLS_asFunctionalElement1(__VLS_intrinsics.th, __VLS_intrinsics.th)({});
    __VLS_asFunctionalElement1(__VLS_intrinsics.th, __VLS_intrinsics.th)({});
    __VLS_asFunctionalElement1(__VLS_intrinsics.th, __VLS_intrinsics.th)({});
    __VLS_asFunctionalElement1(__VLS_intrinsics.th, __VLS_intrinsics.th)({});
    __VLS_asFunctionalElement1(__VLS_intrinsics.th, __VLS_intrinsics.th)({});
    __VLS_asFunctionalElement1(__VLS_intrinsics.th, __VLS_intrinsics.th)({});
    __VLS_asFunctionalElement1(__VLS_intrinsics.tbody, __VLS_intrinsics.tbody)({});
    for (const [cert] of __VLS_vFor((__VLS_ctx.certificates))) {
        __VLS_asFunctionalElement1(__VLS_intrinsics.tr, __VLS_intrinsics.tr)({
            key: (cert.id),
        });
        __VLS_asFunctionalElement1(__VLS_intrinsics.td, __VLS_intrinsics.td)({});
        (cert.serialNumber);
        __VLS_asFunctionalElement1(__VLS_intrinsics.td, __VLS_intrinsics.td)({});
        (cert.subject);
        __VLS_asFunctionalElement1(__VLS_intrinsics.td, __VLS_intrinsics.td)({});
        __VLS_asFunctionalElement1(__VLS_intrinsics.span, __VLS_intrinsics.span)({
            ...{ class: "tag-algo" },
        });
        /** @type {__VLS_StyleScopedClasses['tag-algo']} */ ;
        (cert.algorithm);
        __VLS_asFunctionalElement1(__VLS_intrinsics.td, __VLS_intrinsics.td)({});
        __VLS_asFunctionalElement1(__VLS_intrinsics.span, __VLS_intrinsics.span)({
            ...{ class: (['status', cert.status.toLowerCase()]) },
        });
        /** @type {__VLS_StyleScopedClasses['status']} */ ;
        (cert.status);
        __VLS_asFunctionalElement1(__VLS_intrinsics.td, __VLS_intrinsics.td)({});
        (cert.validUntil);
        __VLS_asFunctionalElement1(__VLS_intrinsics.td, __VLS_intrinsics.td)({});
        __VLS_asFunctionalElement1(__VLS_intrinsics.button, __VLS_intrinsics.button)({
            ...{ onClick: (...[$event]) => {
                    if (!!(__VLS_ctx.loading))
                        return;
                    __VLS_ctx.downloadCert(cert.id);
                    // @ts-ignore
                    [requestNewCert, loading, certificates, downloadCert,];
                } },
            ...{ class: "btn-sm" },
        });
        /** @type {__VLS_StyleScopedClasses['btn-sm']} */ ;
        // @ts-ignore
        [];
    }
}
// @ts-ignore
[];
const __VLS_export = (await import('vue')).defineComponent({});
export default {};
//# sourceMappingURL=DashboardView.vue.js.map