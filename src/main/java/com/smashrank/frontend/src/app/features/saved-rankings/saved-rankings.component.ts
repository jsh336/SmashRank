import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { SavedRankingService } from '../../core/services/saved-ranking.service';
import { AuthService } from '../../core/services/auth.service';
import { SavedRanking } from '../../core/models/saved-ranking.model';
import { RegionalRankingEntry, NotableWin } from '../../core/services/region-ranking.service';

@Component({
  selector: 'app-saved-rankings',
  standalone: true,
  imports: [CommonModule, FormsModule],
  templateUrl: './saved-rankings.component.html',
  styleUrls: ['./saved-rankings.component.scss']
})
export class SavedRankingsComponent implements OnInit {

  savedRankings: SavedRanking[] = [];
  selectedRanking: SavedRanking | null = null;
  rankingEntries: RegionalRankingEntry[] = [];
  loading = false;
  editing = false;
  saving = false;
  expandedPlayer: string | null = null;

  constructor(
    private savedRankingService: SavedRankingService,
    public authService: AuthService
  ) {}

  ngOnInit(): void {
    this.loadAll();
  }

  loadAll(): void {
    this.loading = true;
    this.savedRankingService.getAll().subscribe({
      next: (data) => {
        this.savedRankings = data;
        this.loading = false;
      },
      error: (err) => {
        console.error('Error loading saved rankings:', err);
        this.loading = false;
      }
    });
  }

  selectRanking(ranking: SavedRanking): void {
    this.selectedRanking = ranking;
    try {
      const allEntries = JSON.parse(ranking.rankingData) as RegionalRankingEntry[];
      this.rankingEntries = allEntries.filter(e => !e.hidden);
      // Reasignar posiciones visuales secuenciales para la lista filtrada
      this.rankingEntries.forEach((e, idx) => {
        e.position = idx + 1;
      });
    } catch (e) {
      console.error('Error parsing ranking data:', e);
      this.rankingEntries = [];
    }
    this.editing = false;
    this.expandedPlayer = null;
  }

  backToList(): void {
    this.selectedRanking = null;
    this.rankingEntries = [];
    this.editing = false;
    this.expandedPlayer = null;
    this.loadAll();
  }

  toggleEdit(): void {
    if (!this.authService.isLoggedIn()) return;
    this.editing = !this.editing;
    this.expandedPlayer = null;
  }

  moveUp(index: number): void {
    if (index === 0) return;
    const temp = this.rankingEntries[index];
    this.rankingEntries[index] = this.rankingEntries[index - 1];
    this.rankingEntries[index - 1] = temp;
    this.recalculatePositions();
  }

  moveDown(index: number): void {
    if (index === this.rankingEntries.length - 1) return;
    const temp = this.rankingEntries[index];
    this.rankingEntries[index] = this.rankingEntries[index + 1];
    this.rankingEntries[index + 1] = temp;
    this.recalculatePositions();
  }

  deletePlayer(index: number): void {
    if (confirm('¿Seguro que deseas eliminar a este jugador de este ranking?')) {
      this.rankingEntries.splice(index, 1);
      this.recalculatePositions();
    }
  }

  recalculatePositions(): void {
    for (let i = 0; i < this.rankingEntries.length; i++) {
      this.rankingEntries[i].position = i + 1;
    }
  }

  saveChanges(): void {
    if (!this.selectedRanking || !this.selectedRanking.id) return;
    this.saving = true;

    const updated: SavedRanking = {
      ...this.selectedRanking,
      totalPlayers: this.rankingEntries.length,
      rankingData: JSON.stringify(this.rankingEntries)
    };

    this.savedRankingService.update(this.selectedRanking.id, updated).subscribe({
      next: (res) => {
        this.saving = false;
        this.editing = false;
        this.selectedRanking = res;
        alert('¡Cambios guardados con éxito!');
      },
      error: (err) => {
        console.error('Error updating saved ranking:', err);
        alert('Error al guardar los cambios en el ranking.');
        this.saving = false;
      }
    });
  }

  deleteRanking(id: number): void {
    if (confirm('¿Seguro que deseas eliminar este ranking permanentemente del sistema?')) {
      this.savedRankingService.delete(id).subscribe({
        next: () => {
          alert('Ranking eliminado con éxito.');
          this.backToList();
        },
        error: (err) => {
          console.error('Error deleting saved ranking:', err);
          alert('Error al eliminar el ranking.');
        }
      });
    }
  }

  deleteRankingDirect(id: number, event: Event): void {
    event.stopPropagation();
    this.deleteRanking(id);
  }

  togglePlayer(userId: string): void {
    if (this.editing) return;
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

  formatDate(dateStr: string | undefined): string {
    if (!dateStr) return '';
    return new Date(dateStr).toLocaleDateString('es-ES', { day: 'numeric', month: 'short', year: 'numeric' });
  }

  exportToCsv(): void {
    if (!this.selectedRanking) return;

    // Separador punto y coma — estándar europeo para Excel
    const SEP = ';';

    const headers = [
      'Posicion', 'Jugador', 'Start.gg ID',
      'Puntos Totales', 'Puntos Posibles', 'Eficiencia (%)',
      'Torneos Jugados', 'Mejor Posicion',
      'Victorias Notables', 'Upsets',
      'Torneos Asistidos', 'Detalle Victorias'
    ];

    const rows = this.rankingEntries.map(player => {
      const upsets = this.getUpsets(player.notableWins).length;
      const winsDetail = (player.notableWins || [])
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
        player.notableWins ? player.notableWins.length : 0,
        upsets,
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

    // BOM UTF-8 para compatibilidad con Excel
    const blob = new Blob(
      [new Uint8Array([0xEF, 0xBB, 0xBF]), csvContent],
      { type: 'text/csv;charset=utf-8;' }
    );

    const url = URL.createObjectURL(blob);
    const link = document.createElement('a');
    link.href = url;
    link.download = `ranking_${this.selectedRanking.region.toLowerCase().replace(/\s+/g, '_')}_${new Date().toISOString().split('T')[0]}.csv`;
    link.style.display = 'none';
    document.body.appendChild(link);
    link.click();
    document.body.removeChild(link);
    URL.revokeObjectURL(url);
  }
}
