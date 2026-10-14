"use server";

import { redirect } from "next/navigation";
import { getDb } from "@/lib/db/client";
import { runTick } from "@/lib/jobs/tick";
import { newSimUser, sendSimEvent, userLog } from "@/lib/line/simulator";

const back = (u: string, extra = "") => redirect(`/dev/line?u=${encodeURIComponent(u)}${extra}`);

export async function simTextAction(formData: FormData) {
  const u = String(formData.get("userId"));
  const text = String(formData.get("text") ?? "").trim();
  if (text) {
    userLog().push({ userId: u, text, at: new Date() });
    await sendSimEvent(await getDb(), u, { type: "message", message: { type: "text", id: String(Date.now()), text } });
  }
  back(u);
}

export async function simPostbackAction(formData: FormData) {
  const u = String(formData.get("userId"));
  const data = String(formData.get("data"));
  const label = String(formData.get("label") ?? "");
  if (label) userLog().push({ userId: u, text: label, at: new Date() });
  await sendSimEvent(await getDb(), u, { type: "postback", postback: { data } });
  back(u);
}

export async function simFollowAction(formData: FormData) {
  const u = String(formData.get("userId"));
  const unfollow = formData.get("unfollow") === "1";
  await sendSimEvent(await getDb(), u, { type: unfollow ? "unfollow" : "follow" });
  back(u);
}

export async function simNewUserAction(formData: FormData) {
  const u = newSimUser(String(formData.get("name") ?? "").trim());
  await sendSimEvent(await getDb(), u, { type: "follow" });
  back(u);
}

export async function simTickAction(formData: FormData) {
  const u = String(formData.get("userId"));
  const r = await runTick(await getDb());
  back(u, `&tick=${encodeURIComponent(`新評論 ${r.sync.reduce((n, s) => n + s.created, 0)}、通知送出 ${r.notifications.sent}（保留 ${r.notifications.held}）、回覆檢查 ${r.replies.checked}（被拒 ${r.replies.rejected}）`)}`);
}
