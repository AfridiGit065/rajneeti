import { apiClient } from "@/lib/api-client";
import type { BackendProfile } from "@/types/backend";
import type { Result } from "@/types/api";

export interface ProfileRepository {
  getProfile(): Promise<Result<BackendProfile>>;
  updateProfile(input: { username: string; avatarUrl: string }): Promise<Result<BackendProfile>>;
}

export class RestProfileRepository implements ProfileRepository {
  getProfile() { return apiClient.get<BackendProfile>("/api/profile"); }
  updateProfile(input: { username: string; avatarUrl: string }) { return apiClient.put<BackendProfile>("/api/profile", input); }
}
