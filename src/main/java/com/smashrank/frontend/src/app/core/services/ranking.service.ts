import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable, of } from 'rxjs';
import { catchError } from 'rxjs/operators';
import { PlayerRanking } from '../../models/player-ranking.model';
import { environment } from '../../../environments/environment';

@Injectable({
  providedIn: 'root'
})
export class RankingService {
  private apiUrl = `${environment.apiUrl}/ranking`;

  // Datos mock de fallback con exactamente 15 jugadores reales de la escena de Smash
  private readonly mockRanking: PlayerRanking[] = [
    {
      position: 1,
      playerId: 1,
      gamertag: 'MkLeo',
      country: 'MX',
      startGgSlug: 'user/mkleo',
      totalPoints: 950,
      totalPossiblePoints: 1000,
      efficiencyScore: 95.0,
      tournamentsPlayed: 8,
      bestPlacement: 1
    },
    {
      position: 2,
      playerId: 2,
      gamertag: 'Sparg0',
      country: 'MX',
      startGgSlug: 'user/sparg0',
      totalPoints: 910,
      totalPossiblePoints: 1000,
      efficiencyScore: 91.0,
      tournamentsPlayed: 7,
      bestPlacement: 1
    },
    {
      position: 3,
      playerId: 3,
      gamertag: 'Sonix',
      country: 'DO',
      startGgSlug: 'user/sonix',
      totalPoints: 890,
      totalPossiblePoints: 1000,
      efficiencyScore: 89.0,
      tournamentsPlayed: 9,
      bestPlacement: 1
    },
    {
      position: 4,
      playerId: 4,
      gamertag: 'Miya',
      country: 'JP',
      startGgSlug: 'user/miya',
      totalPoints: 850,
      totalPossiblePoints: 1000,
      efficiencyScore: 85.0,
      tournamentsPlayed: 12,
      bestPlacement: 1
    },
    {
      position: 5,
      playerId: 5,
      gamertag: 'acola',
      country: 'JP',
      startGgSlug: 'user/acola',
      totalPoints: 840,
      totalPossiblePoints: 1000,
      efficiencyScore: 84.0,
      tournamentsPlayed: 10,
      bestPlacement: 1
    },
    {
      position: 6,
      playerId: 6,
      gamertag: 'Light',
      country: 'US',
      startGgSlug: 'user/light',
      totalPoints: 780,
      totalPossiblePoints: 950,
      efficiencyScore: 82.1,
      tournamentsPlayed: 8,
      bestPlacement: 2
    },
    {
      position: 7,
      playerId: 7,
      gamertag: 'Tweek',
      country: 'US',
      startGgSlug: 'user/tweek',
      totalPoints: 750,
      totalPossiblePoints: 950,
      efficiencyScore: 78.9,
      tournamentsPlayed: 7,
      bestPlacement: 2
    },
    {
      position: 8,
      playerId: 8,
      gamertag: 'Glutonny',
      country: 'FR',
      startGgSlug: 'user/glutonny',
      totalPoints: 730,
      totalPossiblePoints: 950,
      efficiencyScore: 76.8,
      tournamentsPlayed: 10,
      bestPlacement: 2
    },
    {
      position: 9,
      playerId: 9,
      gamertag: 'Riddles',
      country: 'CA',
      startGgSlug: 'user/riddles',
      totalPoints: 680,
      totalPossiblePoints: 900,
      efficiencyScore: 75.5,
      tournamentsPlayed: 9,
      bestPlacement: 3
    },
    {
      position: 10,
      playerId: 10,
      gamertag: 'Kurama',
      country: 'US',
      startGgSlug: 'user/kurama',
      totalPoints: 660,
      totalPossiblePoints: 900,
      efficiencyScore: 73.3,
      tournamentsPlayed: 8,
      bestPlacement: 3
    },
    {
      position: 11,
      playerId: 11,
      gamertag: 'Zomba',
      country: 'US',
      startGgSlug: 'user/zomba',
      totalPoints: 630,
      totalPossiblePoints: 900,
      efficiencyScore: 70.0,
      tournamentsPlayed: 11,
      bestPlacement: 3
    },
    {
      position: 12,
      playerId: 12,
      gamertag: 'Sisqui',
      country: 'ES',
      startGgSlug: 'user/sisqui',
      totalPoints: 590,
      totalPossiblePoints: 850,
      efficiencyScore: 69.4,
      tournamentsPlayed: 10,
      bestPlacement: 4
    },
    {
      position: 13,
      playerId: 13,
      gamertag: 'Shuton',
      country: 'JP',
      startGgSlug: 'user/shuton',
      totalPoints: 580,
      totalPossiblePoints: 850,
      efficiencyScore: 68.2,
      tournamentsPlayed: 8,
      bestPlacement: 3
    },
    {
      position: 14,
      playerId: 14,
      gamertag: 'Kameme',
      country: 'JP',
      startGgSlug: 'user/kameme',
      totalPoints: 550,
      totalPossiblePoints: 850,
      efficiencyScore: 64.7,
      tournamentsPlayed: 9,
      bestPlacement: 4
    },
    {
      position: 15,
      playerId: 15,
      gamertag: 'Bloom4Evr',
      country: 'GB',
      startGgSlug: 'user/bloom4evr',
      totalPoints: 520,
      totalPossiblePoints: 800,
      efficiencyScore: 65.0,
      tournamentsPlayed: 6,
      bestPlacement: 4
    }
  ];

  constructor(private http: HttpClient) {}

  /**
   * Obtiene el ranking de jugadores. En caso de fallo de conexión con el backend de Spring Boot,
   * se hace fallback automáticamente a datos estáticos realistas de demo.
   */
  getRanking(): Observable<PlayerRanking[]> {
    return this.http.get<PlayerRanking[]>(this.apiUrl).pipe(
      catchError((error) => {
        console.warn('No se pudo conectar con el backend de Spring Boot en ' + this.apiUrl + '. Usando datos de demo.', error);
        return of(this.mockRanking);
      })
    );
  }

  /**
   * Fuerza el recálculo y persistencia de las posiciones y estadísticas de ranking en la BBDD.
   */
  recalculateRanking(): Observable<PlayerRanking[]> {
    return this.http.post<PlayerRanking[]>(`${this.apiUrl}/recalculate`, {}).pipe(
      catchError((error) => {
        console.error('Error al solicitar recálculo al backend de Spring Boot. Devolviendo ranking local actual.', error);
        return of(this.mockRanking);
      })
    );
  }
}
