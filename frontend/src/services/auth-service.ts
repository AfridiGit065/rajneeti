import { repositories } from "@/repositories";
import type { LoginInput, RegisterInput } from "@/types/user";
import type { Result, ApiError } from "@/types/api";

export interface AuthResult {
  session: { accessToken: string; refreshToken: string; expiresAt: string };
  user: { id: string; username: string; displayName: string; avatarInitial: string; level: number };
  redirect?: string;
}

export const AuthService = {
  async login(input: LoginInput): Promise<Result<AuthResult>> {
    const result = await repositories.auth.login(input);
    if (!result.ok) return result;
    return {
      ok: true,
      data: {
        session: {
          accessToken: result.data.accessToken,
          refreshToken: result.data.refreshToken,
          expiresAt: result.data.expiresAt,
        },
        user: result.data.user,
      },
    };
  },

  async register(input: RegisterInput): Promise<Result<AuthResult>> {
    const result = await repositories.auth.register(input);
    if (!result.ok) return result;
    return {
      ok: true,
      data: {
        session: {
          accessToken: result.data.accessToken,
          refreshToken: result.data.refreshToken,
          expiresAt: result.data.expiresAt,
        },
        user: result.data.user,
      },
    };
  },

  async logout(): Promise<Result<void>> {
    return repositories.auth.logout();
  },

  async checkUsername(username: string): Promise<Result<{ available: boolean }>> {
    return repositories.auth.checkUsername(username);
  },

  normalizeError(error: ApiError): string {
    return error.message;
  },
};