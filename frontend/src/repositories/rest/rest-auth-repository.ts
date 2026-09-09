import { apiClient } from "@/lib/api-client";
import type { BackendAuthResponse, BackendUser } from "@/types/backend";
import { err, type Result } from "@/types/api";
import type { AuthRepository } from "../auth-repository";
import type { AuthSession, LoginInput, RegisterInput, UserPublic } from "@/types/user";

export function toUserPublic(user: BackendUser): UserPublic {
  return {
    id: user.id,
    username: user.username,
    displayName: user.username,
    avatarInitial: user.username.slice(0, 2).toUpperCase(),
    level: 1,
    email: user.email,
    avatarUrl: user.avatarUrl ?? "",
    rating: user.rating,
  };
}

function toSession(response: BackendAuthResponse): AuthSession {
  return {
    accessToken: response.accessToken,
    refreshToken: response.refreshToken,
    expiresAt: new Date(Date.now() + response.expiresIn * 1000).toISOString(),
    user: toUserPublic(response.user),
  };
}

export class RestAuthRepository implements AuthRepository {
  async login(input: LoginInput): Promise<Result<AuthSession>> {
    const result = await apiClient.post<BackendAuthResponse>("/api/auth/login", input);
    return result.ok ? { ok: true, data: toSession(result.data) } : result;
  }

  async register(input: RegisterInput): Promise<Result<UserPublic>> {
    const result = await apiClient.post<BackendUser>("/api/auth/register", {
      username: input.username,
      email: input.email,
      password: input.password,
    });
    return result.ok ? { ok: true, data: toUserPublic(result.data) } : result;
  }

  async refresh(refreshToken: string): Promise<Result<AuthSession>> {
    const result = await apiClient.post<BackendAuthResponse>("/api/auth/refresh", { refreshToken });
    return result.ok ? { ok: true, data: toSession(result.data) } : result;
  }

  async logout(refreshToken?: string): Promise<Result<void>> {
    return apiClient.post<void>("/api/auth/logout", refreshToken ? { refreshToken } : undefined);
  }

  async getCurrentUser(): Promise<Result<UserPublic>> {
    const result = await apiClient.get<BackendUser>("/api/auth/me");
    return result.ok ? { ok: true, data: toUserPublic(result.data) } : result;
  }

  async checkUsername(): Promise<Result<{ available: boolean }>> {
    return err<{ available: boolean }>({ status: 501, error: "NOT_SUPPORTED", message: "Username availability is checked when you register." });
  }
}
