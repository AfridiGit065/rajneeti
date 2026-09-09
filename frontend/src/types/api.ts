export interface ApiResponse<T> {
  success: boolean;
  message: string;
  data: T;
  timestamp: string;
}

export interface ApiError {
  status: number;
  error: string;
  message: string;
  path?: string;
  timestamp: string;
  errors?: Array<{ field: string; message: string }>;
}

/** Generic repository result, decoupled from the transport used. */
export type Result<T> =
  | { ok: true; data: T }
  | { ok: false; error: ApiError };

export function ok<T>(data: T): Result<T> {
  return { ok: true, data };
}

export function err<T>(error: Partial<ApiError> = {}): Result<T> {
  return {
    ok: false,
    error: {
      status: error.status ?? 500,
      error: error.error ?? "INTERNAL_ERROR",
      message: error.message ?? "Something went wrong.",
      timestamp: new Date().toISOString(),
      ...(error.path ? { path: error.path } : {}),
      ...(error.errors ? { errors: error.errors } : {}),
    },
  };
}