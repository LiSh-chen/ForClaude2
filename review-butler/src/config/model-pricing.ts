/**
 * 模型單價（美元 / 百萬 token）。來源：Anthropic 公布價目（2026-10-06 快取版），請定期核對。
 * 未列出的模型成本會記為 0 並在 log 警告，不會中斷流程。
 */
export const MODEL_PRICING: Record<string, { input: number; output: number }> = {
  "claude-haiku-5-5": { input: 0.1, output: 0.5 },
  "claude-sonnet-5-5": { input: 2, output: 10 },
  "claude-sonnet-5": { input: 2, output: 10 },
  "claude-opus-5-5": { input: 4, output: 20 },
  "claude-opus-5": { input: 5, output: 25 },
  "claude-opus-4-8": { input: 5, output: 25 },
  "claude-fable-5-1": { input: 10, output: 50 },
};

/** prompt cache：寫入 1.25 倍、讀取 0.1 倍輸入價 */
const CACHE_WRITE = 1.25;
const CACHE_READ = 0.1;

export interface TokenUsage {
  input: number;
  output: number;
  cacheCreation?: number;
  cacheRead?: number;
}

export function computeCostUsd(model: string, u: TokenUsage): number {
  const p = MODEL_PRICING[model];
  if (!p) {
    console.warn(`[pricing] 未知模型 ${model}，成本記為 0`);
    return 0;
  }
  const inputUnits = u.input + (u.cacheCreation ?? 0) * CACHE_WRITE + (u.cacheRead ?? 0) * CACHE_READ;
  return (inputUnits * p.input + u.output * p.output) / 1_000_000;
}
