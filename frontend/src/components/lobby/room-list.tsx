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
    <section aria-label="Available Rooms" className="space-y-4">
      <div className="flex items-center justify-between gap-3">
        <h2 className="text-xl font-bold text-ivory">Available Rooms</h2>
        {!loading && error === null ? (
          <Badge tone="emerald">{rooms.length} Rooms</Badge>
        ) : null}
      </div>

      {loading ? (
        <LoadingState label="Loading rooms..." className="min-h-48" />
      ) : error !== null ? (
        <ErrorState
          title="Could not load rooms"
          message={error}
          action={
            <Button variant="premium" onClick={onRetry}>
              Try Again
            </Button>
          }
        />
      ) : rooms.length === 0 ? (
        <EmptyState
          icon={<Users className="size-6" aria-hidden />}
          title="No Rooms Available"
          description="There are no open rooms at the moment. Create a new room to start playing."
          action={
            <Link href="/rooms/create">
              <Button variant="premium">Create Room</Button>
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