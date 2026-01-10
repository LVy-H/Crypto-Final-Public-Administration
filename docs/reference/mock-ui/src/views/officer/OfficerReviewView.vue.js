import { ref } from 'vue';
import { useRouter, useRoute } from 'vue-router';
const router = useRouter();
const route = useRoute();
const reqId = route.params.id;
const step = ref(1); // 1: Review, 2: Signing
const loading = ref(false);
const signed = ref(false);
const requestData = {
    id: reqId,
    requester: 'nguyenvana',
    type: 'RA_REQUEST',
    details: {
        fullName: 'Nguyen Van A',
        idCard: '001088012345',
        email: 'nguyenvana@gov.vn',
        algorithm: 'ML-DSA-65',
        reason: 'New Employee Registration'
    }
};
async function startSigning() {
    step.value = 2; // Move to signing UI
}
async function confirmSignAndApprove() {
    loading.value = true;
    // Mock Officer Signing (Approving with Key)
    setTimeout(() => {
        loading.value = false;
        signed.value = true;
        setTimeout(() => router.push('/officer'), 2000);
    }, 2000);
}
const __VLS_ctx = {
    ...{},
    ...{},
};
let __VLS_components;
let __VLS_intrinsics;
let __VLS_directives;
/** @type {__VLS_StyleScopedClasses['header']} */ ;
/** @type {__VLS_StyleScopedClasses['btn-back']} */ ;
/** @type {__VLS_StyleScopedClasses['info-section']} */ ;
/** @type {__VLS_StyleScopedClasses['val']} */ ;
/** @type {__VLS_StyleScopedClasses['btn-next']} */ ;
/** @type {__VLS_StyleScopedClasses['signing-ui']} */ ;
/** @type {__VLS_StyleScopedClasses['sign-form']} */ ;
/** @type {__VLS_StyleScopedClasses['field']} */ ;
/** @type {__VLS_StyleScopedClasses['sign-form']} */ ;
/** @type {__VLS_StyleScopedClasses['sign-form']} */ ;
/** @type {__VLS_StyleScopedClasses['btn-sign']} */ ;
/** @type {__VLS_StyleScopedClasses['btn-sign']} */ ;
/** @type {__VLS_StyleScopedClasses['success-box']} */ ;
__VLS_asFunctionalElement1(__VLS_intrinsics.div, __VLS_intrinsics.div)({
    ...{ class: "page-container" },
});
/** @type {__VLS_StyleScopedClasses['page-container']} */ ;
__VLS_asFunctionalElement1(__VLS_intrinsics.div, __VLS_intrinsics.div)({
    ...{ class: "panel" },
});
/** @type {__VLS_StyleScopedClasses['panel']} */ ;
__VLS_asFunctionalElement1(__VLS_intrinsics.div, __VLS_intrinsics.div)({
    ...{ class: "header" },
});
/** @type {__VLS_StyleScopedClasses['header']} */ ;
__VLS_asFunctionalElement1(__VLS_intrinsics.button, __VLS_intrinsics.button)({
    ...{ onClick: (...[$event]) => {
            __VLS_ctx.router.back();
            // @ts-ignore
            [router,];
        } },
    ...{ class: "btn-back" },
});
/** @type {__VLS_StyleScopedClasses['btn-back']} */ ;
__VLS_asFunctionalElement1(__VLS_intrinsics.h2, __VLS_intrinsics.h2)({});
(__VLS_ctx.reqId);
if (__VLS_ctx.step === 1) {
    __VLS_asFunctionalElement1(__VLS_intrinsics.div, __VLS_intrinsics.div)({
        ...{ class: "step-content" },
    });
    /** @type {__VLS_StyleScopedClasses['step-content']} */ ;
    __VLS_asFunctionalElement1(__VLS_intrinsics.div, __VLS_intrinsics.div)({
        ...{ class: "info-section" },
    });
    /** @type {__VLS_StyleScopedClasses['info-section']} */ ;
    __VLS_asFunctionalElement1(__VLS_intrinsics.h3, __VLS_intrinsics.h3)({});
    __VLS_asFunctionalElement1(__VLS_intrinsics.div, __VLS_intrinsics.div)({
        ...{ class: "form-grid" },
    });
    /** @type {__VLS_StyleScopedClasses['form-grid']} */ ;
    __VLS_asFunctionalElement1(__VLS_intrinsics.div, __VLS_intrinsics.div)({
        ...{ class: "field" },
    });
    /** @type {__VLS_StyleScopedClasses['field']} */ ;
    __VLS_asFunctionalElement1(__VLS_intrinsics.label, __VLS_intrinsics.label)({});
    __VLS_asFunctionalElement1(__VLS_intrinsics.div, __VLS_intrinsics.div)({
        ...{ class: "val" },
    });
    /** @type {__VLS_StyleScopedClasses['val']} */ ;
    (__VLS_ctx.requestData.details.fullName);
    __VLS_asFunctionalElement1(__VLS_intrinsics.div, __VLS_intrinsics.div)({
        ...{ class: "field" },
    });
    /** @type {__VLS_StyleScopedClasses['field']} */ ;
    __VLS_asFunctionalElement1(__VLS_intrinsics.label, __VLS_intrinsics.label)({});
    __VLS_asFunctionalElement1(__VLS_intrinsics.div, __VLS_intrinsics.div)({
        ...{ class: "val" },
    });
    /** @type {__VLS_StyleScopedClasses['val']} */ ;
    (__VLS_ctx.requestData.details.idCard);
    __VLS_asFunctionalElement1(__VLS_intrinsics.div, __VLS_intrinsics.div)({
        ...{ class: "field" },
    });
    /** @type {__VLS_StyleScopedClasses['field']} */ ;
    __VLS_asFunctionalElement1(__VLS_intrinsics.label, __VLS_intrinsics.label)({});
    __VLS_asFunctionalElement1(__VLS_intrinsics.div, __VLS_intrinsics.div)({
        ...{ class: "val" },
    });
    /** @type {__VLS_StyleScopedClasses['val']} */ ;
    (__VLS_ctx.requestData.details.email);
    __VLS_asFunctionalElement1(__VLS_intrinsics.div, __VLS_intrinsics.div)({
        ...{ class: "field" },
    });
    /** @type {__VLS_StyleScopedClasses['field']} */ ;
    __VLS_asFunctionalElement1(__VLS_intrinsics.label, __VLS_intrinsics.label)({});
    __VLS_asFunctionalElement1(__VLS_intrinsics.div, __VLS_intrinsics.div)({
        ...{ class: "val tag" },
    });
    /** @type {__VLS_StyleScopedClasses['val']} */ ;
    /** @type {__VLS_StyleScopedClasses['tag']} */ ;
    (__VLS_ctx.requestData.details.algorithm);
    __VLS_asFunctionalElement1(__VLS_intrinsics.div, __VLS_intrinsics.div)({
        ...{ class: "actions" },
    });
    /** @type {__VLS_StyleScopedClasses['actions']} */ ;
    __VLS_asFunctionalElement1(__VLS_intrinsics.button, __VLS_intrinsics.button)({
        ...{ class: "btn-reject" },
    });
    /** @type {__VLS_StyleScopedClasses['btn-reject']} */ ;
    __VLS_asFunctionalElement1(__VLS_intrinsics.div, __VLS_intrinsics.div)({
        ...{ class: "spacer" },
    });
    /** @type {__VLS_StyleScopedClasses['spacer']} */ ;
    __VLS_asFunctionalElement1(__VLS_intrinsics.button, __VLS_intrinsics.button)({
        ...{ onClick: (__VLS_ctx.startSigning) },
        ...{ class: "btn-next" },
    });
    /** @type {__VLS_StyleScopedClasses['btn-next']} */ ;
}
else {
    __VLS_asFunctionalElement1(__VLS_intrinsics.div, __VLS_intrinsics.div)({
        ...{ class: "step-content signing-ui" },
    });
    /** @type {__VLS_StyleScopedClasses['step-content']} */ ;
    /** @type {__VLS_StyleScopedClasses['signing-ui']} */ ;
    __VLS_asFunctionalElement1(__VLS_intrinsics.h3, __VLS_intrinsics.h3)({});
    __VLS_asFunctionalElement1(__VLS_intrinsics.span, __VLS_intrinsics.span)({
        ...{ class: "icon" },
    });
    /** @type {__VLS_StyleScopedClasses['icon']} */ ;
    __VLS_asFunctionalElement1(__VLS_intrinsics.p, __VLS_intrinsics.p)({
        ...{ class: "desc" },
    });
    /** @type {__VLS_StyleScopedClasses['desc']} */ ;
    __VLS_asFunctionalElement1(__VLS_intrinsics.div, __VLS_intrinsics.div)({
        ...{ class: "sign-form" },
    });
    /** @type {__VLS_StyleScopedClasses['sign-form']} */ ;
    __VLS_asFunctionalElement1(__VLS_intrinsics.div, __VLS_intrinsics.div)({
        ...{ class: "field" },
    });
    /** @type {__VLS_StyleScopedClasses['field']} */ ;
    __VLS_asFunctionalElement1(__VLS_intrinsics.label, __VLS_intrinsics.label)({});
    __VLS_asFunctionalElement1(__VLS_intrinsics.select, __VLS_intrinsics.select)({
        disabled: true,
    });
    __VLS_asFunctionalElement1(__VLS_intrinsics.option, __VLS_intrinsics.option)({});
    __VLS_asFunctionalElement1(__VLS_intrinsics.div, __VLS_intrinsics.div)({
        ...{ class: "field" },
    });
    /** @type {__VLS_StyleScopedClasses['field']} */ ;
    __VLS_asFunctionalElement1(__VLS_intrinsics.label, __VLS_intrinsics.label)({});
    __VLS_asFunctionalElement1(__VLS_intrinsics.input)({
        type: "password",
        value: "123456",
        readonly: true,
        ...{ class: "input-pin" },
    });
    /** @type {__VLS_StyleScopedClasses['input-pin']} */ ;
    if (__VLS_ctx.signed) {
        __VLS_asFunctionalElement1(__VLS_intrinsics.div, __VLS_intrinsics.div)({
            ...{ class: "success-box" },
        });
        /** @type {__VLS_StyleScopedClasses['success-box']} */ ;
        __VLS_asFunctionalElement1(__VLS_intrinsics.h4, __VLS_intrinsics.h4)({});
        __VLS_asFunctionalElement1(__VLS_intrinsics.p, __VLS_intrinsics.p)({});
        __VLS_asFunctionalElement1(__VLS_intrinsics.code, __VLS_intrinsics.code)({
            ...{ class: "sig-block" },
        });
        /** @type {__VLS_StyleScopedClasses['sig-block']} */ ;
        __VLS_asFunctionalElement1(__VLS_intrinsics.p, __VLS_intrinsics.p)({});
    }
    else {
        __VLS_asFunctionalElement1(__VLS_intrinsics.button, __VLS_intrinsics.button)({
            ...{ onClick: (__VLS_ctx.confirmSignAndApprove) },
            disabled: (__VLS_ctx.loading),
            ...{ class: "btn-sign" },
        });
        /** @type {__VLS_StyleScopedClasses['btn-sign']} */ ;
        (__VLS_ctx.loading ? 'Đang thực hiện ký (Signing)...' : 'Ký duyệt & Hoàn tất');
    }
}
// @ts-ignore
[reqId, step, requestData, requestData, requestData, requestData, startSigning, signed, confirmSignAndApprove, loading, loading,];
const __VLS_export = (await import('vue')).defineComponent({});
export default {};
//# sourceMappingURL=OfficerReviewView.vue.js.map