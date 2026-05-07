<template>
  <div :class="['page', { 'auth-page': !currentUser }]">
    <div class="bg-orb orb-one"></div>
    <div class="bg-orb orb-two"></div>

    <section v-if="!currentUser" class="panel login-panel">
      <div class="brand login-brand">
        <span class="brand-dot"></span>
        <div>
          <h1>Enterprise AI Agent</h1>
          <p class="subtitle">请登录后使用企业 AI 助手</p>
        </div>
      </div>
      <form class="login-form" @submit.prevent="onLogin">
        <label class="field">
          <span>账号</span>
          <input v-model="loginForm.username" autocomplete="username" placeholder="zhangsan" />
        </label>
        <label class="field">
          <span>密码</span>
          <input v-model="loginForm.password" type="password" autocomplete="current-password" placeholder="123456" />
        </label>
        <button type="submit" class="primary-btn" :disabled="loginLoading">
          {{ loginLoading ? "登录中..." : "登录" }}
        </button>
        <p v-if="loginError" class="login-error">{{ loginError }}</p>
      </form>
      <div class="demo-users">
        <span>演示账号</span>
        <button type="button" @click="fillDemoUser('zhangsan')">张三</button>
        <button type="button" @click="fillDemoUser('hr_admin')">HR</button>
        <button type="button" @click="fillDemoUser('admin')">管理员</button>
      </div>
    </section>

    <aside v-else class="panel left-panel">
      <div class="brand">
        <span class="brand-dot"></span>
        <div>
          <h1>Enterprise AI Agent</h1>
          <p class="subtitle">企业 AI 数字团队工作台</p>
        </div>
      </div>

      <section class="card user-card">
        <h2>当前账号</h2>
        <div class="user-profile">
          <span>{{ currentUser.displayName.slice(0, 1) }}</span>
          <div>
            <strong>{{ currentUser.displayName }}</strong>
            <p>{{ currentUser.role }} · {{ currentUser.employeeId || "全局权限" }}</p>
          </div>
        </div>
        <button type="button" class="secondary-btn logout-btn" @click="onLogout">退出登录</button>
      </section>

      <section class="card">
        <h2>服务类型</h2>
        <div class="mode-grid">
          <button
            v-for="item in readyCapabilities"
            :key="item.key"
            type="button"
            :class="['mode-btn', { active: selectedMode === item.key }]"
            @click="onSelectMode(item.key)"
          >
            {{ item.name }}
          </button>
        </div>
      </section>

      <section class="card">
        <h2>{{ canManageWorkflow ? "后台管理" : "员工服务" }}</h2>
        <div class="mode-grid">
          <button
            type="button"
            :class="['mode-btn', { active: isEmployeeMode }]"
            @click="onSelectEmployeeView"
          >
            员工视图
          </button>
          <button
            v-if="canManageWorkflow"
            type="button"
            :class="['mode-btn', { active: isWorkflowMode }]"
            @click="onSelectWorkflow"
          >
            审批管理
          </button>
        </div>
      </section>

      <section class="card service-status-card">
        <h2>服务状态</h2>
        <div class="health-row">
          <button type="button" class="secondary-btn" @click="onHealthCheck">
            刷新状态
          </button>
          <p :class="['status-text', healthStatusClass]">{{ healthText }}</p>
        </div>
      </section>

      <section class="card">
        <h2>知识库管理</h2>
        <KnowledgeBaseUpload />
      </section>
    </aside>

    <main v-if="currentUser" class="panel chat-panel">
      <header class="chat-header">
        <div>
          <h2>{{ workspaceTitle }}</h2>
          <p>{{ workspaceDescription }}</p>
        </div>
        <div class="header-actions">
          <span class="mode-chip">{{ workspaceModeChip }}</span>
          <span v-if="isStreaming" class="streaming-chip">
            <span class="streaming-dot"></span>
            处理中
          </span>
        </div>
      </header>

      <section v-if="!isWorkflowMode && !isEmployeeMode" class="team-cockpit">
        <div class="agent-roster">
          <article
            v-for="agent in agentRoster"
            :key="agent.code"
            :class="['agent-tile', agent.code, { active: activeAgentCodes.includes(agent.code) }]"
          >
            <span class="agent-avatar">{{ agent.avatar }}</span>
            <div>
              <strong>{{ agent.name }}</strong>
              <p>{{ agent.title }}</p>
            </div>
          </article>
        </div>
        <div v-if="activeTeamResponse" class="route-strip">
          <div>
            <span>意图</span>
            <strong>{{ activeTeamResponse?.intentType }}</strong>
          </div>
          <div>
            <span>风险</span>
            <strong :class="['risk-text', activeTeamResponse?.sentiment.riskLevel.toLowerCase()]">
              {{ activeTeamResponse?.sentiment.riskLevel }} · {{ activeTeamResponse?.sentiment.score }}
            </strong>
          </div>
          <div>
            <span>路由</span>
            <strong>{{ activeTeamResponse?.route.priority }} / {{ activeTeamResponse?.route.assignee }}</strong>
          </div>
          <div>
            <span>耗时</span>
            <strong>{{ activeTeamResponse?.elapsedMs }}ms</strong>
          </div>
        </div>
        <div v-else-if="selectedMode === 'mcp'" class="mcp-strip">
          <div>
            <span>MCP Server</span>
            <strong>{{ mcpStatus?.serverName || "未连接" }}</strong>
          </div>
          <div>
            <span>工具数量</span>
            <strong>{{ mcpStatus?.toolCount ?? mcpTools.length }}</strong>
          </div>
          <button type="button" class="secondary-btn" @click="refreshMcpInfo">刷新 MCP</button>
        </div>
      </section>

      <section v-else-if="isEmployeeMode" class="employee-panel">
        <aside v-if="canManageWorkflow" class="employee-directory">
          <div class="console-section-title">
            <span>EMPLOYEE DIRECTORY</span>
            <strong>员工档案</strong>
          </div>
          <div class="directory-search">
            <input
              v-model.trim="employeeSearch"
              placeholder="搜索工号、姓名、邮箱、岗位"
              @keydown.enter.prevent="refreshEmployeeDirectory"
            />
            <select v-model="employeeDepartmentFilter" @change="refreshEmployeeDirectory">
              <option value="all">全部部门</option>
              <option v-for="department in employeeDepartments" :key="department" :value="department">
                {{ department }}
              </option>
            </select>
            <button type="button" class="secondary-btn" :disabled="employeeLoading" @click="refreshEmployeeDirectory">
              查询
            </button>
          </div>
          <div class="directory-list">
            <button
              v-for="employee in employeeDirectory"
              :key="employee.employeeId"
              type="button"
              :class="['directory-item', { active: selectedEmployeeId === employee.employeeId }]"
              @click="selectEmployee(employee.employeeId)"
            >
              <span>{{ employee.name.slice(0, 1) }}</span>
              <div>
                <strong>{{ employee.name }} · {{ employee.employeeId }}</strong>
                <p>{{ employee.department }} / {{ employee.position }}</p>
              </div>
            </button>
            <p v-if="!employeeLoading && employeeDirectory.length === 0" class="empty-hint">暂无员工数据</p>
          </div>
        </aside>

        <section class="employee-overview">
          <div class="employee-toolbar">
            <div>
              <span>{{ canManageWorkflow ? "EMPLOYEE VIEW" : "SELF SERVICE" }}</span>
              <h3>{{ employeeOverview?.employee?.name || "员工视图" }}</h3>
            </div>
            <button type="button" class="secondary-btn" :disabled="employeeLoading" @click="refreshEmployeeView">
              {{ employeeLoading ? "刷新中..." : "刷新" }}
            </button>
          </div>

          <p v-if="employeeMessage" class="workflow-message error">{{ employeeMessage }}</p>

          <template v-if="employeeOverview?.employee">
            <div class="employee-profile-card">
              <div class="employee-avatar-lg">{{ employeeOverview.employee.name.slice(0, 1) }}</div>
              <div>
                <h3>{{ employeeOverview.employee.name }}</h3>
                <p>{{ employeeOverview.employee.employeeId }} · {{ employeeOverview.employee.department }} · {{ employeeOverview.employee.position }}</p>
                <div class="employee-contact">
                  <span>{{ employeeOverview.employee.email }}</span>
                  <span>{{ employeeOverview.employee.phone }}</span>
                  <span>入职 {{ employeeOverview.employee.joinDate }}</span>
                </div>
              </div>
            </div>

            <div class="admin-kpis employee-kpis">
              <article>
                <span>待审批</span>
                <strong>{{ employeeOverview.summary.pending }}</strong>
              </article>
              <article>
                <span>已通过</span>
                <strong>{{ employeeOverview.summary.approved }}</strong>
              </article>
              <article>
                <span>已驳回</span>
                <strong>{{ employeeOverview.summary.rejected }}</strong>
              </article>
              <article>
                <span>申请总数</span>
                <strong>{{ employeeOverview.summary.total }}</strong>
              </article>
            </div>

            <div class="leave-balance-grid">
              <article>
                <span>年假</span>
                <strong>{{ employeeOverview.leaveBalance?.annualLeave ?? 0 }} 天</strong>
              </article>
              <article>
                <span>病假</span>
                <strong>{{ employeeOverview.leaveBalance?.sickLeave ?? 0 }} 天</strong>
              </article>
              <article>
                <span>婚假</span>
                <strong>{{ employeeOverview.leaveBalance?.marriageLeave ?? 0 }} 天</strong>
              </article>
              <article>
                <span>产假</span>
                <strong>{{ employeeOverview.leaveBalance?.maternityLeave ?? 0 }} 天</strong>
              </article>
            </div>

            <div class="application-board">
              <div class="console-section-title">
                <span>APPLICATIONS</span>
                <strong>申请记录</strong>
              </div>
              <div class="application-list">
                <article
                  v-for="application in employeeOverview.applications"
                  :key="`${application.type}-${application.applicationId}`"
                  class="application-card"
                >
                  <div>
                    <span :class="['status-pill', application.status.toLowerCase()]">
                      {{ statusLabel(application.status) }}
                    </span>
                    <strong>{{ workflowTypeLabel(application.type) }} · {{ application.applicationId }}</strong>
                    <p>{{ applicationSummary(application) }}</p>
                    <p>{{ application.description }}</p>
                  </div>
                  <footer>
                    <span>提交 {{ application.applyDate }}</span>
                    <span v-if="application.reviewedAt">审批 {{ application.reviewedAt }}</span>
                    <span v-if="application.reviewComment">意见：{{ application.reviewComment }}</span>
                  </footer>
                </article>
                <p v-if="employeeOverview.applications.length === 0" class="empty-hint">暂无申请记录</p>
              </div>
            </div>
          </template>
        </section>
      </section>

      <section v-else class="workflow-panel">
        <div class="admin-kpis">
          <article>
            <span>待审批</span>
            <strong>{{ workflowStats.pending }}</strong>
          </article>
          <article>
            <span>已通过</span>
            <strong>{{ workflowStats.approved }}</strong>
          </article>
          <article>
            <span>已驳回</span>
            <strong>{{ workflowStats.rejected }}</strong>
          </article>
          <article>
            <span>申请总数</span>
            <strong>{{ workflowStats.total }}</strong>
          </article>
        </div>

        <div class="admin-console">
          <section class="admin-list-panel">
            <div class="workflow-toolbar">
              <div class="workflow-filters">
                <label>
                  <span>类型</span>
                  <select v-model="workflowTypeFilter" @change="refreshWorkflowApplications">
                    <option value="all">全部</option>
                    <option value="leave">请假</option>
                    <option value="reimbursement">报销</option>
                  </select>
                </label>
                <label>
                  <span>状态</span>
                  <select v-model="workflowStatusFilter" @change="refreshWorkflowApplications">
                    <option value="all">全部</option>
                    <option value="PENDING">待审批</option>
                    <option value="APPROVED">已通过</option>
                    <option value="REJECTED">已驳回</option>
                  </select>
                </label>
              </div>
              <button type="button" class="secondary-btn" :disabled="workflowLoading" @click="refreshWorkflowApplications">
                {{ workflowLoading ? "刷新中..." : "刷新" }}
              </button>
            </div>

            <p v-if="workflowMessage" :class="['workflow-message', workflowMessageType]">{{ workflowMessage }}</p>

            <div class="workflow-table-wrap">
              <table class="workflow-table">
                <thead>
                  <tr>
                    <th>申请</th>
                    <th>员工</th>
                    <th>内容</th>
                    <th>状态</th>
                    <th>审批</th>
                    <th>操作</th>
                  </tr>
                </thead>
                <tbody>
                  <tr v-if="!workflowLoading && workflowApplications.length === 0">
                    <td colspan="6" class="empty-cell">暂无符合条件的申请</td>
                  </tr>
                  <tr
                    v-for="application in workflowApplications"
                    :key="`${application.type}-${application.applicationId}`"
                    :class="{ selected: selectedWorkflowApplication?.applicationId === application.applicationId && selectedWorkflowApplication?.type === application.type }"
                    @click="selectedWorkflowApplication = application"
                  >
                    <td>
                      <strong>{{ application.applicationId }}</strong>
                      <span>{{ workflowTypeLabel(application.type) }} · {{ application.applyDate }}</span>
                    </td>
                    <td>
                      <strong>{{ application.employeeName || application.employeeId }}</strong>
                      <span>{{ application.department || application.employeeId }}</span>
                    </td>
                    <td>
                      <strong>{{ applicationSummary(application) }}</strong>
                      <span>{{ application.description }}</span>
                    </td>
                    <td>
                      <span :class="['status-pill', application.status.toLowerCase()]">
                        {{ statusLabel(application.status) }}
                      </span>
                    </td>
                    <td>
                      <strong>{{ application.reviewerId || "未审批" }}</strong>
                      <span>{{ application.reviewComment || "暂无审批意见" }}</span>
                    </td>
                    <td>
                      <div class="workflow-actions" @click.stop>
                        <button
                          v-if="application.status === 'PENDING'"
                          type="button"
                          class="approve-btn"
                          @click="openReviewDialog(application, 'APPROVED')"
                        >
                          通过
                        </button>
                        <button
                          v-if="application.status === 'PENDING'"
                          type="button"
                          class="reject-btn"
                          @click="openReviewDialog(application, 'REJECTED')"
                        >
                          驳回
                        </button>
                        <span v-if="application.status !== 'PENDING'" class="done-text">已处理</span>
                      </div>
                    </td>
                  </tr>
                </tbody>
              </table>
            </div>
          </section>

          <aside class="admin-detail-panel">
            <div class="console-section-title">
              <span>APPLICATION DETAIL</span>
              <strong>审批详情</strong>
            </div>
            <template v-if="selectedWorkflowApplication">
              <div class="detail-row">
                <span>申请编号</span>
                <strong>{{ selectedWorkflowApplication.applicationId }}</strong>
              </div>
              <div class="detail-row">
                <span>员工</span>
                <strong>{{ selectedWorkflowApplication.employeeName }} / {{ selectedWorkflowApplication.employeeId }}</strong>
              </div>
              <div class="detail-row">
                <span>业务类型</span>
                <strong>{{ workflowTypeLabel(selectedWorkflowApplication.type) }} · {{ selectedWorkflowApplication.applicationType }}</strong>
              </div>
              <div class="detail-row">
                <span>申请内容</span>
                <strong>{{ applicationSummary(selectedWorkflowApplication) }}</strong>
              </div>
              <div class="detail-row">
                <span>说明</span>
                <p>{{ selectedWorkflowApplication.description }}</p>
              </div>
              <div class="detail-row">
                <span>审批状态</span>
                <strong>{{ statusLabel(selectedWorkflowApplication.status) }}</strong>
              </div>
              <div class="detail-row">
                <span>审批意见</span>
                <p>{{ selectedWorkflowApplication.reviewComment || "暂无审批意见" }}</p>
              </div>
            </template>
            <p v-else class="empty-hint">选择一条申请查看详情</p>
          </aside>
        </div>
      </section>

      <div v-if="!isWorkflowMode && !isEmployeeMode" class="quick-prompts">
        <button
          v-for="prompt in quickPrompts"
          :key="prompt"
          type="button"
          class="prompt-chip"
          @click="onUsePrompt(prompt)"
        >
          {{ prompt }}
        </button>
      </div>

      <div v-if="!isWorkflowMode && !isEmployeeMode" class="message-list" ref="messageListRef">
        <article
          v-for="message in messages"
          :key="message.id"
          :class="['message', message.role, { streaming: message.streaming }]"
        >
          <header>{{ roleLabelMap[message.role] }}</header>
          <pre v-if="message.isJson">{{ message.text }}</pre>
          <template v-else-if="message.role === 'assistant'">
            <div
              class="markdown-body"
              v-html="renderAssistantMarkdown(getAnswerText(message.text))"
            ></div>

            <details v-if="getReferenceText(message.text)" class="fold-card reference-card text-reference-card">
              <summary>
                <span>参考来源</span>
                <strong>查看引用依据</strong>
              </summary>
              <div
                class="markdown-body reference-markdown"
                v-html="renderAssistantMarkdown(getReferenceText(message.text))"
              ></div>
            </details>
          </template>
          <p v-else>{{ message.text }}</p>
          <span v-if="message.streaming" class="typing-cursor"></span>
        </article>
        <div v-if="isStreaming" class="thinking-indicator">
          <span class="thinking-dot"></span>
          <span class="thinking-dot"></span>
          <span class="thinking-dot"></span>
        </div>
      </div>

      <footer v-if="!isWorkflowMode && !isEmployeeMode" class="chat-input">
        <textarea
          v-model.trim="inputMessage"
          ref="textareaRef"
          placeholder="请输入你的问题，例如：我想申请年假需要什么流程？"
          :disabled="isStreaming"
          @keydown.enter.exact.prevent="onSend"
        />
        <div class="input-actions">
          <span class="input-hint">
            {{ isStreaming ? '等待回复...' : 'Enter 发送，Shift + Enter 换行' }}
          </span>
          <button
            type="button"
            class="primary-btn"
            :disabled="isStreaming || !inputMessage"
            @click="onSend"
          >
            {{ isStreaming ? '处理中...' : '发送' }}
          </button>
        </div>
      </footer>
    </main>

    <div v-if="reviewDialog.application" class="modal-backdrop">
      <section class="review-dialog">
        <header>
          <div>
            <span>{{ reviewDialog.decision === 'APPROVED' ? '审批通过' : '审批驳回' }}</span>
            <strong>{{ reviewDialog.application.applicationId }}</strong>
          </div>
          <button type="button" class="icon-close-btn" @click="closeReviewDialog">×</button>
        </header>
        <textarea
          v-model.trim="reviewComment"
          placeholder="请输入审批意见"
          :disabled="reviewSubmitting"
        ></textarea>
        <footer>
          <button type="button" class="secondary-btn" :disabled="reviewSubmitting" @click="closeReviewDialog">
            取消
          </button>
          <button
            type="button"
            class="primary-btn"
            :disabled="reviewSubmitting || !reviewComment"
            @click="submitReview"
          >
            {{ reviewSubmitting ? "提交中..." : "确认提交" }}
          </button>
        </footer>
      </section>
    </div>
  </div>
