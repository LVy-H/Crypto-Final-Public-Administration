import { ref, onMounted } from 'vue';
import { useRouter } from 'vue-router';
const router = useRouter();
const requests = ref([]);
const loading = ref(true);
const stats = ref({ totalUsers: 152, activeCerts: 43, pendingRequests: 2, todaySignatures: 18 });
const services = ref([
    { name: 'API Gateway', status: 'online' },
    { name: 'Identity Service', status: 'online' },
    { name: 'Cloud Sign', status: 'online' },
    { name: 'CA Authority', status: 'online' }
]);
function loadRequests() {
    loading.value = true;
    setTimeout(() => {
        requests.value = [
            {
                id: 'REQ-RA-2025-001',
                type: 'RA_REQUEST',
                requester: 'nguyenvana',
                submittedAt: '2025-10-25T10:30:00Z',
                status: 'PENDING'
            },
            {
                id: 'REQ-SIGN-2025-089',
                type: 'SIGNING_REQUEST',
                requester: 'lethib (Org)',
                submittedAt: '2025-10-25T11:15:00Z',
                status: 'PENDING'
            }
        ];
        loading.value = false;
    }, 600);
}
function reviewRequest(id) {
    router.push(`/officer/review/${id}`);
}
onMounted(() => {
    loadRequests();
});
const __VLS_ctx = {
    ...{},
    ...{},
};
let __VLS_components;
let __VLS_intrinsics;
let __VLS_directives;
/** @type {__VLS_StyleScopedClasses['stat-card']} */ ;
/** @type {__VLS_StyleScopedClasses['section']} */ ;
/** @type {__VLS_StyleScopedClasses['req-table']} */ ;
/** @type {__VLS_StyleScopedClasses['req-table']} */ ;
/** @type {__VLS_StyleScopedClasses['req-table']} */ ;
/** @type {__VLS_StyleScopedClasses['btn-review']} */ ;
/** @type {__VLS_StyleScopedClasses['dot']} */ ;
__VLS_asFunctionalElement1(__VLS_intrinsics.div, __VLS_intrinsics.div)({
    ...{ class: "page-container" },
});
/** @type {__VLS_StyleScopedClasses['page-container']} */ ;
__VLS_asFunctionalElement1(__VLS_intrinsics.div, __VLS_intrinsics.div)({
    ...{ class: "header" },
});
/** @type {__VLS_StyleScopedClasses['header']} */ ;
__VLS_asFunctionalElement1(__VLS_intrinsics.h2, __VLS_intrinsics.h2)({});
if (__VLS_ctx.loading) {
    __VLS_asFunctionalElement1(__VLS_intrinsics.div, __VLS_intrinsics.div)({
        ...{ class: "loading" },
    });
    /** @type {__VLS_StyleScopedClasses['loading']} */ ;
}
else {
    __VLS_asFunctionalElement1(__VLS_intrinsics.div, __VLS_intrinsics.div)({
        ...{ class: "stats-grid" },
    });
    /** @type {__VLS_StyleScopedClasses['stats-grid']} */ ;
    __VLS_asFunctionalElement1(__VLS_intrinsics.div, __VLS_intrinsics.div)({
        ...{ class: "stat-card" },
    });
    /** @type {__VLS_StyleScopedClasses['stat-card']} */ ;
    __VLS_asFunctionalElement1(__VLS_intrinsics.div, __VLS_intrinsics.div)({
        ...{ class: "val" },
    });
    /** @type {__VLS_StyleScopedClasses['val']} */ ;
    (__VLS_ctx.stats.totalUsers);
    __VLS_asFunctionalElement1(__VLS_intrinsics.div, __VLS_intrinsics.div)({
        ...{ class: "lbl" },
    });
    /** @type {__VLS_StyleScopedClasses['lbl']} */ ;
    __VLS_asFunctionalElement1(__VLS_intrinsics.div, __VLS_intrinsics.div)({
        ...{ class: "stat-card" },
    });
    /** @type {__VLS_StyleScopedClasses['stat-card']} */ ;
    __VLS_asFunctionalElement1(__VLS_intrinsics.div, __VLS_intrinsics.div)({
        ...{ class: "val" },
    });
    /** @type {__VLS_StyleScopedClasses['val']} */ ;
    (__VLS_ctx.stats.activeCerts);
    __VLS_asFunctionalElement1(__VLS_intrinsics.div, __VLS_intrinsics.div)({
        ...{ class: "lbl" },
    });
    /** @type {__VLS_StyleScopedClasses['lbl']} */ ;
    __VLS_asFunctionalElement1(__VLS_intrinsics.div, __VLS_intrinsics.div)({
        ...{ class: "stat-card highlight" },
    });
    /** @type {__VLS_StyleScopedClasses['stat-card']} */ ;
    /** @type {__VLS_StyleScopedClasses['highlight']} */ ;
    __VLS_asFunctionalElement1(__VLS_intrinsics.div, __VLS_intrinsics.div)({
        ...{ class: "val" },
    });
    /** @type {__VLS_StyleScopedClasses['val']} */ ;
    (__VLS_ctx.stats.pendingRequests);
    __VLS_asFunctionalElement1(__VLS_intrinsics.div, __VLS_intrinsics.div)({
        ...{ class: "lbl" },
    });
    /** @type {__VLS_StyleScopedClasses['lbl']} */ ;
    __VLS_asFunctionalElement1(__VLS_intrinsics.div, __VLS_intrinsics.div)({
        ...{ class: "stat-card" },
    });
    /** @type {__VLS_StyleScopedClasses['stat-card']} */ ;
    __VLS_asFunctionalElement1(__VLS_intrinsics.div, __VLS_intrinsics.div)({
        ...{ class: "val" },
    });
    /** @type {__VLS_StyleScopedClasses['val']} */ ;
    (__VLS_ctx.stats.todaySignatures);
    __VLS_asFunctionalElement1(__VLS_intrinsics.div, __VLS_intrinsics.div)({
        ...{ class: "lbl" },
    });
    /** @type {__VLS_StyleScopedClasses['lbl']} */ ;
    __VLS_asFunctionalElement1(__VLS_intrinsics.div, __VLS_intrinsics.div)({
        ...{ class: "section" },
    });
    /** @type {__VLS_StyleScopedClasses['section']} */ ;
    __VLS_asFunctionalElement1(__VLS_intrinsics.h3, __VLS_intrinsics.h3)({});
    __VLS_asFunctionalElement1(__VLS_intrinsics.table, __VLS_intrinsics.table)({
        ...{ class: "req-table" },
    });
    /** @type {__VLS_StyleScopedClasses['req-table']} */ ;
    __VLS_asFunctionalElement1(__VLS_intrinsics.thead, __VLS_intrinsics.thead)({});
    __VLS_asFunctionalElement1(__VLS_intrinsics.tr, __VLS_intrinsics.tr)({});
    __VLS_asFunctionalElement1(__VLS_intrinsics.th, __VLS_intrinsics.th)({});
    __VLS_asFunctionalElement1(__VLS_intrinsics.th, __VLS_intrinsics.th)({});
    __VLS_asFunctionalElement1(__VLS_intrinsics.th, __VLS_intrinsics.th)({});
    __VLS_asFunctionalElement1(__VLS_intrinsics.th, __VLS_intrinsics.th)({});
    __VLS_asFunctionalElement1(__VLS_intrinsics.th, __VLS_intrinsics.th)({});
    __VLS_asFunctionalElement1(__VLS_intrinsics.tbody, __VLS_intrinsics.tbody)({});
    for (const [r] of __VLS_vFor((__VLS_ctx.requests))) {
        __VLS_asFunctionalElement1(__VLS_intrinsics.tr, __VLS_intrinsics.tr)({
            key: (r.id),
        });
        __VLS_asFunctionalElement1(__VLS_intrinsics.td, __VLS_intrinsics.td)({
            ...{ class: "mono" },
        });
        /** @type {__VLS_StyleScopedClasses['mono']} */ ;
        (r.id);
        __VLS_asFunctionalElement1(__VLS_intrinsics.td, __VLS_intrinsics.td)({});
        (r.requester);
        __VLS_asFunctionalElement1(__VLS_intrinsics.td, __VLS_intrinsics.td)({});
        __VLS_asFunctionalElement1(__VLS_intrinsics.span, __VLS_intrinsics.span)({
            ...{ class: (['type-tag', r.type === 'RA_REQUEST' ? 'ra' : 'sign']) },
        });
        /** @type {__VLS_StyleScopedClasses['type-tag']} */ ;
        (r.type === 'RA_REQUEST' ? 'Cấp CKS' : 'Ký duyệt');
        __VLS_asFunctionalElement1(__VLS_intrinsics.td, __VLS_intrinsics.td)({});
        (new Date(r.submittedAt).toLocaleString('vi-VN'));
        __VLS_asFunctionalElement1(__VLS_intrinsics.td, __VLS_intrinsics.td)({});
        __VLS_asFunctionalElement1(__VLS_intrinsics.button, __VLS_intrinsics.button)({
            ...{ onClick: (...[$event]) => {
                    if (!!(__VLS_ctx.loading))
                        return;
                    __VLS_ctx.reviewRequest(r.id);
                    // @ts-ignore
                    [loading, stats, stats, stats, stats, requests, reviewRequest,];
                } },
            ...{ class: "btn-review" },
        });
        /** @type {__VLS_StyleScopedClasses['btn-review']} */ ;
        // @ts-ignore
        [];
    }
    __VLS_asFunctionalElement1(__VLS_intrinsics.div, __VLS_intrinsics.div)({
        ...{ class: "section" },
    });
    /** @type {__VLS_StyleScopedClasses['section']} */ ;
    __VLS_asFunctionalElement1(__VLS_intrinsics.h3, __VLS_intrinsics.h3)({});
    __VLS_asFunctionalElement1(__VLS_intrinsics.div, __VLS_intrinsics.div)({
        ...{ class: "svc-grid" },
    });
    /** @type {__VLS_StyleScopedClasses['svc-grid']} */ ;
    for (const [svc] of __VLS_vFor((__VLS_ctx.services))) {
        __VLS_asFunctionalElement1(__VLS_intrinsics.div, __VLS_intrinsics.div)({
            key: (svc.name),
            ...{ class: "svc-item" },
        });
        /** @type {__VLS_StyleScopedClasses['svc-item']} */ ;
        __VLS_asFunctionalElement1(__VLS_intrinsics.span, __VLS_intrinsics.span)({
            ...{ class: (['dot', svc.status]) },
        });
        /** @type {__VLS_StyleScopedClasses['dot']} */ ;
        __VLS_asFunctionalElement1(__VLS_intrinsics.span, __VLS_intrinsics.span)({});
        (svc.name);
        // @ts-ignore
        [services,];
    }
}
// @ts-ignore
[];
const __VLS_export = (await import('vue')).defineComponent({});
export default {};
//# sourceMappingURL=OfficerDashboard.vue.js.map