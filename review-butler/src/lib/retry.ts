import { RateLimitError } from "@/lib/adapters/errors";

export interface BackoffOptions {
  retries?: number;
  baseMs?: number;
  maxMs?: number;
  isRetryable?: (e: unknown) => boolean;
  sleep?: (ms: number) => Promise<void>;
}

const realSleep = (ms: number) => new Promise<void>((r) => setTimeout(r, ms));

/** 指數退避重試（SPEC F3）：1s、2s、4s…，遇到 RateLimitError 時優先採用對方指定的等待時間。 */
export async function withBackoff<T>(fn: () => Promise<T>, opts: BackoffOptions = {}): Promise<T> {
  const { retries = 3, baseMs = 1000, maxMs = 30_000, isRetryable = (e) => e instanceof RateLimitError, sleep = realSleep } = opts;
  for (let attempt = 0; ; attempt++) {
    try {
      return await fn();
    } catch (e) {
      if (attempt >= retries || !isRetryable(e)) throw e;
      const hinted = e instanceof RateLimitError ? e.retryAfterMs : undefined;
      await sleep(Math.min(hinted ?? baseMs * 2 ** attempt, maxMs));
    }
  }
}