</template>

<script setup lang="ts">
import DOMPurify from "dompurify";
import { marked } from "marked";
import { computed, ref, nextTick, onMounted, onUnmounted } from "vue";
import { CAPABILITIES } from "../config/capabilities";
import {
  checkHealth,
  clearAuthToken,
  getAuthToken,
  getCurrentUser,
  getEmployeeOverview,
  getMyEmployeeOverview,
  requestEnterprise,
  requestDigitalTeam,
  getMcpStatus,
  listEmployees,
  login,
  listMcpTools,
  listWorkflowApplications,
  reviewLeaveApplication,
  reviewReimbursementApplication,
  createStreamController
} from "../services/enterpriseApi";
import KnowledgeBaseUpload from "../components/KnowledgeBaseUpload.vue";
import type {
  ChatMessage,
  DigitalTeamResponse,
  EmployeeCard,
  EmployeeOverviewResponse,
  McpStatus,
  McpToolCard,
  ReadyCapability,
  UserProfile,
  WorkflowApplication
} from "../types/enterprise";

const capabilities = CAPABILITIES;
const readyCapabilities = capabilities.filter((item) => item.status === "ready");
marked.setOptions({
  gfm: true,
  breaks: true
});

const selectedMode = ref<ReadyCapability>("team-chat");
const chatId = ref("default-user");
const inputMessage = ref("");
const isStreaming = ref(false);
const topK = 5;
const healthText = ref("后端状态未检查");
const mcpStatus = ref<McpStatus | null>(null);
const mcpTools = ref<McpToolCard[]>([]);
const currentUser = ref<UserProfile | null>(null);
const isWorkflowMode = ref(false);
const isEmployeeMode = ref(false);
const loginLoading = ref(false);
const loginError = ref("");
const loginForm = ref({
  username: "zhangsan",
  password: "123456"
});
const messages = ref<ChatMessage[]>([
  {
    id: createClientId(),
    role: "system",
    text: "欢迎使用企业 AI 助手。请选择服务类型后发送问题。"
  }
]);
const workflowTypeFilter = ref<"all" | "leave" | "reimbursement">("all");
const workflowStatusFilter = ref<"all" | "PENDING" | "APPROVED" | "REJECTED">("PENDING");
const workflowApplications = ref<WorkflowApplication[]>([]);
const workflowAllApplications = ref<WorkflowApplication[]>([]);
const selectedWorkflowApplication = ref<WorkflowApplication | null>(null);
const workflowLoading = ref(false);
const workflowMessage = ref("");
const workflowMessageType = ref<"success" | "error" | "idle">("idle");
const employeeDirectory = ref<EmployeeCard[]>([]);
const employeeOverview = ref<EmployeeOverviewResponse | null>(null);
const selectedEmployeeId = ref("");
const employeeSearch = ref("");
const employeeDepartmentFilter = ref("all");
const employeeLoading = ref(false);

