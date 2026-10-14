import { redirect } from "next/navigation";

export type Flash = { notice?: string; error?: string };

/** 導向某頁並帶上一則提示訊息（由 <Flash/> 顯示）。 */
export function go(path: string, flash: Flash = {}): never {
  const q = new URLSearchParams();
  if (flash.notice) q.set("notice", flash.notice);
  if (flash.error) q.set("error", flash.error);
  redirect(q.size ? `${path}${path.includes("?") ? "&" : "?"}${q}` : path);
}
