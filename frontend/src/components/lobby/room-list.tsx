import Link from "next/link";
import { Badge } from "@/components/ui/badge";
import { Button } from "@/components/ui/button";
import { EmptyState } from "@/components/ui/empty-state";
import { ErrorState } from "@/components/ui/error-state";
import { LoadingState } from "@/components/ui/loading-state";
import { Users } from "@/components/ui/icons";
import { RoomCard } from "./room-card";
import type { RoomSummary } from "@/types/room";

export function RoomList({
  rooms,
  loading,
  error,
  onRetry,
}: {
  rooms: RoomSummary[];
  loading: boolean;
  error: string | null;
  onRetry: () => void;
}) {
  return (
    <section aria-label="উপলব্ধ রুম" className="space-y-4">
      <div className="flex items-center justify-between gap-3">
        <h2 className="font-bengali text-xl font-bold text-ivory">উপলব্ধ রুম</h2>
        {!loading && error === null ? (
          <Badge tone="emerald">{rooms.length}টি রুম</Badge>
        ) : null}
      </div>

      {loading ? (
        <LoadingState label="রুম তালিকা লোড হচ্ছে…" className="min-h-48" />
      ) : error !== null ? (
        <ErrorState
          title="রুম লোড করা যায়নি"
          message={error}
          action={
            <Button variant="premium" onClick={onRetry}>
              আবার চেষ্টা করুন
            </Button>
          }
        />
      ) : rooms.length === 0 ? (
        <EmptyState
          icon={<Users className="size-6" aria-hidden />}
          title="কোনো রুম নেই"
          description="এই মুহূর্তে কোনো খোলা রুম নেই। নতুন রুম তৈরি করে খেলা শুরু করতে পারেন।"
          action={
            <Link href="/rooms/create">
              <Button variant="premium">রুম তৈরি করুন</Button>
            </Link>
          }
        />
      ) : (
        <div className="grid gap-4 sm:grid-cols-2 xl:grid-cols-3">
          {rooms.map((room) => (
            <RoomCard key={room.roomId} room={room} />
          ))}
        </div>
      )}
    </section>
  );
}