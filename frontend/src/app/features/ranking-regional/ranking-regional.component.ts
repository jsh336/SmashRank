import { Component, signal, computed } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import {
  RegionRankingService,
  TournamentSummary,
  RegionalRankingResult,
  RegionalRankingEntry,
  NotableWin
} from '../../core/services/region-ranking.service';

interface Province {
  key: string;       // Start.gg addrState value (English)
  label: string;     // Display name (Spanish)
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

  // ---- Provinces of Andalucía ----
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

  // ---- State ----
  selectedProvince: Province | null = null;
  dateRangeMonths = 12;           // Default: last 12 months (max 24)
  loadingTournaments = false;
  calculating = false;
  tournaments: TournamentSummary[] = [];
  selectedTournamentIds = new Set<string>();
  rankingResult: RegionalRankingResult | null = null;
  errorMessage: string | null = null;
  expandedPlayer: string | null = null;   // startGgUserId of expanded row

  constructor(private regionService: RegionRankingService) {}

  // ---- Computed ----
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

  // ---- Actions ----
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
        // Auto-select all by default
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

  getNotableNonUpsets(wins: NotableWin[]): NotableWin[] {
    return wins.filter(w => !w.isUpset);
  }

  formatDate(dateStr: string): string {
    if (!dateStr) return '';
    return new Date(dateStr).toLocaleDateString('es-ES', { day: 'numeric', month: 'short', year: 'numeric' });
  }
}
