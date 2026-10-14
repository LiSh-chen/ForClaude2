const TZ = "Asia/Taipei";

/** 顯示用：一律轉為台灣時間。 */
export function formatTaipei(date: Date, withTime = true): string {
  return new Intl.DateTimeFormat("zh-TW", {
    timeZone: TZ,
    year: "numeric",
    month: "numeric",
    day: "numeric",
    ...(withTime ? { hour: "2-digit", minute: "2-digit", hour12: false } : {}),
  }).format(date);
}

export function relativeTime(date: Date, now: Date = new Date()): string {
  const min = Math.floor((now.getTime() - date.getTime()) / 60_000);
  if (min < 1) return "剛剛";
  if (min < 60) return `${min} 分鐘前`;
  const hr = Math.floor(min / 60);
  if (hr < 24) return `${hr} 小時前`;
  const day = Math.floor(hr / 24);
  if (day < 30) return `${day} 天前`;
  return formatTaipei(date, false);
}

export const stars = (n: number) => "★".repeat(n) + "☆".repeat(5 - n);
