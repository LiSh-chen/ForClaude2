import type { GoogleReview } from "./interface";

/** 可重現的亂數（mulberry32），讓測試與 demo 結果穩定。 */
export function createRng(seed: number) {
  let a = seed >>> 0;
  return () => {
    a = (a + 0x6d2b79f5) >>> 0;
    let t = a;
    t = Math.imul(t ^ (t >>> 15), t | 1);
    t ^= t + Math.imul(t ^ (t >>> 7), t | 61);
    return ((t ^ (t >>> 14)) >>> 0) / 4294967296;
  };
}

interface Sample { stars: 1 | 2 | 3 | 4 | 5; text: string | null }

/** 涵蓋各星等、無文字、中／英／日／中英混雜、含風險內容。 */
export const SAMPLE_REVIEWS: Sample[] = [
  { stars: 5, text: "牛肉麵湯頭很濃郁，麵條Q彈，店員也很親切，下次會再來！" },
  { stars: 5, text: "Amazing ramen! The broth was rich and the staff were super friendly." },
  { stars: 5, text: "スープが濃厚でとても美味しかったです。また来ます！" },
  { stars: 5, text: null },
  { stars: 4, text: "整體不錯，環境乾淨，只是假日人多要等一下。" },
  { stars: 4, text: "CP值很高，lunch set 很划算，服務生很有礼貌 👍" },
  { stars: 4, text: null },
  { stars: 3, text: "餐點普通，沒有特別驚艷，價格偏高。" },
  { stars: 3, text: "Food was okay but service was slow." },
  { stars: 2, text: "等了四十分鐘才上菜，詢問後店員態度也不太好。" },
  { stars: 2, text: "The place was noisy and the table was sticky." },
  { stars: 1, text: "湯是冷的，而且吃到一根頭髮，非常噁心！" },
  { stars: 1, text: "吃完後上吐下瀉，疑似食物中毒，我會去向衛生局檢舉。" },
  { stars: 1, text: "老闆態度惡劣還罵人，我要請律師處理。" },
  { stars: 1, text: null },
  { stars: 1, text: "Worst experience ever. Never coming back." },
  { stars: 5, text: "這家店根本是自己人在洗評價吧，到處都是五星。" },
];

const NAMES = ["王小明", "Jessica L.", "陳雅婷", "Kenji T.", "林先生", "A Google User", "小美"];

export function generateReviews(opts: { seed: number; count: number; now?: Date; idPrefix?: string }): GoogleReview[] {
  const rng = createRng(opts.seed);
  const now = (opts.now ?? new Date()).getTime();
  const prefix = opts.idPrefix ?? "mock-review";
  return Array.from({ length: opts.count }, (_, i) => {
    const sample = SAMPLE_REVIEWS[Math.floor(rng() * SAMPLE_REVIEWS.length)]!;
    // 越小的 index 越新；每則間隔數小時到數天
    const created = new Date(now - (i + 1) * (3_600_000 * (2 + Math.floor(rng() * 40))));
    return {
      googleReviewId: `${prefix}-${opts.seed}-${i}`,
      starRating: sample.stars,
      comment: sample.text,
      reviewerDisplayName: NAMES[Math.floor(rng() * NAMES.length)]!,
      createTime: created,
      updateTime: created,
      reply: null,
    };
  });
}
