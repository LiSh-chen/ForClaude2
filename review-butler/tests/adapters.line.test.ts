import { describe, expect, it } from "vitest";
import { MockLineAdapter } from "@/lib/adapters/line/mock";

describe("MockLineAdapter", () => {
  it("records push and reply messages", async () => {
    const line = new MockLineAdapter();
    await line.pushMessage("U1", [{ type: "text", text: "hi" }]);
    await line.replyMessage("tok", [{ type: "text", text: "yo" }]);
    expect(line.sent.map((m) => m.kind)).toEqual(["push", "reply"]);
  });
  it("verifies webhook signatures", () => {
    const line = new MockLineAdapter("secret");
    const body = JSON.stringify({ events: [] });
    expect(line.verifySignature(body, line.sign(body))).toBe(true);
    expect(line.verifySignature(body, "bogus")).toBe(false);
    expect(line.verifySignature(body + " ", line.sign(body))).toBe(false);
  });
});
