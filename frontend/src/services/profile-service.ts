import { RestProfileRepository } from "@/repositories/profile-repository";
import { useAuthStore } from "@/store/auth-store";

const repository = new RestProfileRepository();

export const ProfileService = {
  getProfile: () => repository.getProfile(),
  updateProfile: (input: { username: string; avatarUrl: string }) => repository.updateProfile(input),
  downloadStatisticsPdf: () =>
    repository.downloadStatisticsPdf(() => useAuthStore.getState().accessToken),
};
