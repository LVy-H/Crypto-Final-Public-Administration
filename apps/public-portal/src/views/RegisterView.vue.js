import { ref } from 'vue';
import { useRouter } from 'vue-router';
import pqc from '@/services/pqc';
const router = useRouter();
const form = ref({ username: '', email: '', algorithm: 'ML-DSA-65', kycData: '' });
const loading = ref(false);
const error = ref('');
const success = ref(false);
async function handleRegister() {
    loading.value = true;
    error.value = '';
    try {
        if (!form.value.username || !form.value.email) {
            throw new Error('Please fill in all required fields');
        }
        // 1. Generate PQC Key Pair (Client-Side)
        console.log(`Generating ${form.value.algorithm} key pair...`);
        const keyPair = await pqc.generateKeyPair(form.value.algorithm);
        // 2. Generate CSR (Client-Side)
        const subjectDn = `CN=${form.value.username},EMAIL=${form.value.email},UID=${form.value.kycData}`;
        console.log('Generating CSR for:', subjectDn);
        const csrResult = await pqc.generateCsrData(subjectDn, keyPair);
        // 3. Securely store Private Key (Client-Side)
        // For demo, we use a simple passphrase. In prod, prompt user or use derived key.
        const passphrase = form.value.username + "-secret";
        await pqc.storePrivateKey(keyPair.secretKey, form.value.username, passphrase);
        // 4. Submit CSR to Backend
        const response = await fetch('/api/pki/enroll', {
            method: 'POST',
            headers: {
                'Content-Type': 'application/json',
                'X-User-Id': form.value.username
            },
            body: JSON.stringify({ csr: csrResult.csrBase64 })
        });
        if (!response.ok) {
            throw new Error(`Registration failed: ${response.statusText}`);
        }
        const result = await response.json();
        console.log('Enrollment success:', result);
        success.value = true;
        setTimeout(() => router.push('/dashboard'), 1500);
    }
    catch (e) {
        console.error(e);
        error.value = e.message || 'Registration failed';
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
/** @type {__VLS_StyleScopedClasses['form-group']} */ ;
/** @type {__VLS_StyleScopedClasses['form-group']} */ ;
/** @type {__VLS_StyleScopedClasses['form-group']} */ ;
/** @type {__VLS_StyleScopedClasses['form-group']} */ ;
/** @type {__VLS_StyleScopedClasses['form-group']} */ ;
/** @type {__VLS_StyleScopedClasses['btn-primary']} */ ;
/** @type {__VLS_StyleScopedClasses['btn-primary']} */ ;
__VLS_asFunctionalElement1(__VLS_intrinsics.div, __VLS_intrinsics.div)({
    ...{ class: "page-container" },
});
/** @type {__VLS_StyleScopedClasses['page-container']} */ ;
__VLS_asFunctionalElement1(__VLS_intrinsics.div, __VLS_intrinsics.div)({
    ...{ class: "card" },
});
/** @type {__VLS_StyleScopedClasses['card']} */ ;
__VLS_asFunctionalElement1(__VLS_intrinsics.h2, __VLS_intrinsics.h2)({
    ...{ class: "title" },
});
/** @type {__VLS_StyleScopedClasses['title']} */ ;
__VLS_asFunctionalElement1(__VLS_intrinsics.form, __VLS_intrinsics.form)({
    ...{ onSubmit: (__VLS_ctx.handleRegister) },
});
__VLS_asFunctionalElement1(__VLS_intrinsics.div, __VLS_intrinsics.div)({
    ...{ class: "form-group" },
});
/** @type {__VLS_StyleScopedClasses['form-group']} */ ;
__VLS_asFunctionalElement1(__VLS_intrinsics.label, __VLS_intrinsics.label)({});
__VLS_asFunctionalElement1(__VLS_intrinsics.input)({
    value: (__VLS_ctx.form.username),
    type: "text",
    required: true,
    placeholder: "nguyenvana",
});
__VLS_asFunctionalElement1(__VLS_intrinsics.div, __VLS_intrinsics.div)({
    ...{ class: "form-group" },
});
/** @type {__VLS_StyleScopedClasses['form-group']} */ ;
__VLS_asFunctionalElement1(__VLS_intrinsics.label, __VLS_intrinsics.label)({});
__VLS_asFunctionalElement1(__VLS_intrinsics.input)({
    type: "email",
    required: true,
    placeholder: "email@example.com",
});
(__VLS_ctx.form.email);
__VLS_asFunctionalElement1(__VLS_intrinsics.div, __VLS_intrinsics.div)({
    ...{ class: "form-group" },
});
/** @type {__VLS_StyleScopedClasses['form-group']} */ ;
__VLS_asFunctionalElement1(__VLS_intrinsics.label, __VLS_intrinsics.label)({});
__VLS_asFunctionalElement1(__VLS_intrinsics.select, __VLS_intrinsics.select)({
    value: (__VLS_ctx.form.algorithm),
});
__VLS_asFunctionalElement1(__VLS_intrinsics.option, __VLS_intrinsics.option)({
    value: "ML-DSA-44",
});
__VLS_asFunctionalElement1(__VLS_intrinsics.option, __VLS_intrinsics.option)({
    value: "ML-DSA-65",
});
__VLS_asFunctionalElement1(__VLS_intrinsics.option, __VLS_intrinsics.option)({
    value: "ML-DSA-87",
});
__VLS_asFunctionalElement1(__VLS_intrinsics.option, __VLS_intrinsics.option)({
    value: "SLH-DSA-SHAKE-128F",
});
__VLS_asFunctionalElement1(__VLS_intrinsics.div, __VLS_intrinsics.div)({
    ...{ class: "form-group" },
});
/** @type {__VLS_StyleScopedClasses['form-group']} */ ;
__VLS_asFunctionalElement1(__VLS_intrinsics.label, __VLS_intrinsics.label)({});
__VLS_asFunctionalElement1(__VLS_intrinsics.input)({
    value: (__VLS_ctx.form.kycData),
    type: "text",
    required: true,
    placeholder: "012345678912",
});
if (__VLS_ctx.error) {
    __VLS_asFunctionalElement1(__VLS_intrinsics.div, __VLS_intrinsics.div)({
        ...{ class: "error" },
    });
    /** @type {__VLS_StyleScopedClasses['error']} */ ;
    (__VLS_ctx.error);
}
if (__VLS_ctx.success) {
    __VLS_asFunctionalElement1(__VLS_intrinsics.div, __VLS_intrinsics.div)({
        ...{ class: "success" },
    });
    /** @type {__VLS_StyleScopedClasses['success']} */ ;
}
__VLS_asFunctionalElement1(__VLS_intrinsics.button, __VLS_intrinsics.button)({
    type: "submit",
    disabled: (__VLS_ctx.loading),
    ...{ class: "btn-primary" },
});
/** @type {__VLS_StyleScopedClasses['btn-primary']} */ ;
(__VLS_ctx.loading ? 'Đang xử lý...' : 'Gửi yêu cầu');
// @ts-ignore
[handleRegister, form, form, form, form, error, error, success, loading, loading,];
const __VLS_export = (await import('vue')).defineComponent({});
export default {};
//# sourceMappingURL=RegisterView.vue.js.map