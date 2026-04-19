export type ReadyCapability = "chat" | "rag-chat" | "ticket";

export type CapabilityStatus = "ready" | "planned";

export interface CapabilityDefinition {
  key: ReadyCapability | "mcp" | "tool-calling";
  name: string;
  description: string;
  status: CapabilityStatus;
}

export interface TicketResponse {
  employeeName: string;
  department: string;
  requirementType: string;
  actionItems: string[];
}

export interface ChatMessage {
  id: string;
  role: "user" | "assistant" | "system";
  text: string;
  isJson?: boolean;
  streaming?: boolean;
}
