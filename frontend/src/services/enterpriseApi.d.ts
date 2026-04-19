import type { ReadyCapability } from "../types/enterprise";
export declare function checkHealth(): Promise<string>;
export declare function requestEnterprise(capability: ReadyCapability, message: string, chatId: string): Promise<string>;
export type StreamCallback = (chunk: string) => void;
export type StreamDoneCallback = (fullContent: string) => void;
export type StreamErrorCallback = (error: Error) => void;
export interface StreamOptions {
    onChunk?: StreamCallback;
    onDone?: StreamDoneCallback;
    onError?: StreamErrorCallback;
}
export declare function createStreamController(): {
    stream: (capability: ReadyCapability, message: string, chatId: string, options?: StreamOptions) => void;
    close: () => void;
};
export interface DocumentListResponse {
    success: boolean;
    documents: string[];
    count: number;
}
export interface UploadResponse {
    success: boolean;
    message: string;
    filename?: string;
    chunks?: number;
}
export declare function listDocuments(): Promise<DocumentListResponse>;
export declare function uploadDocument(file: File): Promise<UploadResponse>;
export declare function deleteDocument(filename: string): Promise<{
    success: boolean;
    message: string;
}>;
export declare function reloadKnowledgeBase(): Promise<{
    success: boolean;
    message: string;
}>;