function createClientId(): string {
  if (typeof crypto !== "undefined" && typeof crypto.randomUUID === "function") {
    return crypto.randomUUID();
  }
  return `id-${Date.now()}-${Math.random().toString(16).slice(2)}`;
}
const employeeMessage = ref("");
const reviewDialog = ref<{
  application: WorkflowApplication | null;
  decision: "APPROVED" | "REJECTED";
}>({
  application: null,
  decision: "APPROVED"
});
const reviewComment = ref("");
const reviewSubmitting = ref(false);

const messageListRef = ref<HTMLElement | null>(null);
const textareaRef = ref<HTMLTextAreaElement | null>(null);
let currentStreamController: ReturnType<typeof createStreamController> | null = null;

const roleLabelMap: Record<ChatMessage["role"], string> = {
  user: "我",
  assistant: "AI 助手",
  system: "系统"
};

const agentRoster = [
  { code: "knowledge", avatar: "KB", name: "KNOWLEDGE", title: "知识库管理员" },
  { code: "responder", avatar: "RE", name: "RESPONDER", title: "智能响应员" },
  { code: "analyzer", avatar: "AZ", name: "ANALYZER", title: "风险分析官" },
  { code: "router", avatar: "RT", name: "ROUTER", title: "任务路由员" },
  { code: "tracker", avatar: "TK", name: "TRACKER", title: "满意度追踪员" }
] as const;

