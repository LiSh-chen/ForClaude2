import Anthropic from "@anthropic-ai/sdk";
import { computeCostUsd } from "@/config/model-pricing";
import { getModel } from "@/lib/env";
import {
  LlmFatalError,
  LlmRefusalError,
  LlmTruncatedError,
  type LlmAdapter,
  type LlmRequest,
  type LlmResponse,
} from "./interface";

/** 最小的 SDK 介面，讓測試可以注入假的 client。 */
export interface MessagesClient {
  messages: { create(params: Anthropic.MessageCreateParamsNonStreaming): Promise<Anthropic.Message> };
}

/**
 * 真實 Claude API。注意（對應 Claude 5 系列的行為）：
 * - 不送 temperature / top_p（非預設值會 400）
 * - 不關閉 thinking、不使用強制 tool_choice（會 400）；以 output_config.effort 控制成本
 * - 把 system prompt 標上 cache_control，重複的規則文字可被快取
 * 認證由 SDK 從 ANTHROPIC_API_KEY（或 ant auth login 的 profile）自動解析。
 */
export class ClaudeLlmAdapter implements LlmAdapter {
  private _client?: MessagesClient;

  constructor(client?: MessagesClient) {
    this._client = client;
  }

  private get client(): MessagesClient {
    return (this._client ??= new Anthropic());
  }

  async generate(req: LlmRequest): Promise<LlmResponse> {
    const model = getModel(req.tier);
    let res: Anthropic.Message;
    try {
      res = await this.client.messages.create({
        model,
        max_tokens: req.maxTokens ?? (req.tier === "classify" ? 2000 : 3000),
        system: [{ type: "text", text: req.system, cache_control: { type: "ephemeral" } }],
        output_config: { effort: req.effort ?? "low" },
        messages: [{ role: "user", content: req.user }],
      });
    } catch (error) {
      // 429 / 5xx / 連線錯誤已由 SDK 重試（預設 2 次）；其餘 4xx 重試無意義
      if (error instanceof Anthropic.BadRequestError || error instanceof Anthropic.AuthenticationError || error instanceof Anthropic.PermissionDeniedError || error instanceof Anthropic.NotFoundError) {
        throw new LlmFatalError(error.message, error.status);
      }
      throw error;
    }

    if (res.stop_reason === "refusal") throw new LlmRefusalError(res.stop_details?.category ?? null);
    if (res.stop_reason === "max_tokens") throw new LlmTruncatedError();

    const text = res.content
      .filter((b): b is Anthropic.TextBlock => b.type === "text")
      .map((b) => b.text)
      .join("")
      .trim();

    const tokensIn = res.usage.input_tokens + (res.usage.cache_creation_input_tokens ?? 0) + (res.usage.cache_read_input_tokens ?? 0);
    return {
      text,
      model: res.model ?? model,
      tokensIn,
      tokensOut: res.usage.output_tokens,
      costUsd: computeCostUsd(model, {
        input: res.usage.input_tokens,
        output: res.usage.output_tokens,
        cacheCreation: res.usage.cache_creation_input_tokens ?? 0,
        cacheRead: res.usage.cache_read_input_tokens ?? 0,
      }),
    };
  }
}
