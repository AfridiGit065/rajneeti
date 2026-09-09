import type { AuthRepository } from "../auth-repository";
import type { AuthSession, LoginInput, RegisterInput, UserPublic } from "@/types/user";
import { err, ok, type Result } from "@/types/api";
import { MOCK_CURRENT_USER, MOCK_DEMO_CREDENTIALS, MOCK_USERS } from "@/mocks/users";

const delay = (ms = 450) => new Promise((r) => setTimeout(r, ms));

function buildSession(user: UserPublic): AuthSession {
  return {
    accessToken: `mock-access.${user.id}.${Date.now()}`,
    refreshToken: `mock-refresh.${user.id}`,
    user,
    expiresAt: new Date(Date.now() + 1000 * 60 * 60 * 24).toISOString(),
  };
}

export class MockAuthRepository implements AuthRepository {
  async login(input: LoginInput): Promise<Result<AuthSession>> {
    await delay();
    const valid =
      input.email.trim().toLowerCase() === MOCK_DEMO_CREDENTIALS.email.toLowerCase() &&
      input.password === MOCK_DEMO_CREDENTIALS.password;
    if (!valid) {
      return err({
        status: 401,
        error: "INVALID_CREDENTIALS",
        message: "ভুল ইমেইল বা পাসওয়ার্ড।",
      });
    }
    return ok(buildSession(MOCK_CURRENT_USER));
  }

  async register(input: RegisterInput): Promise<Result<UserPublic>> {
    await delay(700);
    if (MOCK_USERS.some((u) => u.username.toLowerCase() === input.username.toLowerCase())) {
      return err({
        status: 409,
        error: "USERNAME_ALREADY_EXISTS",
        message: "এই ইউজারনেমটি ইতিমধ্যে ব্যবহৃত।",
      });
    }
    if (!input.email.includes("@")) {
      return err({
        status: 400,
        error: "INVALID_EMAIL",
        message: "সঠিক ইমেইল ঠিকানা দিন।",
      });
    }
    return ok(
      {
        id: "u-new",
        username: input.username,
        displayName: input.displayName,
        avatarInitial: input.displayName.slice(0, 1) || "র",
        level: 1,
      },
    );
  }

  async checkUsername(username: string): Promise<Result<{ available: boolean }>> {
    await delay(220);
    const taken = MOCK_USERS.some((u) => u.username.toLowerCase() === username.toLowerCase());
    return ok({ available: !taken });
  }

  async refresh(refreshToken: string): Promise<Result<AuthSession>> {
    await delay(200);
    if (!refreshToken.startsWith("mock-refresh")) {
      return err({ status: 401, error: "INVALID_TOKEN", message: "টোকেন মেয়াদোত্তীর্ণ।" });
    }
    return ok(buildSession(MOCK_CURRENT_USER));
  }

  async logout(_refreshToken?: string): Promise<Result<void>> {
    await delay(200);
    return ok(undefined);
  }

  async getCurrentUser(): Promise<Result<UserPublic>> {
    await delay(200);
    return ok(MOCK_CURRENT_USER);
  }
}
