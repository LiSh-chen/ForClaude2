"use client";

import { useFormStatus } from "react-dom";
import { Button } from "@/components/ui/button";

/** 送出時自動停用並顯示處理中文字，避免 AI 產生期間重複點擊。 */
export function SubmitButton({ children, pendingText = "處理中…", ...props }: React.ComponentProps<typeof Button> & { pendingText?: string }) {
  const { pending } = useFormStatus();
  return (
    <Button type="submit" disabled={pending || props.disabled} {...props}>
      {pending ? pendingText : children}
    </Button>
  );
}
