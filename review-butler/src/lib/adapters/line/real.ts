import { NotImplementedError } from "../errors";
import type { LineAdapter } from "./interface";

/** 階段 2 實作。 */
export class RealLineAdapter implements LineAdapter {
  pushMessage(): never { throw new NotImplementedError("Line.pushMessage"); }
  replyMessage(): never { throw new NotImplementedError("Line.replyMessage"); }
  getProfile(): never { throw new NotImplementedError("Line.getProfile"); }
  verifySignature(): never { throw new NotImplementedError("Line.verifySignature"); }
}
