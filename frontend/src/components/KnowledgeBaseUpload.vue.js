import { ref, onMounted } from "vue";
import { listDocuments, uploadDocument, deleteDocument } from "../services/enterpriseApi";
const loading = ref(false);
const uploadingFile = ref("");
const isDragging = ref(false);
const documents = ref([]);
const message = ref("");
const messageType = ref("success");
const fileInput = ref(null);
function triggerFileInput() {
    fileInput.value?.click();
}
async function refreshDocuments() {
    loading.value = true;
    message.value = "";
    try {
        const result = await listDocuments();
        documents.value = result.documents || [];
    }
    catch {
        showMessage("获取文档列表失败", "error");
    }
    finally {
        loading.value = false;
    }
}
function showMessage(msg, type = "success") {
    message.value = msg;
    messageType.value = type;
    setTimeout(() => {
        message.value = "";
    }, 3000);
}
function onDragOver() {
    isDragging.value = true;
}
function onDragLeave() {
    isDragging.value = false;
}
async function onDrop(e) {
    isDragging.value = false;
    const files = e.dataTransfer?.files;
    if (files && files.length > 0) {
        await handleFiles(Array.from(files));
    }
}
async function onFileSelect(e) {
    const input = e.target;
    if (input.files && input.files.length > 0) {
        await handleFiles(Array.from(input.files));
        input.value = "";
    }
}
async function handleFiles(files) {
    for (const file of files) {
        const ext = file.name.split(".").pop()?.toLowerCase();
        if (!["md", "pdf", "doc", "docx", "txt"].includes(ext || "")) {
            showMessage(`不支持的文件类型: ${file.name}`, "error");
            continue;
        }
        uploadingFile.value = file.name;
        try {
            const result = await uploadDocument(file);
            if (result.success) {
                showMessage(`${file.name} 上传成功`, "success");
                await refreshDocuments();
            }
            else {
                showMessage(result.message || "上传失败", "error");
            }
        }
        catch (err) {
            showMessage(`上传失败: ${err.message}`, "error");
        }
        finally {
            uploadingFile.value = "";
        }
    }
}
async function onDelete(filename) {
    if (!confirm(`确定要删除文档 "${filename}" 吗？`)) {
        return;
    }
    try {
        const result = await deleteDocument(filename);
        if (result.success) {
            showMessage("删除成功", "success");
            await refreshDocuments();
        }
        else {
            showMessage(result.message || "删除失败", "error");
        }
    }
    catch {
        showMessage("删除失败", "error");
    }
}
function getFileIcon(filename) {
    const ext = filename.split(".").pop()?.toLowerCase();
    switch (ext) {
        case "md": return "M";
        case "pdf": return "P";
        case "doc":
        case "docx": return "W";
        case "txt": return "T";
        default: return "F";
    }
}
onMounted(() => {
    refreshDocuments();
});
debugger; /* PartiallyEnd: #3632/scriptSetup.vue */
const __VLS_ctx = {};
let __VLS_components;
let __VLS_directives;
/** @type {__VLS_StyleScopedClasses['kb-header']} */ ;
/** @type {__VLS_StyleScopedClasses['refresh-btn']} */ ;
/** @type {__VLS_StyleScopedClasses['icon']} */ ;
/** @type {__VLS_StyleScopedClasses['upload-area']} */ ;
/** @type {__VLS_StyleScopedClasses['upload-area']} */ ;
/** @type {__VLS_StyleScopedClasses['document-item']} */ ;
/** @type {__VLS_StyleScopedClasses['delete-btn']} */ ;
/** @type {__VLS_StyleScopedClasses['message']} */ ;
/** @type {__VLS_StyleScopedClasses['message']} */ ;
// CSS variable injection 
// CSS variable injection end 
__VLS_asFunctionalElement(__VLS_intrinsicElements.div, __VLS_intrinsicElements.div)({
    ...{ class: "knowledge-base" },
});
__VLS_asFunctionalElement(__VLS_intrinsicElements.div, __VLS_intrinsicElements.div)({
    ...{ class: "kb-header" },
});
__VLS_asFunctionalElement(__VLS_intrinsicElements.h3, __VLS_intrinsicElements.h3)({});
__VLS_asFunctionalElement(__VLS_intrinsicElements.button, __VLS_intrinsicElements.button)({
    ...{ onClick: (__VLS_ctx.refreshDocuments) },
    ...{ class: "refresh-btn" },
    disabled: (__VLS_ctx.loading),
});
__VLS_asFunctionalElement(__VLS_intrinsicElements.span, __VLS_intrinsicElements.span)({
    ...{ class: (['icon', { spinning: __VLS_ctx.loading }]) },
});
__VLS_asFunctionalElement(__VLS_intrinsicElements.div, __VLS_intrinsicElements.div)({
    ...{ onDragover: (__VLS_ctx.onDragOver) },
    ...{ onDragleave: (__VLS_ctx.onDragLeave) },
    ...{ onDrop: (__VLS_ctx.onDrop) },
    ...{ class: "upload-area" },
    ...{ class: ({ 'drag-over': __VLS_ctx.isDragging }) },
});
__VLS_asFunctionalElement(__VLS_intrinsicElements.input)({
    ...{ onChange: (__VLS_ctx.onFileSelect) },
    ref: "fileInput",
    type: "file",
    accept: ".md",
    hidden: true,
});
/** @type {typeof __VLS_ctx.fileInput} */ ;
__VLS_asFunctionalElement(__VLS_intrinsicElements.div, __VLS_intrinsicElements.div)({
    ...{ onClick: (__VLS_ctx.triggerFileInput) },
    ...{ class: "upload-content" },
});
__VLS_asFunctionalElement(__VLS_intrinsicElements.span, __VLS_intrinsicElements.span)({
    ...{ class: "upload-icon" },
});
__VLS_asFunctionalElement(__VLS_intrinsicElements.span, __VLS_intrinsicElements.span)({
    ...{ class: "upload-text" },
});
__VLS_asFunctionalElement(__VLS_intrinsicElements.span, __VLS_intrinsicElements.span)({
    ...{ class: "upload-hint" },
});
if (__VLS_ctx.uploadingFile) {
    __VLS_asFunctionalElement(__VLS_intrinsicElements.div, __VLS_intrinsicElements.div)({
        ...{ class: "upload-progress" },
    });
    __VLS_asFunctionalElement(__VLS_intrinsicElements.span, __VLS_intrinsicElements.span)({});
    (__VLS_ctx.uploadingFile);
}
__VLS_asFunctionalElement(__VLS_intrinsicElements.div, __VLS_intrinsicElements.div)({
    ...{ class: "document-list" },
});
if (__VLS_ctx.documents.length === 0) {
    __VLS_asFunctionalElement(__VLS_intrinsicElements.div, __VLS_intrinsicElements.div)({
        ...{ class: "empty-state" },
    });
}
for (const [doc] of __VLS_getVForSourceType((__VLS_ctx.documents))) {
    __VLS_asFunctionalElement(__VLS_intrinsicElements.div, __VLS_intrinsicElements.div)({
        key: (doc),
        ...{ class: "document-item" },
    });
    __VLS_asFunctionalElement(__VLS_intrinsicElements.div, __VLS_intrinsicElements.div)({
        ...{ class: "doc-icon" },
    });
    (__VLS_ctx.getFileIcon(doc));
    __VLS_asFunctionalElement(__VLS_intrinsicElements.div, __VLS_intrinsicElements.div)({
        ...{ class: "doc-info" },
    });
    __VLS_asFunctionalElement(__VLS_intrinsicElements.span, __VLS_intrinsicElements.span)({
        ...{ class: "doc-name" },
        title: (doc),
    });
    (doc);
    __VLS_asFunctionalElement(__VLS_intrinsicElements.button, __VLS_intrinsicElements.button)({
        ...{ onClick: (...[$event]) => {
                __VLS_ctx.onDelete(doc);
            } },
        ...{ class: "delete-btn" },
        title: "删除",
    });
}
if (__VLS_ctx.message) {
    __VLS_asFunctionalElement(__VLS_intrinsicElements.div, __VLS_intrinsicElements.div)({
        ...{ class: (['message', __VLS_ctx.messageType]) },
    });
    (__VLS_ctx.message);
}
/** @type {__VLS_StyleScopedClasses['knowledge-base']} */ ;
/** @type {__VLS_StyleScopedClasses['kb-header']} */ ;
/** @type {__VLS_StyleScopedClasses['refresh-btn']} */ ;
/** @type {__VLS_StyleScopedClasses['upload-area']} */ ;
/** @type {__VLS_StyleScopedClasses['upload-content']} */ ;
/** @type {__VLS_StyleScopedClasses['upload-icon']} */ ;
/** @type {__VLS_StyleScopedClasses['upload-text']} */ ;
/** @type {__VLS_StyleScopedClasses['upload-hint']} */ ;
/** @type {__VLS_StyleScopedClasses['upload-progress']} */ ;
/** @type {__VLS_StyleScopedClasses['document-list']} */ ;
/** @type {__VLS_StyleScopedClasses['empty-state']} */ ;
/** @type {__VLS_StyleScopedClasses['document-item']} */ ;
/** @type {__VLS_StyleScopedClasses['doc-icon']} */ ;
/** @type {__VLS_StyleScopedClasses['doc-info']} */ ;
/** @type {__VLS_StyleScopedClasses['doc-name']} */ ;
/** @type {__VLS_StyleScopedClasses['delete-btn']} */ ;
var __VLS_dollars;
const __VLS_self = (await import('vue')).defineComponent({
    setup() {
        return {
            loading: loading,
            uploadingFile: uploadingFile,
            isDragging: isDragging,
            documents: documents,
            message: message,
            messageType: messageType,
            fileInput: fileInput,
            triggerFileInput: triggerFileInput,
            refreshDocuments: refreshDocuments,
            onDragOver: onDragOver,
            onDragLeave: onDragLeave,
            onDrop: onDrop,
            onFileSelect: onFileSelect,
            onDelete: onDelete,
            getFileIcon: getFileIcon,
        };
    },
});
export default (await import('vue')).defineComponent({
    setup() {
        return {};
    },
});
; /* PartiallyEnd: #4569/main.vue */
