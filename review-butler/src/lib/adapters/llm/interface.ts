/** classify 用小模型、write 用大模型；實際模型名稱由環境變數決定。 */
export type LlmTier = "classify" | "write";

export interface LlmRequest {
  tier: LlmTier;
  /** 記錄用途（classify / write / report / geo_check ...） */
  purpose: string;
  system: string;
  user: string;
  maxTokens?: number;
  /** 推理強度，預設 low（成本優先）。實測品質不足時再調高。 */
  effort?: "low" | "medium" | "high";
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

/** 模型因安全政策拒絕回答（stop_reason = "refusal"）。呼叫端應視為失敗並請店家手動回覆。 */
export class LlmRefusalError extends Error {
  constructor(public category?: string | null) {
    super(`LLM refused the request${category ? ` (${category})` : ""}`);
    this.name = "LlmRefusalError";
  }
}

/** 輸出被 max_tokens 截斷 */
export class LlmTruncatedError extends Error {
  constructor() {
    super("LLM output truncated (max_tokens)");
    this.name = "LlmTruncatedError";
  }
}

/** 重試也沒用的錯誤（例如 400 參數錯誤、401 金鑰錯誤） */
export class LlmFatalError extends Error {
  constructor(message: string, public status?: number) {
    super(message);
    this.name = "LlmFatalError";
  }
}
