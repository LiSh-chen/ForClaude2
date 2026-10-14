/** 環境變數存取。開發環境缺少金鑰時退回「僅限開發」的預設值，正式環境一律要求設定。 */
import { createHash } from "node:crypto";

const isProd = () => process.env.NODE_ENV === "production";
const warned = new Set<string>();
function devFallback(name: string, value: Buffer | string): Buffer | string {
  if (isProd()) throw new Error(`${name} must be set in production`);
  if (!warned.has(name)) {
    warned.add(name);
    console.warn(`[env] ${name} 未設定，使用僅限開發的預設值（請勿用於正式環境）`);
  }
  return value;
}

/** 32 bytes，base64 編碼 */
export function getEncryptionKey(): Buffer {
  const raw = process.env.ENCRYPTION_KEY;
  if (raw) {
    const key = Buffer.from(raw, "base64");
    if (key.length !== 32) throw new Error("ENCRYPTION_KEY must be 32 bytes, base64-encoded (openssl rand -base64 32)");
    return key;
  }
  return devFallback("ENCRYPTION_KEY", createHash("sha256").update("review-butler-dev-only-key").digest()) as Buffer;
}

export function getSessionSecret(): string {
  return process.env.SESSION_SECRET ?? (devFallback("SESSION_SECRET", "review-butler-dev-only-session-secret") as string);
}

/** 分類用小模型、撰寫用大模型；環境變數可覆寫。 */
export const DEFAULT_MODELS = { classify: "claude-haiku-5-5", write: "claude-sonnet-5-5" } as const;

export function getModel(tier: "classify" | "write"): string {
  const v = tier === "classify" ? process.env.LLM_MODEL_CLASSIFY : process.env.LLM_MODEL_WRITE;
  return v?.trim() || DEFAULT_MODELS[tier];
}
