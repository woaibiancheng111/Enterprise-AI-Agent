import type { CapabilityDefinition } from "../types/enterprise";

export const CAPABILITIES: CapabilityDefinition[] = [
  {
    key: "team-chat",
    name: "综合办理",
    description: "适合不确定找谁、需要先判断政策再处理的事务。",
    status: "ready"
  },
  {
    key: "chat",
    name: "日常咨询",
    description: "快速解答 HR、行政、办公相关的一般问题。",
    status: "ready"
  },
  {
    key: "rag-chat",
    name: "制度查询",
    description: "基于公司资料库回答制度、流程和材料要求。",
    status: "ready"
  },
  {
    key: "ticket",
    name: "工单整理",
    description: "把诉求整理成可转交处理的结构化工单。",
    status: "ready"
  },
  {
    key: "tool-chat",
    name: "业务工具",
    description: "查询员工信息、假期余额、申请请假或报销。",
    status: "ready"
  },
  {
    key: "mcp",
    name: "MCP 集成",
    description: "通过 MCP 工具服务执行员工、时间、知识库能力。",
    status: "ready"
  }
];
