export function formatDate(iso: string): string {
  return new Date(iso).toLocaleDateString("bn-BD", {
    day: "numeric",
    month: "long",
    year: "numeric",
  });
}

export function formatTime(iso: string): string {
  return new Date(iso).toLocaleTimeString("bn-BD", {
    hour: "numeric",
    minute: "2-digit",
  });
}

export function formatRatingChange(change: number): string {
  if (change > 0) return `+${change}`;
  if (change < 0) return `−${Math.abs(change)}`;
  return "±0";
}