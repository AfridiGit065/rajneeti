import { apiClient } from "@/lib/api-client";
import type { BackendProfile } from "@/types/backend";
import type { Result } from "@/types/api";

const API_URL = (process.env.NEXT_PUBLIC_API_URL ?? "http://localhost:8080").replace(/\/$/, "");

export interface ProfileRepository {
  getProfile(): Promise<Result<BackendProfile>>;
  updateProfile(input: { username: string; avatarUrl: string }): Promise<Result<BackendProfile>>;
  downloadStatisticsPdf(getToken: () => string | null): Promise<Result<Blob>>;
}

export class RestProfileRepository implements ProfileRepository {
  getProfile() { return apiClient.get<BackendProfile>("/api/profile"); }
  updateProfile(input: { username: string; avatarUrl: string }) { return apiClient.put<BackendProfile>("/api/profile", input); }

  async downloadStatisticsPdf(getToken: () => string | null): Promise<Result<Blob>> {
    const token = getToken();
    try {
      const response = await fetch(`${API_URL}/api/profile/statistics/pdf`, {
        method: "GET",
        headers: {
          Accept: "application/pdf",
          ...(token ? { Authorization: `Bearer ${token}` } : {}),
        },
      });
      if (!response.ok) {
        return {
          ok: false,
          error: {
            status: response.status,
            error: "PDF_DOWNLOAD_FAILED",
            message: `Failed to download statistics PDF (HTTP ${response.status}).`,
            timestamp: new Date().toISOString(),
          },
        };
      }
      const blob = await response.blob();
      return { ok: true, data: blob };
    } catch {
      return {
        ok: false,
        error: {
          status: 0,
          error: "NETWORK_ERROR",
          message: "Unable to reach the server. Please try again.",
          timestamp: new Date().toISOString(),
        },
      };
    }
  }
}