const currentCapability = computed(() =>
  capabilities.find((item) => item.key === selectedMode.value)
);

const canManageWorkflow = computed(() =>
  currentUser.value?.role === "HR" || currentUser.value?.role === "ADMIN"
);

const workspaceTitle = computed(() => {
  if (isWorkflowMode.value) return "审批管理后台";
  if (isEmployeeMode.value) return canManageWorkflow.value ? "员工管理视图" : "我的员工视图";
  return currentCapability.value?.name;
});

const workspaceDescription = computed(() => {
  if (isWorkflowMode.value) return "集中处理请假与报销申请，审批结果实时写入 MySQL 并进入审计日志。";
  if (isEmployeeMode.value) return canManageWorkflow.value
    ? "查看员工档案、假期余额和申请进度，适合 HR/Admin 日常管理。"
    : "查看个人资料、假期余额和业务申请进度。";
  return currentCapability.value?.description;
});

const workspaceModeChip = computed(() => {
  if (isWorkflowMode.value) return "admin";
  if (isEmployeeMode.value) return "employee";
  return selectedMode.value;
});

const workflowStats = computed(() => ({
  pending: workflowAllApplications.value.filter((item) => item.status === "PENDING").length,
  approved: workflowAllApplications.value.filter((item) => item.status === "APPROVED").length,
  rejected: workflowAllApplications.value.filter((item) => item.status === "REJECTED").length,
  total: workflowAllApplications.value.length
}));

const employeeDepartments = computed(() =>
  [...new Set(employeeDirectory.value.map((item) => item.department).filter(Boolean))].sort()
);

const activeTeamResponse = computed<DigitalTeamResponse | null>(() => {
  if (selectedMode.value !== "team-chat") {
    return null;
  }
  const latest = [...messages.value].reverse().find((item) => item.teamResponse);
  return latest?.teamResponse ?? null;
});

const activeAgentCodes = computed(() => {
  if (activeTeamResponse.value) {
    return activeTeamResponse.value.steps.map((item) => item.code);
  }
  if (selectedMode.value === "tool-chat") {
    return ["responder", "router"];
  }
  if (selectedMode.value === "mcp") {
    return ["knowledge", "responder", "router"];
  }
  if (selectedMode.value === "rag-chat") {
    return ["knowledge", "responder"];
  }
  if (selectedMode.value === "ticket") {
    return ["router", "tracker"];
  }
  return ["responder"];
});

const healthStatusClass = computed(() => {
  if (healthText.value.includes("在线")) {
    return "online";
  }
  if (healthText.value.includes("不可用")) {
    return "offline";
  }
  return "idle";
});

const quickPromptsMap: Record<ReadyCapability, string[]> = {
  "team-chat": [
    "我想申请年假，但不确定还剩几天，帮我处理一下",
    "报销一直没人处理，我有点着急，帮我看看该怎么办",
    "请按公司制度说明差旅报销流程，并判断是否需要转人工"
  ],
  chat: [
    "请介绍一下你能提供哪些帮助",
    "我想申请年假，流程是什么？",
    "报销打车费需要准备什么材料？"
  ],
  "rag-chat": [
    "请按公司制度说明请假审批流程",
    "差旅报销金额上限是多少？",
    "报销单据最晚什么时候提交？"
  ],
  ticket: [
    "帮我生成一个办公用品申领工单",
    "帮我整理请假申请的处理工单",
    "帮我创建报销异常处理工单"
  ],
  "tool-chat": [
    "帮我查询员工 E001 的基本信息",
    "帮员工 E001 申请 3 天年假，开始日期 2026-05-01",
    "今天是几号星期几？帮我算一下到月底还有多少个工作日"
  ],
  mcp: [
    "列出当前 MCP Server 上可用的工具",
    "通过 MCP 查询员工 E001 的基本信息",
    "通过 MCP 帮员工 E001 查询假期余额"
  ]
};
const quickPrompts = computed(() => quickPromptsMap[selectedMode.value]);

function appendMessage(message: Omit<ChatMessage, "id">): void {
  messages.value.push({
    id: createClientId(),
    ...message
  });
}

function onUsePrompt(prompt: string): void {
  inputMessage.value = prompt;
  textareaRef.value?.focus();
}

function fillDemoUser(username: string): void {
  loginForm.value.username = username;
  loginForm.value.password = "123456";
}

async function onLogin(): Promise<void> {
  loginLoading.value = true;
  loginError.value = "";
  try {
    const result = await login(loginForm.value.username, loginForm.value.password);
    currentUser.value = result.user;
    chatId.value = result.user.username;
    messages.value = [
      {
        id: createClientId(),
        role: "system",
        text: `欢迎回来，${result.user.displayName}。企业 AI 助手已连接你的账号权限。`
      }
    ];
    await onHealthCheck();
  } catch (error) {
    loginError.value = extractErrorMessage(error);
  } finally {
    loginLoading.value = false;
  }
}

function onLogout(): void {
  clearAuthToken();
  currentUser.value = null;
  isWorkflowMode.value = false;
  isEmployeeMode.value = false;
  chatId.value = "default-user";
  mcpStatus.value = null;
  mcpTools.value = [];
  workflowApplications.value = [];
  workflowAllApplications.value = [];
  selectedWorkflowApplication.value = null;
  employeeDirectory.value = [];
  employeeOverview.value = null;
  selectedEmployeeId.value = "";
}

function onSelectMode(mode: ReadyCapability): void {
  isWorkflowMode.value = false;
  isEmployeeMode.value = false;
  selectedMode.value = mode;
  if (mode === "mcp") {
    refreshMcpInfo();
  }
}

async function onSelectWorkflow(): Promise<void> {
  isWorkflowMode.value = true;
  isEmployeeMode.value = false;
  await loadWorkflowApplications();
}

async function onSelectEmployeeView(): Promise<void> {
  isWorkflowMode.value = false;
  isEmployeeMode.value = true;
  await refreshEmployeeView();
}

