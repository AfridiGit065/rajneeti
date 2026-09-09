export type ClassValue = string | number | null | false | undefined;
export type ClassArray = ClassValue[];
export type ClassDictionary = Record<string, unknown>;
export type ClassInput = ClassValue | ClassArray | ClassDictionary;

function isObject(value: unknown): value is ClassDictionary {
  return typeof value === "object" && value !== null && !Array.isArray(value);
}

/** Tiny className combiner (clsx-equivalent, zero deps). */
export function cn(...inputs: ClassInput[]): string {
  const parts: string[] = [];
  for (const input of inputs) {
    if (!input) continue;
    if (typeof input === "string" || typeof input === "number") {
      parts.push(String(input));
    } else if (Array.isArray(input)) {
      parts.push(cn(...input));
    } else if (isObject(input)) {
      for (const [key, value] of Object.entries(input)) {
        if (value) parts.push(key);
      }
    }
  }
  return parts.join(" ");
}

/** Returns a stable id generator for UI keys. */
export function createId(prefix: string): string {
  return `${prefix}-${Date.now().toString(36)}-${Math.random().toString(36).slice(2, 8)}`;
}

/** Formats a coin amount with a gold discount tone. */
export function formatCoins(value: number): string {
  return new Intl.NumberFormat("en-US").format(value);
}