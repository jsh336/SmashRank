import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { AuthService } from '../../core/services/auth.service';
import { SavedRankingService } from '../../core/services/saved-ranking.service';
import { SavedRanking } from '../../core/models/saved-ranking.model';
import { RegionalRankingEntry } from '../../core/services/region-ranking.service';

@Component({
  selector: 'app-admin',
  standalone: true,
  imports: [CommonModule, FormsModule],
  templateUrl: './admin.component.html',
  styleUrls: ['./admin.component.scss']
})
export class AdminComponent implements OnInit {
  savedRankings: SavedRanking[] = [];
  selectedRanking: SavedRanking | null = null;
  rankingEntries: RegionalRankingEntry[] = [];
  loadingRankings = false;
  savingRanking = false;
  expandedRankingPlayer: string | null = null;

  constructor(
    public authService: AuthService,
    private savedRankingService: SavedRankingService
  ) {}

  ngOnInit(): void {
    this.loadSavedRankings();
  }
  loadSavedRankings(): void {
    this.loadingRankings = true;
    this.savedRankingService.getAll().subscribe({
      next: (data) => {
        this.savedRankings = data;
        this.loadingRankings = false;
      },
      error: (err) => {
        console.error('Error loading saved rankings in admin:', err);
        this.loadingRankings = false;
      }
    });
  }

  selectAdminRanking(ranking: SavedRanking): void {
    this.selectedRanking = ranking;
    try {
      this.rankingEntries = JSON.parse(ranking.rankingData) as RegionalRankingEntry[];
    } catch (e) {
      console.error('Error parsing ranking data in admin:', e);
      this.rankingEntries = [];
    }
    this.expandedRankingPlayer = null;
  }

  backToRankingsList(): void {
    this.selectedRanking = null;
    this.rankingEntries = [];
    this.expandedRankingPlayer = null;
    this.loadSavedRankings();
  }

  toggleHidePlayer(index: number): void {
    const player = this.rankingEntries[index];
    player.hidden = !player.hidden;
  }

  removePlayerFromRanking(index: number): void {
    if (confirm(`¿Seguro que deseas eliminar a "${this.rankingEntries[index].playerName}" de este ranking?`)) {
      this.rankingEntries.splice(index, 1);
      this.recalculateRankingPositions();
    }
  }

  moveRankingPlayerUp(index: number): void {
    if (index === 0) return;
    const temp = this.rankingEntries[index];
    this.rankingEntries[index] = this.rankingEntries[index - 1];
    this.rankingEntries[index - 1] = temp;
    this.recalculateRankingPositions();
  }

  moveRankingPlayerDown(index: number): void {
    if (index === this.rankingEntries.length - 1) return;
    const temp = this.rankingEntries[index];
    this.rankingEntries[index] = this.rankingEntries[index + 1];
    this.rankingEntries[index + 1] = temp;
    this.recalculateRankingPositions();
  }

  recalculateRankingPositions(): void {
    for (let i = 0; i < this.rankingEntries.length; i++) {
      this.rankingEntries[i].position = i + 1;
    }
  }

  saveRankingChanges(): void {
    if (!this.selectedRanking || !this.selectedRanking.id) return;
    this.savingRanking = true;

    const updated: SavedRanking = {
      ...this.selectedRanking,
      totalPlayers: this.rankingEntries.length,
      rankingData: JSON.stringify(this.rankingEntries)
    };

    this.savedRankingService.update(this.selectedRanking.id, updated).subscribe({
      next: (res) => {
        this.savingRanking = false;
        this.selectedRanking = res;
        alert('¡Ranking guardado con éxito!');
        this.backToRankingsList();
      },
      error: (err) => {
        console.error('Error updating saved ranking in admin:', err);
        alert('Error al guardar los cambios en el ranking.');
        this.savingRanking = false;
      }
    });
  }

  deleteSavedRanking(id: number): void {
    if (confirm('¿Seguro que deseas eliminar este ranking permanentemente del sistema?')) {
      this.savedRankingService.delete(id).subscribe({
        next: () => {
          alert('Ranking eliminado con éxito.');
          this.backToRankingsList();
        },
        error: (err) => {
          console.error('Error deleting saved ranking in admin:', err);
          alert('Error al eliminar el ranking.');
        }
      });
    }
  }

  toggleRankingPlayerDetails(userId: string): void {
    this.expandedRankingPlayer = this.expandedRankingPlayer === userId ? null : userId;
  }
}
