/**
 * 方案與額度（SPEC §2）。額度集中在這裡，業務程式碼不得寫死數字。
 * 之後若需要在後台調整，可改成讀資料庫，介面維持 getPlan()。
 */
export type PlanCode = "trial" | "basic" | "pro";

export interface PlanConfig {
  code: PlanCode;
  name: string;
  /** 月費（新台幣），試用為 0 */
  priceTwd: number;
  maxLocations: number;
  monthlyDrafts: number;
  features: {
    monthlyReport: boolean;
    autoReplyPositive: boolean;
  };
  /** 僅試用方案有值 */
  trialDays?: number;
}

export const PLANS: Record<PlanCode, PlanConfig> = {
  // 試用 = 進階方案全部功能、不需綁卡
  trial: {
    code: "trial",
    name: "免費試用",
    priceTwd: 0,
    maxLocations: 3,
    monthlyDrafts: 400,
    features: { monthlyReport: true, autoReplyPositive: true },
    trialDays: 14,
  },
  basic: {
    code: "basic",
    name: "基本",
    priceTwd: 299,
    maxLocations: 1,
    monthlyDrafts: 100,
    features: { monthlyReport: false, autoReplyPositive: false },
  },
  pro: {
    code: "pro",
    name: "進階",
    priceTwd: 490,
    maxLocations: 3,
    monthlyDrafts: 400,
    features: { monthlyReport: true, autoReplyPositive: true },
  },
};

export function getPlan(code: PlanCode): PlanConfig {
  return PLANS[code];
}
