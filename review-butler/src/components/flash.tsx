"use client";

import { useSearchParams } from "next/navigation";

/** 顯示 server action 以 ?notice= / ?error= 帶回來的提示訊息。 */
export function Flash() {
  const params = useSearchParams();
  const notice = params.get("notice");
  const error = params.get("error");
  if (!notice && !error) return null;
  return (
    <p role={error ? "alert" : "status"} className={error ? "rounded-md bg-red-50 p-3 text-sm text-red-800" : "rounded-md bg-green-50 p-3 text-sm text-green-800"}>
      {error ?? notice}
    </p>
  );
}
