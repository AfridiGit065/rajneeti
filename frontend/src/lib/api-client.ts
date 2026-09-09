import { err, ok, type ApiError, type ApiResponse, type Result } from "@/types/api";

const API_URL = (process.env.NEXT_PUBLIC_API_URL ?? "http://localhost:8080").replace(/\/$/, "");

let getAccessToken: (() => string | null) | null = null;
let onUnauthorized: (() => void) | null = null;

export function configureApiClient(options: {
  getAccessToken: () => string | null;
  onUnauthorized: () => void;
}) {
  getAccessToken = options.getAccessToken;
  onUnauthorized = options.onUnauthorized;
}

function errorFromResponse(status: number, body: Partial<ApiError>): ApiError {
  return {
    status,
    error: body.error ?? "REQUEST_FAILED",
    message: body.message ?? "The request could not be completed.",
    timestamp: body.timestamp ?? new Date().toISOString(),
    ...(body.path ? { path: body.path } : {}),
    ...(body.errors ? { errors: body.errors } : {}),
  };
}

async function request<T>(method: string, path: string, body?: unknown): Promise<Result<T>> {
  const token = getAccessToken?.();
  try {
    const response = await fetch(`${API_URL}${path}`, {
      method,
      headers: {
        Accept: "application/json",
        ...(body === undefined ? {} : { "Content-Type": "application/json" }),
        ...(token ? { Authorization: `Bearer ${token}` } : {}),
      },
      ...(body === undefined ? {} : { body: JSON.stringify(body) }),
    });
    const payload: ApiResponse<T> | ApiError | null = await response.json().catch(() => null);

    if (!response.ok || !payload || !("success" in payload) || !payload.success) {
      const error = errorFromResponse(response.status, payload ?? {});
      if (response.status === 401 && token) onUnauthorized?.();
      return err<T>(error);
    }
    return ok(payload.data);
  } catch {
    return err<T>({ status: 0, error: "NETWORK_ERROR", message: "Unable to reach the server. Please try again." });
  }
}

export const apiClient = {
  get: <T>(path: string) => request<T>("GET", path),
  post: <T>(path: string, body?: unknown) => request<T>("POST", path, body),
  put: <T>(path: string, body?: unknown) => request<T>("PUT", path, body),
  patch: <T>(path: string, body?: unknown) => request<T>("PATCH", path, body),
  delete: <T>(path: string) => request<T>("DELETE", path),
};
