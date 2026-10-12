import { describe, expect, it } from "vitest";
import { PLANS } from "@/config/plans";

describe("plans (SPEC §2)", () => {
  it("matches the spec", () => {
    expect(PLANS.basic).toMatchObject({ priceTwd: 299, maxLocations: 1, monthlyDrafts: 100 });
    expect(PLANS.pro).toMatchObject({ priceTwd: 490, maxLocations: 3, monthlyDrafts: 400 });
    expect(PLANS.trial.trialDays).toBe(14);
  });
  it("trial has all pro features and limits", () => {
    expect(PLANS.trial.features).toEqual(PLANS.pro.features);
    expect(PLANS.trial.monthlyDrafts).toBe(PLANS.pro.monthlyDrafts);
  });
});
