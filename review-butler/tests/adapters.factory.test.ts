import { afterEach, describe, expect, it, vi } from "vitest";
import { getLlm, getLine, getGoogleBusiness, getPayment, useMockAdapters } from "@/lib/adapters";
import { NotImplementedError } from "@/lib/adapters/errors";
import { MockLlmAdapter } from "@/lib/adapters/llm/mock";
import { RealGoogleBusinessAdapter } from "@/lib/adapters/google-business/real";

afterEach(() => vi.unstubAllEnvs());

describe("adapter factory", () => {
  it("defaults to mock and returns singletons", () => {
    vi.stubEnv("USE_MOCK_ADAPTERS", "");
    expect(useMockAdapters()).toBe(true);
    expect(getLlm()).toBeInstanceOf(MockLlmAdapter);
    expect(getLlm()).toBe(getLlm());
    for (const get of [getLine, getGoogleBusiness, getPayment]) expect(get()).toBeDefined();
  });
  it("USE_MOCK_ADAPTERS=false disables mocks", () => {
    vi.stubEnv("USE_MOCK_ADAPTERS", "false");
    expect(useMockAdapters()).toBe(false);
  });
  it("real adapters are stubs for now", () => {
    expect(() => new RealGoogleBusinessAdapter().listLocations()).toThrow(NotImplementedError);
  });
});
