import { Component } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import {
  RegionRankingService,
  TournamentSummary,
  RegionalRankingResult,
  NotableWin
} from '../../core/services/region-ranking.service';
import { AuthService } from '../../core/services/auth.service';
import { SavedRankingService } from '../../core/services/saved-ranking.service';
import { SavedRanking } from '../../core/models/saved-ranking.model';

interface Province {
  key: string;
  label: string;
  emoji: string;
}

@Component({
  selector: 'app-ranking-regional',
  standalone: true,
  imports: [CommonModule, FormsModule],
  templateUrl: './ranking-regional.component.html',
  styleUrls: ['./ranking-regional.component.scss']
})
export class RankingRegionalComponent {

  provinces: Province[] = [
    { key: 'Seville',  label: 'Sevilla',  emoji: '🌹' },
    { key: 'Malaga',   label: 'Málaga',   emoji: '☀️' },
    { key: 'Granada',  label: 'Granada',  emoji: '🏰' },
    { key: 'Cadiz',    label: 'Cádiz',    emoji: '🏖️' },
    { key: 'Cordoba',  label: 'Córdoba',  emoji: '🕌' },
    { key: 'Almeria',  label: 'Almería',  emoji: '⛵' },
    { key: 'Huelva',   label: 'Huelva',   emoji: '🌿' },
    { key: 'Jaen',     label: 'Jaén',     emoji: '🫒' }
  ];

  selectedProvince: Province | null = null;
  dateRangeMonths = 12;
  loadingTournaments = false;
  calculating = false;
  saving = false;
  saveSuccess = false;
  tournaments: TournamentSummary[] = [];
  selectedTournamentIds = new Set<string>();
  rankingResult: RegionalRankingResult | null = null;
  errorMessage: string | null = null;
  expandedPlayer: string | null = null;

  constructor(
    private regionService: RegionRankingService,
    public authService: AuthService,
    private savedRankingService: SavedRankingService
  ) {}

  get afterDateEpoch(): number {
    const d = new Date();
    d.setMonth(d.getMonth() - this.dateRangeMonths);
    return Math.floor(d.getTime() / 1000);
  }

  get dateFromLabel(): string {
    const d = new Date();
    d.setMonth(d.getMonth() - this.dateRangeMonths);
    return d.toLocaleDateString('es-ES', { year: 'numeric', month: 'long' });
  }

  get selectedCount(): number {
    return this.selectedTournamentIds.size;
  }

  isSelected(id: string): boolean {
    return this.selectedTournamentIds.has(id);
  }

  toggleTournament(id: string): void {
    if (this.selectedTournamentIds.has(id)) {
      this.selectedTournamentIds.delete(id);
    } else {
      this.selectedTournamentIds.add(id);
    }
  }

  selectAll(): void {
    this.tournaments.forEach(t => this.selectedTournamentIds.add(t.id));
  }

  selectNone(): void {
    this.selectedTournamentIds.clear();
  }

  togglePlayer(userId: string): void {
    this.expandedPlayer = this.expandedPlayer === userId ? null : userId;
  }

  selectProvince(province: Province): void {
    this.selectedProvince = province;
    this.tournaments = [];
    this.selectedTournamentIds.clear();
    this.rankingResult = null;
    this.errorMessage = null;
    this.fetchTournaments();
  }

  fetchTournaments(): void {
    if (!this.selectedProvince) return;
    this.loadingTournaments = true;
    this.errorMessage = null;

    this.regionService.getTournamentsByRegion(
      this.selectedProvince.key,
      this.afterDateEpoch
    ).subscribe({
      next: (data) => {
        this.tournaments = data;
        data.forEach(t => this.selectedTournamentIds.add(t.id));
        this.loadingTournaments = false;
        if (data.length === 0) {
          this.errorMessage = `No se encontraron torneos de SSBU en ${this.selectedProvince!.label} en el período seleccionado.`;
        }
      },
      error: (err) => {
        console.error('Error fetching tournaments:', err);
        this.errorMessage = 'Error al conectar con Start.gg. Comprueba que el API token está configurado.';
        this.loadingTournaments = false;
      }
    });
  }

