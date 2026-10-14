import fixtures from "./reviews.json";
import type { Industry } from "@/config/industries";

export interface FixtureReview {
  id: string;
  stars: 1 | 2 | 3 | 4 | 5;
  comment: string | null;
  language: "zh-TW" | "en" | "ja" | "other";
  industry: Industry;
  expectRisk: string[];
  note: string;
}

export const REVIEW_FIXTURES = fixtures as FixtureReview[];
