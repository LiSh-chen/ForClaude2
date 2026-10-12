import type { GoogleBusinessAdapter } from "./google-business/interface";
import { MockGoogleBusinessAdapter } from "./google-business/mock";
import { RealGoogleBusinessAdapter } from "./google-business/real";
import type { LineAdapter } from "./line/interface";
import { MockLineAdapter } from "./line/mock";
import { RealLineAdapter } from "./line/real";
import type { LlmAdapter } from "./llm/interface";
import { ClaudeLlmAdapter } from "./llm/claude";
import { MockLlmAdapter } from "./llm/mock";
import type { PaymentAdapter } from "./payment/interface";
import { EcpayPaymentAdapter } from "./payment/ecpay";
import { MockPaymentAdapter } from "./payment/mock";

/** USE_MOCK_ADAPTERS 未設定時預設為 mock，避免開發環境誤打正式 API。 */
export function useMockAdapters(): boolean {
  return process.env.USE_MOCK_ADAPTERS !== "false";
}

// 以 globalThis 保留單例，避免 Next dev 熱重載丟失 mock 的記憶體狀態。
const g = globalThis as unknown as { __adapters?: Record<string, unknown> };
const cache = (g.__adapters ??= {});
function once<T>(key: string, make: () => T): T {
  return (cache[key] ??= make()) as T;
}

export const getGoogleBusiness = (): GoogleBusinessAdapter =>
  once("google", () => (useMockAdapters() ? new MockGoogleBusinessAdapter() : new RealGoogleBusinessAdapter()));
export const getLine = (): LineAdapter =>
  once("line", () => (useMockAdapters() ? new MockLineAdapter() : new RealLineAdapter()));
export const getLlm = (): LlmAdapter =>
  once("llm", () => (useMockAdapters() ? new MockLlmAdapter() : new ClaudeLlmAdapter()));
export const getPayment = (): PaymentAdapter =>
  once("payment", () => (useMockAdapters() ? new MockPaymentAdapter() : new EcpayPaymentAdapter()));
