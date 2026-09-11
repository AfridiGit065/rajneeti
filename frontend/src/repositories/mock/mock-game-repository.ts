import type { GameRepository } from "../game-repository";
import type { ActionIntent, GameResult, GameState } from "@/types/game";
import { err, ok, type Result } from "@/types/api";
import { MOCK_FINISHED_GAME_STATE, MOCK_GAME_STATE } from "@/mocks/matches";

const delay = (ms = 400) => new Promise((r) => setTimeout(r, ms));

export class MockGameRepository implements GameRepository {
  private state: GameState = MOCK_GAME_STATE;

  async getGameState(matchId: string): Promise<Result<GameState>> {
    await delay(250);
    if (matchId === "match-2") {
      return ok(MOCK_FINISHED_GAME_STATE);
    }
    return ok(this.state);
  }

  async performAction(
    _matchId: string,
    intent: ActionIntent,
  ): Promise<Result<GameState>> {
    await delay(550);
    this.state = {
      ...this.state,
      activeAction: intent,
      phase: "action_resolution",
      log: [
        {
          id: `log-${Date.now()}`,
          timestamp: new Date().toISOString(),
          text: buildActionText(intent),
          kind: "action",
        },
        ...this.state.log,
      ],
    };
    return ok(this.state);
  }

  async challenge(_matchId: string, _targetPlayerId: string): Promise<Result<GameState>> {
    await delay(550);
    const actor = this.state.activeAction;
    if (!actor) {
      return err({
        status: 400,
        error: "NO_ACTIVE_ACTION",
        message: "কোনো সক্রিয় অ্যাকশন নেই।",
      });
    }
    this.state = {
      ...this.state,
      phase: "challenge_resolution",
      log: [
        {
          id: `log-${Date.now()}`,
          timestamp: new Date().toISOString(),
          text: "চ্যালেঞ্জ ঘোষণা করা হয়েছে!",
          kind: "challenge",
        },
        ...this.state.log,
      ],
    };
    return ok(this.state);
  }

  async block(_matchId: string, _claimedCharacter: string): Promise<Result<GameState>> {
    await delay(500);
    this.state = {
      ...this.state,
      phase: "block_resolution",
      log: [
        {
          id: `log-${Date.now()}`,
          timestamp: new Date().toISOString(),
          text: "ব্লক করা হয়েছে!",
          kind: "block",
        },
        ...this.state.log,
      ],
    };
    return ok(this.state);
  }

  async endTurn(_matchId: string, _playerId: string): Promise<Result<GameState>> {
    await delay(350);
    return ok(this.state);
  }

  async getGameResult(matchId: string): Promise<Result<GameResult>> {
    await delay(300);
    if (matchId === "match-2") {
      return ok({
        matchId,
        winnerId: MOCK_FINISHED_GAME_STATE.winnerPlayerId ?? "",
        winnerName: "শাপলা",
        finishedAt: MOCK_FINISHED_GAME_STATE.endedAt ?? new Date().toISOString(),
        turnCount: MOCK_FINISHED_GAME_STATE.turnNumber,
      });
    }
    return ok({
      matchId,
      winnerId: "p-1",
      winnerName: "শাপলা",
      finishedAt: new Date().toISOString(),
      turnCount: 11,
    });
  }

  async resolveForeignAid(_matchId: string, blocked: boolean): Promise<Result<GameState>> {
    await delay(350);
    if (!blocked) {
      const actor = this.state.players.find((p) => p.userId === (this.state.activeAction as { targetPlayerId?: string } | null)?.targetPlayerId) ?? this.state.players[0];
      if (actor) actor.coins += 2;
    }
    this.state = { ...this.state, activeAction: null };
    return ok(this.state);
  }

  async confirmExchange(_matchId: string, keepCardIds: string[]): Promise<Result<GameState>> {
    await delay(550);
    // Simulate: keep only the selected cards, advance turn
    const keepSet = new Set(keepCardIds);
    this.state = {
      ...this.state,
      players: this.state.players.map((p) =>
        p.userId === (this.state.activeAction as { actorUserId?: string } | null)?.actorUserId
          ? { ...p, influenceCards: p.influenceCards.filter((c) => keepSet.has(c.id)).slice(0, 2) }
          : p,
      ),
      activeAction: null,
      log: [
        { id: `log-${Date.now()}`, timestamp: new Date().toISOString(), text: "কার্ড বদল সম্পন্ন হয়েছে!", kind: "action" },
        ...this.state.log,
      ],
    };
    return ok(this.state);
  }

  async resolveAssassinate(_matchId: string, succeeded: boolean): Promise<Result<GameState>> {
    await delay(550);
    if (succeeded) {
      const intent = this.state.activeAction as { targetPlayerId?: string } | null;
      const target = this.state.players.find((p) => p.userId === intent?.targetPlayerId);
      const actor = this.state.players.find((p) => p.isTurn) ?? this.state.players[0];
      actor.coins = Math.max(0, actor.coins - 3);
      if (target && target.influenceCards.length > 0) {
        target.influenceCards = target.influenceCards.slice(1);
      }
    }
    this.state = {
      ...this.state,
      activeAction: null,
      log: [
        {
          id: `log-${Date.now()}`,
          timestamp: new Date().toISOString(),
          text: succeeded ? "সরিয়ে দেওয়া সফল হয়েছে!" : "সরিয়ে দেওয়া প্রতিহত হয়েছে!",
          kind: succeeded ? "action" : "block",
        },
        ...this.state.log,
      ],
    };
    return ok(this.state);
  }
}

function buildActionText(intent: ActionIntent): string {
  switch (intent.action) {
    case "income":
      return "আয়: +1 কয়েন অর্জন করল।";
    case "foreign_aid":
      return "বিদেশি অনুদান: +2 কয়েন অর্জন করল।";
    case "tax":
      return "কর আদায়: মন্ত্রী দাবি করা হয়েছে — চ্যালেঞ্জের জন্য অপেক্ষমাণ (+3 কয়েন অনুমোদন সাপেক্ষে)।";
    case "steal":
      return "চুরি: দালাল দাবি করা হয়েছে — চ্যালেঞ্জ/ব্লকের জন্য অপেক্ষমাণ (+2 কয়েন অনুমোদন সাপেক্ষে)।";
    case "exchange":
      return "কার্ড বদল: আমলা দাবি করে কার্ড বদল করল।";
    case "assassinate":
      return "সরিয়ে দেওয়া: ঘাতক দাবি করল!";
    case "coup":
      return "ক্ষমতা দখল: 7 কয়েন দিয়ে কোপ!";
  }
}