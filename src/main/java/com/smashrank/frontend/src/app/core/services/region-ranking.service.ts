import { Injectable } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../../environments/environment';

export interface TournamentSummary {
  id: string;
  name: string;
  startAt: string;
  numAttendees: number;
  ssbUltimateEntrants: number;
  city: string;
}

export interface NotableWin {
  opponentName: string;
  startGgUserId: string;
  opponentPlacement: number;
  winnerPlacement: number;
  tournamentName: string;
  eventName: string;
  isUpset: boolean;
  tournamentEntrants: number;
}

export interface RegionalRankingEntry {
  position: number;
  playerName: string;
  startGgUserId: string;
  avatarUrl: string;
  totalPoints: number;
  totalPossiblePoints: number;
  efficiencyScore: number;
  tournamentsPlayed: number;
  bestPlacement: number;
  tournamentsAttended: string[];
  notableWins: NotableWin[];
}

export interface RegionalRankingResult {
  region: string;
  calculatedAt: string;
  dateFrom: string;
  dateTo: string;
  totalTournaments: number;
  totalPlayers: number;
  ranking: RegionalRankingEntry[];
}

@Injectable({ providedIn: 'root' })
export class RegionRankingService {
  private readonly apiUrl = `${environment.apiUrl}/startgg`;

  constructor(private http: HttpClient) {}

  getTournamentsByRegion(region: string, afterDate: number, page = 1, perPage = 25): Observable<TournamentSummary[]> {
    const params = new HttpParams()
      .set('region', region)
      .set('afterDate', afterDate.toString())
      .set('page', page.toString())
      .set('perPage', perPage.toString());
    return this.http.get<TournamentSummary[]>(`${this.apiUrl}/tournaments/region`, { params });
  }

  calculateRegionalRanking(
    region: string,
    tournamentIds: string[],
    dateFrom: string,
    dateTo: string
  ): Observable<RegionalRankingResult> {
    return this.http.post<RegionalRankingResult>(`${this.apiUrl}/ranking/regional`, {
      region,
      tournamentIds,
      dateFrom,
      dateTo
    });
  }
}
