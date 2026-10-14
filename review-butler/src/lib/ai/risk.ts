export const RISK_FLAGS = ["疑似假評論", "涉及食安", "涉及人身攻擊", "提到法律行動"] as const;
export type RiskFlag = (typeof RISK_FLAGS)[number];

const REMINDERS: Record<RiskFlag, string> = {
  疑似假評論: "此評論疑似不實，建議先確認是否為真實顧客，再決定是否回覆。",
  涉及食安: "此評論涉及食安，建議親自處理。",
  涉及人身攻擊: "此評論涉及人身攻擊，建議親自處理，避免情緒性回覆。",
  提到法律行動: "此評論提到法律行動，建議親自處理，必要時先諮詢專業人士。",
};

/**
 * 給店家看的提醒（SPEC F4 規則 9）。
 * 刻意與草稿本文分開存放，只顯示在介面上——否則一鍵發布時提醒文字會被一起發到 Google。
 */
export function riskReminders(flags: string[]): string[] {
  return flags.filter((f): f is RiskFlag => f in REMINDERS).map((f) => REMINDERS[f]);
}
