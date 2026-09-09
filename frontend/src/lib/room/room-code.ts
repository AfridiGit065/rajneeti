export const ROOM_CODE_PATTERN = /^[A-Z0-9]{5,6}$/;

/**
 * Normalizes a raw room-code input: strips spaces/dashes, uppercases,
 * keeps only A-Z and 0-9, and truncates to 6 chars (RAJ123 style).
 */
export function normalizeRoomCode(input: string): string {
  return input
    .toUpperCase()
    .replace(/[^A-Z0-9]/g, "")
    .slice(0, 6);
}

export function isValidRoomCode(code: string): boolean {
  return ROOM_CODE_PATTERN.test(code);
}