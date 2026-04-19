<template>
  <div class="knowledge-base">
    <div class="kb-header">
      <h3>知识库管理</h3>
      <button class="refresh-btn" @click="refreshDocuments" :disabled="loading">
        <span :class="['icon', { spinning: loading }]">&#x21bb;</span>
      </button>
    </div>

    <div class="upload-area" @dragover.prevent="onDragOver" @dragleave="onDragLeave" @drop.prevent="onDrop"
      :class="{ 'drag-over': isDragging }">
      <input ref="fileInput" type="file" accept=".md" @change="onFileSelect" hidden />
      <div class="upload-content" @click="triggerFileInput">
        <span class="upload-icon">+</span>
        <span class="upload-text">拖拽或点击上传</span>
        <span class="upload-hint">支持 MD, PDF, DOC, TXT</span>
      </div>
    </div>

    <div v-if="uploadingFile" class="upload-progress">
      <span>上传中: {{ uploadingFile }}</span>
    </div>

    <div class="document-list">
      <div v-if="documents.length === 0" class="empty-state">
        暂无文档
      </div>
      <div v-for="doc in documents" :key="doc" class="document-item">
        <div class="doc-icon">
          {{ getFileIcon(doc) }}
        </div>
        <div class="doc-info">
          <span class="doc-name" :title="doc">{{ doc }}</span>
        </div>
        <button class="delete-btn" @click="onDelete(doc)" title="删除">
          &times;
        </button>
      </div>
    </div>

    <div v-if="message" :class="['message', messageType]">
      {{ message }}
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from "vue";
import { listDocuments, uploadDocument, deleteDocument } from "../services/enterpriseApi";

const loading = ref(false);
const uploadingFile = ref("");
const isDragging = ref(false);
const documents = ref<string[]>([]);
const message = ref("");
const messageType = ref<"success" | "error">("success");
const fileInput = ref<HTMLInputElement | null>(null);

function triggerFileInput() {
  fileInput.value?.click();
}

async function refreshDocuments() {
  loading.value = true;
  message.value = "";
  try {
    const result = await listDocuments();
    documents.value = result.documents || [];
  } catch {
    showMessage("获取文档列表失败", "error");
  } finally {
    loading.value = false;
  }
}

function showMessage(msg: string, type: "success" | "error" = "success") {
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

async function onDrop(e: DragEvent) {
  isDragging.value = false;
  const files = e.dataTransfer?.files;
  if (files && files.length > 0) {
    await handleFiles(Array.from(files));
  }
}

async function onFileSelect(e: Event) {
  const input = e.target as HTMLInputElement;
  if (input.files && input.files.length > 0) {
    await handleFiles(Array.from(input.files));
    input.value = "";
  }
}

async function handleFiles(files: File[]) {
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
      } else {
        showMessage(result.message || "上传失败", "error");
      }
    } catch (err) {
      showMessage(`上传失败: ${(err as Error).message}`, "error");
    } finally {
      uploadingFile.value = "";
    }
  }
}

async function onDelete(filename: string) {
  if (!confirm(`确定要删除文档 "${filename}" 吗？`)) {
    return;
  }

  try {
    const result = await deleteDocument(filename);
    if (result.success) {
      showMessage("删除成功", "success");
      await refreshDocuments();
    } else {
      showMessage(result.message || "删除失败", "error");
    }
  } catch {
    showMessage("删除失败", "error");
  }
}

function getFileIcon(filename: string): string {
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
</script>

<style scoped>
.knowledge-base {
  padding: 12px;
}

.kb-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 12px;
}

.kb-header h3 {
  margin: 0;
  font-size: 14px;
  color: #f8fafc;
}

.refresh-btn {
  width: 28px;
  height: 28px;
  border: 1px solid rgba(148, 163, 184, 0.35);
  border-radius: 6px;
  background: rgba(15, 23, 42, 0.45);
  color: #94a3b8;
  cursor: pointer;
  display: flex;
  align-items: center;
  justify-content: center;
  transition: all 0.2s;
}

.refresh-btn:hover {
  border-color: rgba(96, 165, 250, 0.8);
  color: #f8fafc;
}

.icon {
  font-size: 14px;
  display: inline-block;
}

.icon.spinning {
  animation: spin 1s linear infinite;
}

@keyframes spin {
  from {
    transform: rotate(0deg);
  }
  to {
    transform: rotate(360deg);
  }
}

.upload-area {
  border: 2px dashed rgba(148, 163, 184, 0.35);
  border-radius: 10px;
  padding: 16px;
  text-align: center;
  cursor: pointer;
  transition: all 0.2s;
  margin-bottom: 12px;
}

.upload-area:hover,
.upload-area.drag-over {
  border-color: rgba(96, 165, 250, 0.8);
  background: rgba(59, 130, 246, 0.1);
}

.upload-content {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 4px;
}

.upload-icon {
  font-size: 24px;
  color: #60a5fa;
  font-weight: bold;
}

.upload-text {
  font-size: 13px;
  color: #cbd5e1;
}

.upload-hint {
  font-size: 11px;
  color: #64748b;
}

.upload-progress {
  padding: 8px 12px;
  background: rgba(59, 130, 246, 0.15);
  border-radius: 6px;
  font-size: 12px;
  color: #93c5fd;
  margin-bottom: 12px;
  text-align: center;
}

.document-list {
  display: flex;
  flex-direction: column;
  gap: 6px;
  max-height: 200px;
  overflow-y: auto;
}

.empty-state {
  text-align: center;
  padding: 20px;
  color: #64748b;
  font-size: 13px;
}

.document-item {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 8px 10px;
  background: rgba(30, 41, 59, 0.45);
  border: 1px solid rgba(148, 163, 184, 0.2);
  border-radius: 8px;
  transition: all 0.2s;
}

.document-item:hover {
  border-color: rgba(148, 163, 184, 0.4);
}

.doc-icon {
  width: 28px;
  height: 28px;
  border-radius: 4px;
  background: rgba(96, 165, 250, 0.2);
  color: #60a5fa;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 12px;
  font-weight: bold;
  flex-shrink: 0;
}

.doc-info {
  flex: 1;
  min-width: 0;
}

.doc-name {
  font-size: 12px;
  color: #cbd5e1;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
  display: block;
}

.delete-btn {
  width: 24px;
  height: 24px;
  border: none;
  border-radius: 4px;
  background: transparent;
  color: #f87171;
  cursor: pointer;
  font-size: 16px;
  display: flex;
  align-items: center;
  justify-content: center;
  transition: all 0.2s;
  flex-shrink: 0;
}

.delete-btn:hover {
  background: rgba(248, 113, 113, 0.15);
}

.message {
  margin-top: 10px;
  padding: 8px 12px;
  border-radius: 6px;
  font-size: 12px;
  text-align: center;
}

.message.success {
  background: rgba(34, 197, 94, 0.15);
  color: #4ade80;
}

.message.error {
  background: rgba(248, 113, 113, 0.15);
  color: #f87171;
}
</style>
