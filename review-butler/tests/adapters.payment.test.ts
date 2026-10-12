import { describe, expect, it } from "vitest";
import { MockPaymentAdapter } from "@/lib/adapters/payment/mock";

describe("MockPaymentAdapter", () => {
  const pay = new MockPaymentAdapter();
  const base = { MerchantTradeNo: "T1", TradeNo: "9", TradeAmt: "490", RtnCode: "1" };

  it("accepts a correctly signed notification", () => {
    const n = pay.parseNotification({ ...base, CheckMacValue: pay.macFor(base) });
    expect(n).toMatchObject({ merchantTradeNo: "T1", amountTwd: 490, success: true });
  });
  it("rejects tampered or unsigned notifications", () => {
    const mac = pay.macFor(base);
    expect(pay.parseNotification({ ...base, TradeAmt: "1", CheckMacValue: mac })).toBeNull();
    expect(pay.parseNotification(base)).toBeNull();
  });
  it("creates checkout, cancels and issues invoice", async () => {
    const s = await pay.createCheckout({ organizationId: "o", plan: "pro", amountTwd: 490 });
    expect(s.merchantTradeNo).toMatch(/^MOCK/);
    await pay.cancelSubscription(s.merchantTradeNo);
    expect(pay.canceled).toContain(s.merchantTradeNo);
    expect((await pay.issueInvoice({ tradeNo: "123456789", amountTwd: 490, email: "a@b.c" })).invoiceNumber).toBeTruthy();
  });
});
