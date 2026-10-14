import { z } from "zod";
import { RISK_FLAGS } from "./risk";

const schema = z.object({
  sentiment: z.enum(["positive", "neutral", "negative"]),
  topics: z.array(z.string()).default([]),
  language: z.enum(["zh-TW", "en", "ja", "other"]).catch("other"),
  needs_human: z.boolean().default(false),
  risk_flags: z.array(z.string()).default([]),
});

export interface ParsedClassification {
  sentiment: "positive" | "neutral" | "negative";
  topics: string[];
  language: "zh-TW" | "en" | "ja" | "other";
  needsHuman: boolean;
  riskFlags: string[];
}

/** 從模型輸出取出 JSON 並驗證；不符格式回傳 null。topics 只保留清單內的值，risk_flags 只保留已知旗標。 */
export function parseClassification(text: string, allowedTopics: string[]): ParsedClassification | null {
  const start = text.indexOf("{");
  const end = text.lastIndexOf("}");
  if (start < 0 || end <= start) return null;
  let raw: unknown;
  try {
    raw = JSON.parse(text.slice(start, end + 1));
  } catch {
    return null;
  }
  const r = schema.safeParse(raw);
  if (!r.success) return null;
  const topics = r.data.topics.filter((t) => allowedTopics.includes(t)).slice(0, 3);
  const riskFlags = r.data.risk_flags.filter((f) => (RISK_FLAGS as readonly string[]).includes(f));
  return {
    sentiment: r.data.sentiment,
    topics: topics.length ? topics : ["其他"],
    language: r.data.language,
    needsHuman: r.data.needs_human || riskFlags.length > 0,
    riskFlags,
  };
}
