import type { CapabilityDefinition } from "../types/enterprise";

export const CAPABILITIES: CapabilityDefinition[] = [
  {
    key: "chat",
    name: "普通问答",
    description: "调用 /enterprise/chat，适合通用 HR 与行政对话。",
    status: "ready"
  },
  {
    key: "rag-chat",
    name: "知识库问答",
    description: "调用 /enterprise/rag-chat，结合企业知识文档回答问题。",
    status: "ready"
  },
  {
    key: "ticket",
    name: "工单生成",
    description: "调用 /enterprise/ticket，返回结构化工单对象。",
    status: "ready"
  },
  {
    key: "mcp",
    name: "MCP 集成",
    description: "后续可接入 MCP Server 与多 Agent 协作。",
    status: "planned"
  },
  {
    key: "tool-calling",
    name: "工具调用",
    description: "后续可接入 Tool Calling、函数执行与工作流编排。",
    status: "planned"
  }
];
