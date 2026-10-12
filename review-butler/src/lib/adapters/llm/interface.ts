/** classify 用小模型、write 用大模型；實際模型名稱由環境變數決定。 */
export type LlmTier = "classify" | "write";

export interface LlmRequest {
  tier: LlmTier;
  /** 記錄用途（classify / write / report / geo_check ...） */
  purpose: string;
  system: string;
  user: string;
  maxTokens?: number;
}

export interface LlmResponse {
  text: string;
  model: string;
  tokensIn: number;
  tokensOut: number;
  /** 美元 */
  costUsd: number;
}

export interface LlmAdapter {
  generate(req: LlmRequest): Promise<LlmResponse>;
}
