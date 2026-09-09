import { GameBoard } from "@/components/game/game-board";

export default async function GameBoardPage({
  params,
}: {
  params: Promise<{ matchId: string }>;
}) {
  const { matchId } = await params;
  return <GameBoard matchId={matchId} />;
}