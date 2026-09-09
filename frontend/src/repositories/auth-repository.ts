import type {
  AuthSession,
  LoginInput,
  RegisterInput,
  UserPublic,
} from "@/types/user";
import type { Result } from "@/types/api";

export interface AuthRepository {
  login(input: LoginInput): Promise<Result<AuthSession>>;
  register(input: RegisterInput): Promise<Result<AuthSession>>;
  refresh(refreshToken: string): Promise<Result<AuthSession>>;
  logout(): Promise<Result<void>>;
  getCurrentUser(): Promise<Result<UserPublic>>;
  checkUsername(username: string): Promise<Result<{ available: boolean }>>;
}