async function loadWorkflowApplications(clearMessage: boolean = true): Promise<void> {
  if (!canManageWorkflow.value) {
    return;
  }
  workflowLoading.value = true;
  if (clearMessage) {
    workflowMessage.value = "";
    workflowMessageType.value = "idle";
  }
  try {
    const [result, allResult] = await Promise.all([
      listWorkflowApplications(workflowTypeFilter.value, workflowStatusFilter.value),
      listWorkflowApplications("all", "all")
    ]);
    workflowApplications.value = result.applications;
    workflowAllApplications.value = allResult.applications;
    if (!selectedWorkflowApplication.value && result.applications.length > 0) {
      selectedWorkflowApplication.value = result.applications[0];
    }
    if (selectedWorkflowApplication.value) {
      selectedWorkflowApplication.value = allResult.applications.find((item) =>
        item.applicationId === selectedWorkflowApplication.value?.applicationId
        && item.type === selectedWorkflowApplication.value?.type
      ) ?? result.applications[0] ?? null;
    }
  } catch (error) {
    workflowMessage.value = `审批列表加载失败：${extractErrorMessage(error)}`;
    workflowMessageType.value = "error";
  } finally {
    workflowLoading.value = false;
  }
}

async function refreshWorkflowApplications(): Promise<void> {
  await loadWorkflowApplications();
}

async function refreshEmployeeDirectory(): Promise<void> {
  if (!canManageWorkflow.value) {
    return;
  }
  employeeLoading.value = true;
  employeeMessage.value = "";
  try {
    const result = await listEmployees(employeeSearch.value, employeeDepartmentFilter.value);
    employeeDirectory.value = result.employees;
    if (!selectedEmployeeId.value && result.employees.length > 0) {
      selectedEmployeeId.value = currentUser.value?.employeeId || result.employees[0].employeeId;
    }
    if (selectedEmployeeId.value) {
      await loadEmployeeOverview(selectedEmployeeId.value);
    }
  } catch (error) {
    employeeMessage.value = `员工列表加载失败：${extractErrorMessage(error)}`;
  } finally {
    employeeLoading.value = false;
  }
}

async function refreshEmployeeView(): Promise<void> {
  employeeLoading.value = true;
  employeeMessage.value = "";
  try {
    if (canManageWorkflow.value) {
      const result = await listEmployees(employeeSearch.value, employeeDepartmentFilter.value);
      employeeDirectory.value = result.employees;
      if (!selectedEmployeeId.value) {
        selectedEmployeeId.value = currentUser.value?.employeeId || result.employees[0]?.employeeId || "";
      }
      if (selectedEmployeeId.value) {
        employeeOverview.value = await getEmployeeOverview(selectedEmployeeId.value);
      } else {
        employeeOverview.value = null;
      }
      return;
    }
    employeeOverview.value = await getMyEmployeeOverview();
    selectedEmployeeId.value = employeeOverview.value.employee?.employeeId || "";
  } catch (error) {
    employeeMessage.value = `员工视图加载失败：${extractErrorMessage(error)}`;
  } finally {
    employeeLoading.value = false;
  }
}

async function loadEmployeeOverview(employeeId: string): Promise<void> {
  try {
    selectedEmployeeId.value = employeeId;
    employeeOverview.value = await getEmployeeOverview(employeeId);
  } catch (error) {
    employeeMessage.value = `员工详情加载失败：${extractErrorMessage(error)}`;
  }
}

async function selectEmployee(employeeId: string): Promise<void> {
  employeeLoading.value = true;
  employeeMessage.value = "";
  await loadEmployeeOverview(employeeId);
  employeeLoading.value = false;
}

function openReviewDialog(application: WorkflowApplication, decision: "APPROVED" | "REJECTED"): void {
  reviewDialog.value = { application, decision };
  reviewComment.value = decision === "APPROVED" ? "审批通过" : "";
}

function closeReviewDialog(): void {
  if (reviewSubmitting.value) {
    return;
  }
  reviewDialog.value = { application: null, decision: "APPROVED" };
  reviewComment.value = "";
}

async function submitReview(): Promise<void> {
  const application = reviewDialog.value.application;
  if (!application || !reviewComment.value) {
    return;
  }
  reviewSubmitting.value = true;
  workflowMessage.value = "";
  workflowMessageType.value = "idle";
  try {
    const request = {
      decision: reviewDialog.value.decision,
      comment: reviewComment.value
    };
    const result = application.type === "leave"
      ? await reviewLeaveApplication(application.applicationId, request)
      : await reviewReimbursementApplication(application.applicationId, request);
    workflowMessage.value = result.message;
    workflowMessageType.value = "success";
    reviewDialog.value = { application: null, decision: "APPROVED" };
    reviewComment.value = "";
    await loadWorkflowApplications(false);
  } catch (error) {
    workflowMessage.value = `审批提交失败：${extractErrorMessage(error)}`;
    workflowMessageType.value = "error";
  } finally {
    reviewSubmitting.value = false;
  }
}

function workflowTypeLabel(type: string): string {
  return type === "leave" ? "请假" : "报销";
}

function statusLabel(status: string): string {
  if (status === "PENDING") return "待审批";
  if (status === "APPROVED") return "已通过";
  if (status === "REJECTED") return "已驳回";
  return status;
}

function applicationSummary(application: WorkflowApplication): string {
  if (application.type === "leave") {
    return `${application.applicationType} · ${application.startDate} 至 ${application.endDate} · ${application.days ?? 0} 天`;
  }
  return `${application.applicationType} · ${Number(application.amount ?? 0).toFixed(2)} 元`;
}

async function refreshMcpInfo(): Promise<void> {
  try {
    const [status, tools] = await Promise.all([getMcpStatus(), listMcpTools()]);
    mcpStatus.value = status;
    mcpTools.value = tools;
  } catch (error) {
    appendMessage({
      role: "assistant",
      text: `MCP 信息加载失败：${extractErrorMessage(error)}`
    });
  }
}

function renderAssistantMarkdown(markdown: string): string {
  if (!markdown) return "";
  const rendered = marked.parse(markdown) as string;
  return DOMPurify.sanitize(rendered);
}

function splitReferenceSection(text: string): { answer: string; reference: string } {
  const referenceMarker = "【参考来源】";
  const markerIndex = text.lastIndexOf(referenceMarker);
  if (markerIndex === -1) {
    return { answer: text, reference: "" };
  }
  if (markerIndex === 0) {
    return splitLeadingReferenceSection(text, referenceMarker);
  }

  const beforeMarker = text.slice(0, markerIndex);
  const separatorIndex = beforeMarker.lastIndexOf("---");
  const answer = separatorIndex >= 0
    ? beforeMarker.slice(0, separatorIndex).trim()
    : beforeMarker.trim();

  if (!answer) {
    return { answer: text, reference: "" };
  }

  return {
    answer,
    reference: text.slice(markerIndex).trim()
  };
}

function splitLeadingReferenceSection(text: string, referenceMarker: string): { answer: string; reference: string } {
  const lines = text.split(/\r?\n/);
  let answerStartIndex = -1;
  let hasReferenceItem = false;

  for (let index = 1; index < lines.length; index += 1) {
    const line = lines[index].trim();
    if (!line) {
      continue;
    }
    if (isReferenceLine(line)) {
      hasReferenceItem = true;
      continue;
    }
    if (hasReferenceItem) {
      answerStartIndex = index;
      break;
    }
  }

  if (answerStartIndex === -1) {
    return { answer: "", reference: text.trim() };
  }

  const reference = lines.slice(0, answerStartIndex).join("\n").trim();
  const answer = lines.slice(answerStartIndex).join("\n").trim();
  return {
    answer,
    reference: reference || referenceMarker
  };
}

