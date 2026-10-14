"use server";

import { z } from "zod";
import { getLine } from "@/lib/adapters";
import { requireUser } from "@/lib/auth";
import { go } from "@/lib/flash";
import { createBindCode, removeBinding, sendTestMessage, setBindingLocations } from "@/lib/line/bind";

const uuid = z.string().uuid();

export async function createBindCodeAction() {
  const { db, org } = await requireUser();
  await createBindCode(db, org.id);
  go("/dashboard/line", { notice: "綁定碼已產生，10 分鐘內有效。請把它輸入到 LINE 官方帳號的對話框。" });
}

export async function saveBindingLocationsAction(formData: FormData) {
  const { db, org } = await requireUser();
  const ids = formData.getAll("locationId").map(String).filter((v) => uuid.safeParse(v).success);
  const ok = await setBindingLocations(db, org.id, uuid.parse(formData.get("bindingId")), ids);
  go("/dashboard/line", ok ? { notice: "已更新要接收通知的店點" } : { error: "找不到這個綁定" });
}

export async function testBindingAction(formData: FormData) {
  const { db, org } = await requireUser();
  const res = await sendTestMessage(db, getLine(), org.id, uuid.parse(formData.get("bindingId")));
  go("/dashboard/line", res.ok ? { notice: "已傳送測試訊息，請看 LINE" } : { error: `傳送失敗：${res.error}（對方可能還沒加官方帳號好友，或已封鎖）` });
}

export async function removeBindingAction(formData: FormData) {
  const { db, org } = await requireUser();
  const ok = await removeBinding(db, org.id, uuid.parse(formData.get("bindingId")));
  go("/dashboard/line", ok ? { notice: "已解除綁定，這位不會再收到通知" } : { error: "找不到這個綁定" });
}
