import { describe, expect, it } from "vitest";
import { MockLlmAdapter } from "@/lib/adapters/llm/mock";

describe("MockLlmAdapter", () => {
  const llm = new MockLlmAdapter();
  it("classify returns parseable JSON with risk flags", async () => {
    const res = await llm.generate({
      tier: "classify",
      purpose: "classify",
      system: "s",
      user: "星等：1\n評論：吃完上吐下瀉，我要請律師",
    });
    const parsed = JSON.parse(res.text);
    expect(parsed.sentiment).toBe("negative");
    expect(parsed.risk_flags).toEqual(expect.arrayContaining(["涉及食安", "提到法律行動"]));
    expect(parsed.needs_human).toBe(true);
  });
  it("reports usage", async () => {
    const res = await llm.generate({ tier: "write", purpose: "write", system: "s", user: "星等：5\n評論：很棒" });
    expect(res.tokensIn).toBeGreaterThan(0);
    expect(res.tokensOut).toBeGreaterThan(0);
    expect(res.costUsd).toBe(0);
  });
});