function isReferenceLine(line: string): boolean {
  return /^\[\d+\]\s+/.test(line)
    || /^\d+\.\s+\[/.test(line)
    || /^[-*]\s+/.test(line);
}

function getAnswerText(text: string): string {
  return splitReferenceSection(text).answer;
}

function getReferenceText(text: string): string {
  return splitReferenceSection(text).reference;
}

async function onHealthCheck(): Promise<void> {
  try {
    healthText.value = "检测中...";
    const result = await checkHealth();
    healthText.value = `后端在线：${result.trim() || "OK"}`;
  } catch (error) {
    healthText.value = `后端不可用：${extractErrorMessage(error)}`;
  }
}

function onSend(): void {
  if (!inputMessage.value || isStreaming.value) {
    return;
  }

  const currentInput = inputMessage.value;
  inputMessage.value = "";
  appendMessage({ role: "user", text: currentInput });

  if (selectedMode.value === "ticket"
      || selectedMode.value === "team-chat"
      || selectedMode.value === "tool-chat"
      || selectedMode.value === "mcp") {
    handleNonStreamRequest(currentInput);
    return;
  }

  handleStreamRequest(currentInput);
}

async function handleNonStreamRequest(currentInput: string): Promise<void> {
  isStreaming.value = true;
  try {
    if (selectedMode.value === "team-chat") {
      const teamResponse = await requestDigitalTeam(
        currentInput,
        chatId.value || "default-user",
        topK
      );
      appendMessage({
        role: "assistant",
        text: teamResponse.answer,
        teamResponse
      });
      await nextTick();
      scrollToBottom();
      return;
    }

    const responseText = await requestEnterprise(
      selectedMode.value,
      currentInput,
      chatId.value || "default-user"
    );
    appendMessage({
      role: "assistant",
      text: responseText,
      isJson: selectedMode.value === "ticket"
    });
    await nextTick();
    scrollToBottom();
  } catch (error) {
    appendMessage({
      role: "assistant",
      text: `调用失败：${extractErrorMessage(error)}`
    });
  } finally {
    isStreaming.value = false;
  }
}

function handleStreamRequest(currentInput: string): void {
  const assistantMessageId = createClientId();
  const assistantMessage: ChatMessage = {
    id: assistantMessageId,
    role: "assistant",
    text: "",
    streaming: true
  };
  messages.value.push(assistantMessage);

  isStreaming.value = true;

  if (currentStreamController) {
    currentStreamController.close();
  }
  currentStreamController = createStreamController();

  currentStreamController.stream(
    selectedMode.value,
    currentInput,
    chatId.value || "default-user",
    {
      onChunk: (chunk: string) => {
        const msg = messages.value.find((m) => m.id === assistantMessageId);
        if (msg) {
          msg.text += chunk;
          nextTick(() => {
            scrollToBottom();
          });
        }
      },
      onDone: (fullContent: string) => {
        const msg = messages.value.find((m) => m.id === assistantMessageId);
        if (msg) {
          msg.text = fullContent;
          msg.streaming = false;
        }
        isStreaming.value = false;
        nextTick(() => {
          scrollToBottom();
        });
      },
      onError: (error: Error) => {
        const msg = messages.value.find((m) => m.id === assistantMessageId);
        if (msg) {
          msg.text = `流式请求失败：${error.message}`;
          msg.streaming = false;
        }
        isStreaming.value = false;
      }
    },
      topK
  );
}

function scrollToBottom(): void {
  const messageList = messageListRef.value;
  if (messageList) {
    messageList.scrollTop = messageList.scrollHeight;
  }
}

function extractErrorMessage(error: unknown): string {
  if (typeof error === "object" && error !== null && "response" in error) {
    const response = (error as { response?: { data?: { message?: string } } }).response;
    if (response?.data?.message) {
      return response.data.message;
    }
  }
  if (error instanceof Error) {
    return error.message;
  }
  return "未知错误";
}

onMounted(async () => {
  if (!getAuthToken()) {
    return;
  }
  try {
    currentUser.value = await getCurrentUser();
    chatId.value = currentUser.value.username;
    await onHealthCheck();
    if (isWorkflowMode.value && canManageWorkflow.value) {
      await loadWorkflowApplications();
    }
  } catch {
    clearAuthToken();
    currentUser.value = null;
  }
});

onUnmounted(() => {
  if (currentStreamController) {
    currentStreamController.close();
  }
});
</script>

<style scoped>
.auth-page {
  grid-template-columns: minmax(320px, 460px);
  align-items: center;
  justify-content: center;
}

.login-panel {
  z-index: 1;
  padding: 28px;
}

.login-brand {
  margin-bottom: 22px;
}

.login-form {
  display: grid;
  gap: 14px;
}

.login-form .primary-btn {
  width: 100%;
}

.login-error {
  margin: 0;
  color: #f87171;
  font-size: 13px;
  line-height: 1.5;
}

.demo-users {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
  align-items: center;
  margin-top: 18px;
  padding-top: 18px;
  border-top: 1px solid rgba(148, 163, 184, 0.14);
}

.demo-users span {
  color: #64748b;
  font-size: 12px;
}

.demo-users button {
  border: 1px solid rgba(148, 163, 184, 0.18);
  border-radius: 999px;
  padding: 7px 10px;
  color: #cbd5e1;
  background: rgba(30, 41, 59, 0.52);
  font-size: 12px;
}

.demo-users button:hover {
  border-color: rgba(96, 165, 250, 0.45);
  color: #f8fafc;
}

.user-card {
  display: grid;
  gap: 12px;
}

.user-profile {
  display: flex;
  align-items: center;
  gap: 10px;
}

.user-profile span {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  width: 36px;
  height: 36px;
  border-radius: 10px;
  color: #ffffff;
  background: linear-gradient(135deg, #2563eb, #10b981);
  font-weight: 700;
}

.user-profile strong {
  display: block;
  color: #f8fafc;
  font-size: 14px;
}

.user-profile p {
  margin: 3px 0 0;
  color: #64748b;
  font-size: 12px;
}

.logout-btn {
  width: 100%;
}

.team-cockpit {
  display: grid;
  gap: 12px;
  padding: 14px 20px;
  border-bottom: 1px solid rgba(148, 163, 184, 0.12);
  background: rgba(15, 23, 42, 0.28);
}

.agent-roster {
  display: grid;
  grid-template-columns: repeat(5, minmax(0, 1fr));
  gap: 8px;
}

.agent-tile {
  display: flex;
  align-items: center;
  gap: 10px;
  min-width: 0;
  border: 1px solid rgba(148, 163, 184, 0.14);
  border-radius: 12px;
  padding: 10px;
  background: rgba(30, 41, 59, 0.42);
  opacity: 0.72;
}

.agent-tile.active {
  opacity: 1;
  border-color: rgba(96, 165, 250, 0.42);
  background: rgba(37, 99, 235, 0.12);
}

.agent-avatar {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  width: 34px;
  height: 34px;
  flex: 0 0 34px;
  border-radius: 10px;
  color: #ffffff;
  font-size: 11px;
  font-weight: 800;
}

.agent-tile.knowledge .agent-avatar { background: #2563eb; }
.agent-tile.responder .agent-avatar { background: #7c3aed; }
.agent-tile.analyzer .agent-avatar { background: #db2777; }
.agent-tile.router .agent-avatar { background: #059669; }
.agent-tile.tracker .agent-avatar { background: #d97706; }

.agent-tile strong {
  display: block;
  overflow: hidden;
  color: #f8fafc;
  font-size: 12px;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.agent-tile p {
  margin: 2px 0 0;
  overflow: hidden;
  color: #64748b;
  font-size: 11px;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.route-strip,
.mcp-strip {
  display: grid;
  gap: 8px;
}

.route-strip {
  grid-template-columns: repeat(4, minmax(0, 1fr));
}

.mcp-strip {
  grid-template-columns: repeat(2, minmax(0, 1fr)) auto;
  align-items: stretch;
}

.route-strip div,
.mcp-strip div {
  border-radius: 10px;
  padding: 9px 10px;
  background: rgba(15, 23, 42, 0.58);
  border: 1px solid rgba(148, 163, 184, 0.12);
}

.route-strip span,
.mcp-strip span {
  display: block;
  color: #64748b;
  font-size: 10px;
  font-weight: 700;
  letter-spacing: 0.08em;
}

.route-strip strong,
.mcp-strip strong {
  display: block;
  margin-top: 4px;
  overflow: hidden;
  color: #e2e8f0;
  font-size: 12px;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.risk-text.low { color: #34d399; }
.risk-text.medium { color: #fbbf24; }
.risk-text.high { color: #f87171; }

.workflow-panel {
  grid-row: 2 / -1;
  display: grid;
  grid-template-rows: auto 1fr;
  gap: 12px;
  min-height: 0;
  padding: 18px 20px;
  overflow: hidden;
}

.admin-kpis {
  display: grid;
  grid-template-columns: repeat(4, minmax(0, 1fr));
  gap: 10px;
}

.admin-kpis article,
.leave-balance-grid article {
  border: 1px solid rgba(148, 163, 184, 0.14);
  border-radius: 14px;
  padding: 14px;
  background: rgba(15, 23, 42, 0.52);
}

.admin-kpis span,
.leave-balance-grid span {
  display: block;
  color: #64748b;
  font-size: 11px;
  font-weight: 800;
}

.admin-kpis strong,
.leave-balance-grid strong {
  display: block;
  margin-top: 8px;
  color: #f8fafc;
  font-size: 24px;
}

.admin-console {
  display: grid;
  grid-template-columns: minmax(0, 1fr) 320px;
  gap: 12px;
  min-height: 0;
}

.admin-list-panel,
.admin-detail-panel,
.employee-directory,
.employee-overview {
  min-height: 0;
  border: 1px solid rgba(148, 163, 184, 0.14);
  border-radius: 16px;
  background: rgba(15, 23, 42, 0.46);
}

.admin-list-panel {
  display: grid;
  grid-template-rows: auto auto 1fr;
  gap: 12px;
  min-width: 0;
  padding: 14px;
}

.admin-detail-panel {
  overflow: auto;
  padding: 14px;
}

.console-section-title {
  display: grid;
  gap: 3px;
  margin-bottom: 12px;
}

.console-section-title span {
  color: #60a5fa;
  font-size: 10px;
  font-weight: 900;
  letter-spacing: 0.1em;
}

.console-section-title strong {
  color: #f8fafc;
  font-size: 15px;
}

.detail-row {
  display: grid;
  gap: 6px;
  padding: 12px 0;
  border-bottom: 1px solid rgba(148, 163, 184, 0.1);
}

.detail-row span {
  color: #64748b;
  font-size: 11px;
  font-weight: 800;
}

.detail-row strong,
.detail-row p {
  margin: 0;
  color: #e2e8f0;
  font-size: 13px;
  line-height: 1.6;
}

.employee-panel {
  grid-row: 2 / -1;
  display: grid;
  grid-template-columns: 340px minmax(0, 1fr);
  gap: 12px;
  min-height: 0;
  padding: 18px 20px;
  overflow: hidden;
}

.employee-directory {
  display: grid;
  grid-template-rows: auto auto 1fr;
  padding: 14px;
}

.directory-search {
  display: grid;
  grid-template-columns: 1fr;
  gap: 8px;
  margin-bottom: 12px;
}

.directory-search select,
.workflow-filters select {
  border: 1px solid rgba(148, 163, 184, 0.2);
  border-radius: 10px;
  padding: 9px 10px;
  color: #e2e8f0;
  background: rgba(15, 23, 42, 0.72);
  outline: none;
}

.directory-list,
.application-list {
  display: grid;
  align-content: start;
  gap: 8px;
  min-height: 0;
  overflow: auto;
}

.directory-item {
  display: flex;
  align-items: center;
  gap: 10px;
  width: 100%;
  border: 1px solid rgba(148, 163, 184, 0.12);
  border-radius: 12px;
  padding: 10px;
  text-align: left;
  background: rgba(30, 41, 59, 0.38);
}

.directory-item.active {
  border-color: rgba(96, 165, 250, 0.48);
  background: rgba(37, 99, 235, 0.16);
}

.directory-item > span,
.employee-avatar-lg {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  color: #ffffff;
  background: linear-gradient(135deg, #2563eb, #059669);
  font-weight: 800;
}

.directory-item > span {
  width: 34px;
  height: 34px;
  flex: 0 0 34px;
  border-radius: 10px;
}

.directory-item strong,
.application-card strong {
  display: block;
  color: #f8fafc;
  font-size: 13px;
}

.directory-item p,
.application-card p,
.application-card footer {
  margin: 4px 0 0;
  color: #94a3b8;
  font-size: 12px;
  line-height: 1.5;
}

.employee-overview {
  display: grid;
  grid-template-rows: auto auto auto auto 1fr;
  gap: 12px;
  padding: 14px;
  overflow: hidden;
}

.employee-toolbar {
  display: flex;
  justify-content: space-between;
  gap: 12px;
  align-items: center;
}

.employee-toolbar span {
  color: #60a5fa;
  font-size: 10px;
  font-weight: 900;
  letter-spacing: 0.1em;
}

.employee-toolbar h3,
.employee-profile-card h3 {
  margin: 3px 0 0;
  color: #f8fafc;
}

.employee-profile-card {
  display: flex;
  gap: 14px;
  align-items: center;
  border: 1px solid rgba(148, 163, 184, 0.12);
  border-radius: 16px;
  padding: 16px;
  background: linear-gradient(135deg, rgba(37, 99, 235, 0.14), rgba(15, 23, 42, 0.46));
}

.employee-avatar-lg {
  width: 58px;
  height: 58px;
  flex: 0 0 58px;
  border-radius: 16px;
  font-size: 22px;
}

.employee-profile-card p {
  margin: 5px 0 0;
  color: #94a3b8;
  font-size: 13px;
}

.employee-contact {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
  margin-top: 10px;
}

.employee-contact span {
  border-radius: 999px;
  padding: 5px 9px;
  color: #cbd5e1;
  background: rgba(15, 23, 42, 0.54);
  font-size: 11px;
}

.employee-kpis strong {
  font-size: 20px;
}

.leave-balance-grid {
  display: grid;
  grid-template-columns: repeat(4, minmax(0, 1fr));
  gap: 10px;
}

.leave-balance-grid strong {
  font-size: 20px;
}

.application-board {
  display: grid;
  grid-template-rows: auto 1fr;
  min-height: 0;
}

.application-card {
  display: grid;
  gap: 8px;
  border: 1px solid rgba(148, 163, 184, 0.12);
  border-radius: 14px;
  padding: 13px;
  background: rgba(30, 41, 59, 0.38);
}

.application-card .status-pill {
  width: fit-content;
  margin-bottom: 8px;
}

.application-card footer {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
  border-top: 1px solid rgba(148, 163, 184, 0.1);
  padding-top: 8px;
}

.empty-hint {
  margin: 0;
  color: #64748b;
  font-size: 12px;
  line-height: 1.6;
}

.workflow-toolbar {
  display: flex;
  align-items: end;
  justify-content: space-between;
  gap: 12px;
}

.workflow-filters {
  display: flex;
  flex-wrap: wrap;
  gap: 10px;
}

.workflow-filters label {
  display: grid;
  gap: 6px;
  color: #64748b;
  font-size: 11px;
  font-weight: 700;
}

.workflow-filters select {
  min-width: 120px;
  border: 1px solid rgba(148, 163, 184, 0.2);
  border-radius: 10px;
  padding: 9px 10px;
  color: #e2e8f0;
  background: rgba(15, 23, 42, 0.72);
  outline: none;
}

.workflow-message {
  margin: 0;
  border-radius: 10px;
  padding: 10px 12px;
  font-size: 12px;
}

.workflow-message.success {
  color: #86efac;
  background: rgba(22, 163, 74, 0.12);
  border: 1px solid rgba(34, 197, 94, 0.22);
}

.workflow-message.error {
  color: #fecaca;
  background: rgba(220, 38, 38, 0.12);
  border: 1px solid rgba(248, 113, 113, 0.22);
}

.workflow-table-wrap {
  min-height: 0;
  overflow: auto;
  border: 1px solid rgba(148, 163, 184, 0.15);
  border-radius: 14px;
}

.workflow-table {
  width: 100%;
  border-collapse: collapse;
  min-width: 920px;
}

.workflow-table th,
.workflow-table td {
  padding: 12px;
  border-bottom: 1px solid rgba(148, 163, 184, 0.12);
  text-align: left;
  vertical-align: top;
}

.workflow-table th {
  position: sticky;
  top: 0;
  z-index: 1;
  color: #94a3b8;
  background: rgba(15, 23, 42, 0.96);
  font-size: 11px;
  font-weight: 800;
}

.workflow-table tbody tr {
  cursor: pointer;
}

.workflow-table tbody tr.selected {
  background: rgba(37, 99, 235, 0.12);
}

.workflow-table td strong {
  display: block;
  color: #f8fafc;
  font-size: 13px;
  line-height: 1.4;
}

.workflow-table td span {
  display: block;
  margin-top: 5px;
  color: #94a3b8;
  font-size: 12px;
  line-height: 1.5;
}

.empty-cell {
  color: #64748b;
  text-align: center;
}

.status-pill {
  display: inline-flex;
  align-items: center;
  border-radius: 999px;
  padding: 6px 10px;
  font-size: 12px;
  font-weight: 700;
}

.status-pill.pending {
  color: #fef3c7;
  background: rgba(245, 158, 11, 0.14);
  border: 1px solid rgba(251, 191, 36, 0.24);
}

.status-pill.approved {
  color: #bbf7d0;
  background: rgba(22, 163, 74, 0.14);
  border: 1px solid rgba(74, 222, 128, 0.24);
}

.status-pill.rejected {
  color: #fecaca;
  background: rgba(220, 38, 38, 0.14);
  border: 1px solid rgba(248, 113, 113, 0.24);
}

.workflow-actions {
  display: flex;
  gap: 8px;
  align-items: center;
}

.approve-btn,
.reject-btn {
  border-radius: 9px;
  padding: 8px 11px;
  color: #ffffff;
  font-size: 12px;
  font-weight: 700;
}

.approve-btn {
  background: #16a34a;
}

.reject-btn {
  background: #dc2626;
}

.done-text {
  color: #64748b;
  font-size: 12px;
}

.modal-backdrop {
  position: fixed;
  inset: 0;
  z-index: 10;
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 20px;
  background: rgba(2, 6, 23, 0.68);
  backdrop-filter: blur(8px);
}

.review-dialog {
  width: min(520px, 100%);
  border: 1px solid rgba(148, 163, 184, 0.22);
  border-radius: 18px;
  padding: 18px;
  background: #0f172a;
  box-shadow: 0 24px 80px rgba(2, 6, 23, 0.55);
}

.review-dialog header,
.review-dialog footer {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
}

.review-dialog header span {
  display: block;
  color: #60a5fa;
  font-size: 11px;
  font-weight: 800;
}

.review-dialog header strong {
  display: block;
  margin-top: 4px;
  color: #f8fafc;
  font-size: 16px;
}

.review-dialog textarea {
  width: 100%;
  min-height: 120px;
  margin: 16px 0;
}

.icon-close-btn {
  width: 34px;
  height: 34px;
  padding: 0;
  border-radius: 50%;
  color: #cbd5e1;
  background: rgba(30, 41, 59, 0.72);
}

.fold-card {
  border: 1px solid rgba(148, 163, 184, 0.16);
  border-radius: 14px;
  background: rgba(15, 23, 42, 0.4);
  overflow: hidden;
}

.fold-card summary {
  display: grid;
  grid-template-columns: auto 1fr auto;
  gap: 10px;
  align-items: center;
  min-height: 48px;
  padding: 12px 14px;
  cursor: pointer;
  list-style: none;
  user-select: none;
}

.fold-card summary::-webkit-details-marker {
  display: none;
}

.fold-card summary::before {
  content: "+";
  display: inline-flex;
  align-items: center;
  justify-content: center;
  width: 22px;
  height: 22px;
  border-radius: 999px;
  color: #93c5fd;
  background: rgba(59, 130, 246, 0.14);
  border: 1px solid rgba(96, 165, 250, 0.28);
  font-weight: 700;
}

.fold-card[open] summary::before {
  content: "-";
}

.fold-card summary span {
  color: #60a5fa;
  font-size: 11px;
  font-weight: 800;
  letter-spacing: 0.08em;
}

.fold-card summary strong {
  color: #f8fafc;
  font-size: 13px;
}

.fold-card summary em {
  border-radius: 999px;
  padding: 5px 9px;
  color: #bfdbfe;
  background: rgba(37, 99, 235, 0.16);
  border: 1px solid rgba(96, 165, 250, 0.25);
  font-size: 11px;
  font-style: normal;
  font-weight: 700;
}

.text-reference-card {
  margin-top: 12px;
}

.reference-markdown {
  padding: 0 14px 14px;
}

.thinking-indicator {
  display: flex;
  justify-content: center;
  gap: 6px;
  padding: 16px;
}

.thinking-dot {
  width: 8px;
  height: 8px;
  border-radius: 50%;
  background: #60a5fa;
  animation: thinking 1.4s ease-in-out infinite;
}

.thinking-dot:nth-child(2) {
  animation-delay: 0.2s;
}

.thinking-dot:nth-child(3) {
  animation-delay: 0.4s;
}

@keyframes thinking {
  0%, 80%, 100% {
    transform: scale(0.6);
    opacity: 0.5;
  }
  40% {
    transform: scale(1);
    opacity: 1;
  }
}

@media (max-width: 900px) {
  .agent-roster,
  .route-strip,
  .mcp-strip {
    grid-template-columns: 1fr;
  }

  .workflow-toolbar {
    align-items: stretch;
    flex-direction: column;
  }

  .admin-console,
  .employee-panel {
    grid-template-columns: 1fr;
  }

  .admin-kpis,
  .leave-balance-grid {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }

  .employee-directory,
  .admin-detail-panel {
    max-height: 360px;
  }
}
</style>
