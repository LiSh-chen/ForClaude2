import { createHash, randomUUID } from "node:crypto";
import type {
  CheckoutRequest,
  CheckoutSession,
  InvoiceRequest,
  PaymentAdapter,
  PaymentNotification,
} from "./interface";

/**
 * Mock 的檢查碼只是示意（SHA-256），**不是**綠界的 CheckMacValue 演算法；
 * 真實演算法在階段 4 的 ecpay.ts 實作。
 */
export class MockPaymentAdapter implements PaymentAdapter {
  readonly canceled: string[] = [];

  constructor(private hashKey = "mock-hash-key") {}

  macFor(payload: Record<string, string>): string {
    const base = Object.keys(payload)
      .filter((k) => k !== "CheckMacValue")
      .sort()
      .map((k) => `${k}=${payload[k]}`)
      .join("&");
    return createHash("sha256").update(`${this.hashKey}&${base}`).digest("hex");
  }

  async createCheckout(req: CheckoutRequest): Promise<CheckoutSession> {
    const merchantTradeNo = `MOCK${randomUUID().replace(/-/g, "").slice(0, 16)}`;
    return { merchantTradeNo, redirectUrl: `/mock-checkout?trade=${merchantTradeNo}&amount=${req.amountTwd}` };
  }

  parseNotification(payload: Record<string, string>): PaymentNotification | null {
    if (!payload.CheckMacValue || payload.CheckMacValue !== this.macFor(payload)) return null;
    return {
      merchantTradeNo: payload.MerchantTradeNo ?? "",
      tradeNo: payload.TradeNo ?? "",
      amountTwd: Number(payload.TradeAmt ?? 0),
      success: payload.RtnCode === "1",
    };
  }

  async cancelSubscription(merchantTradeNo: string) {
    this.canceled.push(merchantTradeNo);
  }

  async issueInvoice(req: InvoiceRequest) {
    return { invoiceNumber: `MK${req.tradeNo.slice(-8).padStart(8, "0")}` };
  }
}
