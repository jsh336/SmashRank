import { RegionalRankingEntry } from '../services/region-ranking.service';

export interface SavedRanking {
  id?: number;
  region: string;
  name: string;
  calculatedAt?: string;
  dateFrom: string;
  dateTo: string;
  totalTournaments: number;
  totalPlayers: number;
  rankingData: string; // JSON string of RegionalRankingEntry[]
  createdBy?: string;
}
