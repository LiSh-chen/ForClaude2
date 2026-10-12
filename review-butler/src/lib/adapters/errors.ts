export class NotImplementedError extends Error {
  constructor(what: string) {
    super(`${what} 尚未實作（real adapter 將在後續階段完成）`);
    this.name = "NotImplementedError";
  }
}

/** Google 授權被撤銷或過期 → 需通知店家重新授權（F1） */
export class AuthRevokedError extends Error {
  constructor(message = "Google authorization revoked") {
    super(message);
    this.name = "AuthRevokedError";
  }
}

/** 速率限制 → 呼叫端以指數退避重試（F3） */
export class RateLimitError extends Error {
  constructor(public retryAfterMs?: number) {
    super("Rate limited");
    this.name = "RateLimitError";
  }
}
