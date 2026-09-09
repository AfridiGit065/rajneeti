import { RestProfileRepository } from "@/repositories/profile-repository";

const repository = new RestProfileRepository();

export const ProfileService = {
  getProfile: () => repository.getProfile(),
  updateProfile: (input: { username: string; avatarUrl: string }) => repository.updateProfile(input),
};
