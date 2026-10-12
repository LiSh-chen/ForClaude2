import { NotImplementedError } from "../errors";
import type { PaymentAdapter } from "./interface";

/** 階段 4 實作；端點與欄位以綠界官方最新文件為準。 */
export class EcpayPaymentAdapter implements PaymentAdapter {
  createCheckout(): never { throw new NotImplementedError("Payment.createCheckout"); }
  parseNotification(): never { throw new NotImplementedError("Payment.parseNotification"); }
  cancelSubscription(): never { throw new NotImplementedError("Payment.cancelSubscription"); }
  issueInvoice(): never { throw new NotImplementedError("Payment.issueInvoice"); }
}
