export interface PlayerRanking {
  position: number;
  playerId: number;
  gamertag: string;
  country: string;
  startGgSlug: string;
  totalPoints: number;
  totalPossiblePoints: number;
  efficiencyScore: number;
  tournamentsPlayed: number;
  bestPlacement: number;
}
