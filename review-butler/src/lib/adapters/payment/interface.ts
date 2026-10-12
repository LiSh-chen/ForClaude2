export interface CheckoutRequest {
  organizationId: string;
  plan: string;
  amountTwd: number;
}

export interface CheckoutSession {
  /** 導向綠界付款頁用的表單欄位或 URL */
  merchantTradeNo: string;
  redirectUrl: string;
}

export interface PaymentNotification {
  merchantTradeNo: string;
  tradeNo: string;
  amountTwd: number;
  success: boolean;
}

export interface InvoiceRequest {
  tradeNo: string;
  amountTwd: number;
  email: string;
  taxId?: string;
}

export interface PaymentAdapter {
  createCheckout(req: CheckoutRequest): Promise<CheckoutSession>;
  /** 驗證綠界回傳通知的檢查碼；驗證失敗回傳 null。冪等處理由呼叫端負責（依 tradeNo）。 */
  parseNotification(payload: Record<string, string>): PaymentNotification | null;
  cancelSubscription(merchantTradeNo: string): Promise<void>;
  issueInvoice(req: InvoiceRequest): Promise<{ invoiceNumber: string }>;
}
