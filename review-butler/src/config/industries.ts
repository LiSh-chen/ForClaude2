/** 產業別與預設分類主題（SPEC F4：主題清單依產業別設定）。 */
export const INDUSTRIES = ["food", "beauty", "medical", "education", "other"] as const;
export type Industry = (typeof INDUSTRIES)[number];

export const DEFAULT_TOPICS: Record<Industry, string[]> = {
  food: ["服務態度", "等候時間", "價格", "餐點品質", "環境衛生", "其他"],
  beauty: ["服務態度", "技術專業", "價格", "預約與等候", "環境衛生", "其他"],
  medical: ["服務態度", "診療專業", "等候時間", "價格", "環境衛生", "其他"],
  education: ["教學品質", "服務態度", "價格", "環境設施", "溝通聯繫", "其他"],
  other: ["服務態度", "等候時間", "價格", "品質", "環境", "其他"],
};
