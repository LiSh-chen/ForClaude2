import Anthropic from "@anthropic-ai/sdk";
import { afterEach, describe, expect, it, vi } from "vitest";
import { ClaudeLlmAdapter, type MessagesClient } from "@/lib/adapters/llm/claude";
import { LlmFatalError, LlmRefusalError, LlmTruncatedError } from "@/lib/adapters/llm/interface";

afterEach(() => vi.unstubAllEnvs());

function fakeClient(response: Partial<Anthropic.Message> | Error) {
  const create = vi.fn(async (_params: Anthropic.MessageCreateParamsNonStreaming) => {
    if (response instanceof Error) throw response;
    return {
      id: "msg_1", type: "message", role: "assistant", model: "claude-sonnet-5-5", stop_reason: "end_turn", stop_sequence: null,
      content: [{ type: "text", text: " 回覆內容 ", citations: null }],
      usage: { input_tokens: 1000, output_tokens: 500, cache_creation_input_tokens: 0, cache_read_input_tokens: 0 },
      ...response,
    } as Anthropic.Message;
  });
  return { client: { messages: { create } } as unknown as MessagesClient, create };
}

describe("ClaudeLlmAdapter (以假的 SDK client 測試；真實呼叫需 ANTHROPIC_API_KEY)", () => {
  it("sends a Claude-5-compatible request and returns text, usage and cost", async () => {
    vi.stubEnv("LLM_MODEL_WRITE", "claude-sonnet-5-5");
    const { client, create } = fakeClient({});
    const res = await new ClaudeLlmAdapter(client).generate({ tier: "write", purpose: "write", system: "SYS", user: "USR" });
    expect(res).toMatchObject({ text: "回覆內容", model: "claude-sonnet-5-5", tokensIn: 1000, tokensOut: 500 });
    expect(res.costUsd).toBeCloseTo((1000 * 2 + 500 * 10) / 1e6, 8);

    const params = create.mock.calls[0]![0] as unknown as Record<string, unknown>;
    expect(params.model).toBe("claude-sonnet-5-5");
    expect(params.output_config).toEqual({ effort: "low" });
    expect(params.system).toEqual([{ type: "text", text: "SYS", cache_control: { type: "ephemeral" } }]);
    // Claude 5 系列：下列參數會 400，不可送出
    for (const k of ["temperature", "top_p", "top_k", "thinking", "tool_choice"]) expect(params).not.toHaveProperty(k);
  });

  it("uses the classify model from env and defaults to the small model", async () => {
    const { client, create } = fakeClient({});
    await new ClaudeLlmAdapter(client).generate({ tier: "classify", purpose: "classify", system: "s", user: "u" });
    expect((create.mock.calls[0]![0] as { model: string }).model).toBe("claude-haiku-5-5");
    vi.stubEnv("LLM_MODEL_CLASSIFY", "my-classify-model");
    await new ClaudeLlmAdapter(client).generate({ tier: "classify", purpose: "classify", system: "s", user: "u" });
    expect((create.mock.calls[1]![0] as { model: string }).model).toBe("my-classify-model");
  });

  it("counts cache tokens in usage and cost", async () => {
    const { client } = fakeClient({ usage: { input_tokens: 100, output_tokens: 0, cache_creation_input_tokens: 0, cache_read_input_tokens: 900 } as Anthropic.Usage });
    const res = await new ClaudeLlmAdapter(client).generate({ tier: "write", purpose: "write", system: "s", user: "u" });
    expect(res.tokensIn).toBe(1000);
    expect(res.costUsd).toBeCloseTo((100 * 2 + 900 * 2 * 0.1) / 1e6, 8);
  });

  it("surfaces refusals and truncation as typed errors", async () => {
    const refusal = fakeClient({ stop_reason: "refusal", stop_details: { type: "refusal", category: "general_harms", explanation: null } } as Partial<Anthropic.Message>);
    await expect(new ClaudeLlmAdapter(refusal.client).generate({ tier: "write", purpose: "w", system: "s", user: "u" })).rejects.toBeInstanceOf(LlmRefusalError);
    const cut = fakeClient({ stop_reason: "max_tokens" });
    await expect(new ClaudeLlmAdapter(cut.client).generate({ tier: "write", purpose: "w", system: "s", user: "u" })).rejects.toBeInstanceOf(LlmTruncatedError);
  });

  it("maps non-retryable API errors to LlmFatalError and passes transient ones through", async () => {
    const bad = new Anthropic.BadRequestError(400, { type: "error", error: { type: "invalid_request_error", message: "bad" } }, "bad", new Headers());
    await expect(new ClaudeLlmAdapter(fakeClient(bad).client).generate({ tier: "write", purpose: "w", system: "s", user: "u" })).rejects.toBeInstanceOf(LlmFatalError);
    const rate = new Anthropic.RateLimitError(429, { type: "error", error: { type: "rate_limit_error", message: "slow" } }, "slow", new Headers());
    await expect(new ClaudeLlmAdapter(fakeClient(rate).client).generate({ tier: "write", purpose: "w", system: "s", user: "u" })).rejects.toBeInstanceOf(Anthropic.RateLimitError);
  });
});
