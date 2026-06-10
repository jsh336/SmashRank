import { Component } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { Router } from '@angular/router';
import {
  RegionRankingService,
  TournamentSummary,
} from '../../core/services/region-ranking.service';
import { RankingStateService } from '../../core/services/ranking-state.service';

interface Province {
  key: string;
  label: string;
  emoji: string;
}

@Component({
  selector: 'app-calcular-ranking',
  standalone: true,
  imports: [CommonModule, FormsModule],
  templateUrl: './calcular-ranking.component.html',
  styleUrls: ['./calcular-ranking.component.scss']
})
export class CalcularRankingComponent {

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
  tournaments: TournamentSummary[] = [];
  selectedTournamentIds = new Set<string>();
  errorMessage: string | null = null;

  constructor(
    private regionService: RegionRankingService,
    private rankingState: RankingStateService,
    private router: Router
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

  selectAll(): void { this.tournaments.forEach(t => this.selectedTournamentIds.add(t.id)); }
  selectNone(): void { this.selectedTournamentIds.clear(); }

  selectProvince(province: Province): void {
    this.selectedProvince = province;
    this.tournaments = [];
    this.selectedTournamentIds.clear();
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
      error: () => {
        this.errorMessage = 'Error al conectar con Start.gg. Comprueba que el API token está configurado.';
        this.loadingTournaments = false;
      }
    });
  }

  onDateRangeChange(): void {
    if (this.selectedProvince) {
      this.fetchTournaments();
    }
  }

  calculateRanking(): void {
    if (!this.selectedProvince || this.selectedTournamentIds.size === 0) return;
    this.calculating = true;
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
        this.rankingState.setResult(result, this.selectedProvince!.label, this.dateRangeMonths);
        this.calculating = false;
        this.router.navigate(['/mostrar-ranking']);
      },
      error: () => {
        this.errorMessage = 'Error al calcular el ranking. Por favor intenta de nuevo.';
        this.calculating = false;
      }
    });
  }

  formatDate(dateStr: string): string {
    if (!dateStr) return '';
    return new Date(dateStr).toLocaleDateString('es-ES', { day: 'numeric', month: 'short', year: 'numeric' });
  }
}
