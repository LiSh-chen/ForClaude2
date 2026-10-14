import { describe, expect, it } from "vitest";
import { RateLimitError } from "@/lib/adapters/errors";
import { computeCostUsd } from "@/config/model-pricing";
import { withBackoff } from "@/lib/retry";
import { monthKey } from "@/lib/usage";

describe("monthKey (Asia/Taipei)", () => {
  it("rolls over at Taipei midnight, not UTC midnight", () => {
    expect(monthKey(new Date("2026-09-30T15:59:59Z"))).toBe("2026-09");
    expect(monthKey(new Date("2026-09-30T16:00:00Z"))).toBe("2026-10"); // 台灣 10/1 00:00
    expect(monthKey(new Date("2026-12-31T16:00:00Z"))).toBe("2027-01");
  });
});

describe("withBackoff", () => {
  it("retries retryable errors with exponential delays then succeeds", async () => {
    const waits: number[] = [];
    let n = 0;
    const out = await withBackoff(async () => { if (n++ < 3) throw new RateLimitError(); return "ok"; }, { baseMs: 100, sleep: async (ms) => { waits.push(ms); } });
    expect(out).toBe("ok");
    expect(waits).toEqual([100, 200, 400]);
  });
  it("honors Retry-After and gives up after the retry budget", async () => {
    const waits: number[] = [];
    await expect(withBackoff(async () => { throw new RateLimitError(5000); }, { retries: 2, sleep: async (ms) => { waits.push(ms); } })).rejects.toBeInstanceOf(RateLimitError);
    expect(waits).toEqual([5000, 5000]);
  });
  it("does not retry other errors", async () => {
    let n = 0;
    await expect(withBackoff(async () => { n++; throw new Error("nope"); }, { sleep: async () => {} })).rejects.toThrow("nope");
    expect(n).toBe(1);
  });
});

describe("computeCostUsd", () => {
  it("prices tokens per million", () => {
    // Sonnet 5.5：$2 / $10
    expect(computeCostUsd("claude-sonnet-5-5", { input: 1_000_000, output: 1_000_000 })).toBeCloseTo(12);
    expect(computeCostUsd("claude-haiku-5-5", { input: 800, output: 400 })).toBeCloseTo(0.00028, 6);
  });
  it("applies cache multipliers and tolerates unknown models", () => {
    expect(computeCostUsd("claude-sonnet-5-5", { input: 0, output: 0, cacheRead: 1_000_000 })).toBeCloseTo(0.2);
    expect(computeCostUsd("claude-sonnet-5-5", { input: 0, output: 0, cacheCreation: 1_000_000 })).toBeCloseTo(2.5);
    expect(computeCostUsd("some-future-model", { input: 10, output: 10 })).toBe(0);
  });
});
