import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { Router } from '@angular/router';
import {
  RegionalRankingResult,
  RegionalRankingEntry,
  NotableWin
} from '../../core/services/region-ranking.service';
import { RankingStateService } from '../../core/services/ranking-state.service';
import { AuthService } from '../../core/services/auth.service';
import { SavedRankingService } from '../../core/services/saved-ranking.service';
import { SavedRanking } from '../../core/models/saved-ranking.model';

@Component({
  selector: 'app-mostrar-ranking',
  standalone: true,
  imports: [CommonModule, FormsModule],
  templateUrl: './mostrar-ranking.component.html',
  styleUrls: ['./mostrar-ranking.component.scss']
})
export class MostrarRankingComponent implements OnInit {

  rankingResult: RegionalRankingResult | null = null;
  expandedPlayer: string | null = null;
  saving = false;
  saveSuccess = false;

  constructor(
    private rankingState: RankingStateService,
    public authService: AuthService,
    private savedRankingService: SavedRankingService,
    private router: Router
  ) {}

  ngOnInit(): void {
    this.rankingResult = this.rankingState.result();
    if (!this.rankingResult) {
      // No hay resultado, redirigir al calculador
      this.router.navigate(['/calcular-ranking']);
    }
  }

  volverACalcular(): void {
    this.router.navigate(['/calcular-ranking']);
  }

  togglePlayer(userId: string): void {
    this.expandedPlayer = this.expandedPlayer === userId ? null : userId;
  }

  getPositionClass(pos: number): string {
    if (pos === 1) return 'pos-gold';
    if (pos === 2) return 'pos-silver';
    if (pos === 3) return 'pos-bronze';
    return 'pos-normal';
  }

  getPositionEmoji(pos: number): string {
    if (pos === 1) return '🥇';
    if (pos === 2) return '🥈';
    if (pos === 3) return '🥉';
    return `#${pos}`;
  }

  getUpsets(wins: NotableWin[]): NotableWin[] {
    return wins ? wins.filter(w => w.isUpset) : [];
  }

  formatDate(dateStr: string): string {
    if (!dateStr) return '';
    return new Date(dateStr).toLocaleDateString('es-ES', { day: 'numeric', month: 'short', year: 'numeric' });
  }

  get dateFromLabel(): string {
    const d = new Date();
    d.setMonth(d.getMonth() - this.rankingState.dateRangeMonths());
    return d.toLocaleDateString('es-ES', { year: 'numeric', month: 'long' });
  }

  saveRanking(): void {
    if (!this.rankingResult) return;

    const name = window.prompt('Introduce un nombre para guardar este ranking (ej: Sevilla Invierno 2026):');
    if (!name || name.trim() === '') return;

    this.saving = true;
    this.saveSuccess = false;

    const saved: SavedRanking = {
      region: this.rankingResult.region,
      name: name.trim(),
      dateFrom: this.rankingResult.dateFrom,
      dateTo: this.rankingResult.dateTo,
      totalTournaments: this.rankingResult.totalTournaments,
      totalPlayers: this.rankingResult.totalPlayers,
      rankingData: JSON.stringify(this.rankingResult.ranking),
      createdBy: this.authService.currentUser()?.gamerTag || 'Admin'
    };

    this.savedRankingService.create(saved).subscribe({
      next: () => {
        this.saving = false;
        this.saveSuccess = true;
        alert('¡Ranking guardado con éxito! Puedes verlo en "Historial Regional".');
      },
      error: () => {
        alert('Error al guardar el ranking. Asegúrate de estar autenticado.');
        this.saving = false;
      }
    });
  }

  exportToCsv(): void {
    if (!this.rankingResult) return;

    // Separador punto y coma (estándar europeo para Excel)
    const SEP = ';';

    const headers = [
      'Posicion', 'Jugador', 'Start.gg ID',
      'Puntos Totales', 'Puntos Posibles', 'Eficiencia (%)',
      'Torneos Jugados', 'Mejor Posicion',
      'Victorias Notables', 'Upsets',
      'Torneos Asistidos', 'Detalle Victorias'
    ];

    const rows = this.rankingResult.ranking.map(player => {
      const upsets = this.getUpsets(player.notableWins);
      const winsDetail = player.notableWins
        .map(w => `${w.isUpset ? 'UPSET' : 'NOTABLE'}: ${w.opponentName} @ ${w.tournamentName}`)
        .join(' | ');
      const tournamentsStr = (player.tournamentsAttended || []).join(' | ');

      return [
        player.position,
        player.playerName,
        player.startGgUserId,
        player.totalPoints,
        player.totalPossiblePoints,
        player.efficiencyScore.toFixed(2),
        player.tournamentsPlayed,
        player.bestPlacement,
        player.notableWins.length,
        upsets.length,
        tournamentsStr,
        winsDetail
      ];
    });

    const escape = (val: unknown) => {
      const str = String(val ?? '');
      return `"${str.replace(/"/g, '""')}"`;
    };

    const csvContent = [
      headers.map(escape).join(SEP),
      ...rows.map(row => row.map(escape).join(SEP))
    ].join('\r\n');

    // BOM UTF-8 para compatibilidad Excel
    const blob = new Blob(
      [new Uint8Array([0xEF, 0xBB, 0xBF]), csvContent],
      { type: 'text/csv;charset=utf-8;' }
    );

    const link = document.createElement('a');
    link.href = URL.createObjectURL(blob);
    link.download = `ranking_${this.rankingResult.region.toLowerCase().replace(/\s+/g, '_')}_${new Date().toISOString().split('T')[0]}.csv`;
    link.style.display = 'none';
    document.body.appendChild(link);
    link.click();
    document.body.removeChild(link);
    URL.revokeObjectURL(link.href);
  }
}
