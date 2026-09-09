export interface BackendUser {
  id: string;
  username: string;
  email: string;
  avatarUrl?: string;
  rating: number;
  totalMatches: number;
  wins: number;
  losses: number;
  createdAt?: string;
}

export interface BackendAuthResponse {
  accessToken: string;
  refreshToken: string;
  tokenType: string;
  expiresIn: number;
  user: BackendUser;
}

export interface BackendProfile extends BackendUser {
  winRate: number;
}

export interface BackendRoomPlayer {
  id: string;
  username: string;
  avatarUrl?: string;
  seatNumber: number;
  ready: boolean;
  isHost: boolean;
  joinedAt: string;
}

export interface BackendRoom {
  id: string;
  roomCode: string;
  hostId: string;
  hostUsername: string;
  status: "WAITING" | "IN_GAME" | "FINISHED" | "CANCELLED";
  maxPlayers: number;
  currentPlayers: number;
  canStart: boolean;
  players: BackendRoomPlayer[];
  createdAt: string;
}