  onDateRangeChange(): void {
    if (this.selectedProvince) {
      this.rankingResult = null;
      this.fetchTournaments();
    }
  }

  calculateRanking(): void {
    if (!this.selectedProvince || this.selectedTournamentIds.size === 0) return;
    this.calculating = true;
    this.rankingResult = null;
    this.errorMessage = null;

    const now = new Date();
    const from = new Date();
    from.setMonth(from.getMonth() - this.dateRangeMonths);

    this.regionService.calculateRegionalRanking(
      this.selectedProvince.label,
      Array.from(this.selectedTournamentIds),
      from.toISOString().split('T')[0],
      now.toISOString().split('T')[0]
    ).subscribe({
      next: (result) => {
        this.rankingResult = result;
        this.calculating = false;
      },
      error: (err) => {
        console.error('Error calculating ranking:', err);
        this.errorMessage = 'Error al calcular el ranking. Por favor intenta de nuevo.';
        this.calculating = false;
      }
    });
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
    return wins.filter(w => w.isUpset);
  }

  formatDate(dateStr: string): string {
    if (!dateStr) return '';
    return new Date(dateStr).toLocaleDateString('es-ES', { day: 'numeric', month: 'short', year: 'numeric' });
  }

  exportToCsv(): void {
    if (!this.rankingResult) return;
    
    const headers = ['Posicion', 'Jugador', 'Start.gg User ID', 'Puntos Totales', 'Puntos Posibles', 'Eficiencia (%)', 'Torneos Jugados', 'Mejor Posicion', 'Victorias Notables', 'Upsets'];
    
    const rows = this.rankingResult.ranking.map(player => {
      const upsets = this.getUpsets(player.notableWins).length;
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
        upsets
      ];
    });

    const csvContent = [
      headers.join(','),
      ...rows.map(row => row.map(val => {
        const strVal = String(val);
        // Escape quotes and convert to string
        return `"${strVal.replace(/"/g, '""')}"`;
      }).join(','))
    ].join('\n');

    // Add BOM for Excel UTF-8 support
    const blob = new Blob([new Uint8Array([0xEF, 0xBB, 0xBF]), csvContent], { type: 'text/csv;charset=utf-8;' });
    const link = document.createElement('a');
    const url = URL.createObjectURL(blob);
    
    const filename = `ranking_${this.rankingResult.region.toLowerCase()}_${new Date().toISOString().split('T')[0]}.csv`;
    link.setAttribute('href', url);
    link.setAttribute('download', filename);
    link.style.visibility = 'hidden';
    document.body.appendChild(link);
    link.click();
    document.body.removeChild(link);
  }

  saveRanking(): void {
    if (!this.rankingResult) return;
    
    const name = window.prompt('Introduce un nombre para guardar este ranking (ej: Sevilla Invierno 2026):');
    if (!name || name.trim() === '') return;

    this.saving = true;
    this.saveSuccess = false;

    const saved: SavedRanking = {
      region: this.rankingResult.region,
      name: name,
      dateFrom: this.rankingResult.dateFrom,
      dateTo: this.rankingResult.dateTo,
      totalTournaments: this.rankingResult.totalTournaments,
      totalPlayers: this.rankingResult.totalPlayers,
      rankingData: JSON.stringify(this.rankingResult.ranking),
      createdBy: this.authService.currentUser()?.gamerTag || 'Admin'
    };

    this.savedRankingService.create(saved).subscribe({
      next: (res) => {
        this.saving = false;
        this.saveSuccess = true;
        alert('¡Ranking guardado con éxito!');
      },
      error: (err) => {
        console.error('Error saving ranking:', err);
        alert('Error al guardar el ranking. Asegúrate de estar autenticado.');
        this.saving = false;
      }
    });
  }
}
