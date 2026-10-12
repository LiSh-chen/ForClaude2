import { NotImplementedError } from "../errors";
import type { LlmAdapter } from "./interface";

/** 階段 1 實作（使用真實 Claude API；模型名稱讀環境變數）。 */
export class ClaudeLlmAdapter implements LlmAdapter {
  generate(): never { throw new NotImplementedError("Llm.generate (Claude)"); }
}
