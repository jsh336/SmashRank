import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { HttpClient } from '@angular/common/http';
import { AuthService } from '../../core/services/auth.service';
import { environment } from '../../../environments/environment';

interface Player {
  id: number;
  gamertag: string;
  country: string;
  startGgUserId: string | null;
  startGgSlug: string | null;
  rankPoints: number;
}

type AdminTab = 'players' | 'adjust' | 'recalculate';

@Component({
  selector: 'app-admin',
  standalone: true,
  imports: [CommonModule, FormsModule],
  templateUrl: './admin.component.html',
  styleUrls: ['./admin.component.scss']
})
export class AdminComponent implements OnInit {
  activeTab: AdminTab = 'players';

  // ---- Players tab ----
  players: Player[] = [];
  filteredPlayers: Player[] = [];
  searchQuery = '';
  loadingPlayers = false;
  editingPlayer: Player | null = null;
  editForm: Partial<Player> = {};
  showCreateForm = false;
  createForm: Partial<Player> = { gamertag: '', country: 'Spain', startGgSlug: '' };
  syncingId: number | null = null;
  deletingId: number | null = null;

  // ---- Adjust points tab ----
  adjustSearch = '';
  adjustPlayer: Player | null = null;
  adjustDelta = 0;
  adjustReason = '';
  adjustHistory: { playerName: string; delta: number; reason: string; at: Date }[] = [];
  adjusting = false;

  // ---- Recalculate tab ----
  recalculating = false;
  recalcResult: any = null;

  private apiUrl = environment.apiUrl;

  constructor(
    private http: HttpClient,
    public authService: AuthService
  ) {}

  ngOnInit(): void {
    this.loadPlayers();
  }

  setTab(tab: AdminTab): void {
    this.activeTab = tab;
    if (tab === 'players' && this.players.length === 0) {
      this.loadPlayers();
    }
  }

  // ---- Players tab methods ----
  loadPlayers(): void {
    this.loadingPlayers = true;
    this.http.get<Player[]>(`${this.apiUrl}/players`).subscribe({
      next: (data) => {
        this.players = data;
        this.filterPlayers();
        this.loadingPlayers = false;
      },
      error: (err) => {
        console.error('Error loading players:', err);
        this.loadingPlayers = false;
      }
    });
  }

  filterPlayers(): void {
    const q = this.searchQuery.toLowerCase();
    this.filteredPlayers = q
      ? this.players.filter(p => p.gamertag.toLowerCase().includes(q))
      : [...this.players];
  }

  startEdit(player: Player): void {
    this.editingPlayer = player;
    this.editForm = { ...player };
    this.showCreateForm = false;
  }

  cancelEdit(): void {
    this.editingPlayer = null;
    this.editForm = {};
  }

  saveEdit(): void {
    if (!this.editingPlayer) return;
    this.http.put<Player>(`${this.apiUrl}/players/${this.editingPlayer.id}`, this.editForm).subscribe({
      next: (updated) => {
        const idx = this.players.findIndex(p => p.id === updated.id);
        if (idx >= 0) this.players[idx] = updated;
        this.filterPlayers();
        this.cancelEdit();
      },
      error: (err) => console.error('Error updating player:', err)
    });
  }

  deletePlayer(player: Player): void {
    if (!confirm(`¿Eliminar al jugador "${player.gamertag}"? Esta acción no se puede deshacer.`)) return;
    this.deletingId = player.id;
    this.http.delete(`${this.apiUrl}/players/${player.id}`).subscribe({
      next: () => {
        this.players = this.players.filter(p => p.id !== player.id);
        this.filterPlayers();
        this.deletingId = null;
      },
      error: (err) => {
        console.error('Error deleting player:', err);
        this.deletingId = null;
      }
    });
  }

  syncPlayer(player: Player): void {
    if (!player.startGgSlug) {
      alert('Este jugador no tiene un Slug de Start.gg configurado.');
      return;
    }
    this.syncingId = player.id;
    this.http.post(`${this.apiUrl}/players/${player.id}/sync-startgg`, { slug: player.startGgSlug }).subscribe({
      next: () => {
        this.syncingId = null;
        this.loadPlayers();
      },
      error: (err) => {
        console.error('Error syncing player:', err);
        this.syncingId = null;
      }
    });
  }

  toggleCreateForm(): void {
    this.showCreateForm = !this.showCreateForm;
    this.editingPlayer = null;
    this.createForm = { gamertag: '', country: 'Spain', startGgSlug: '' };
  }

  createPlayer(): void {
    if (!this.createForm.gamertag) return;
    this.http.post<Player>(`${this.apiUrl}/players`, this.createForm).subscribe({
      next: (newPlayer) => {
        this.players.unshift(newPlayer);
        this.filterPlayers();
        this.showCreateForm = false;
        this.createForm = { gamertag: '', country: 'Spain', startGgSlug: '' };
      },
      error: (err) => console.error('Error creating player:', err)
    });
  }

  // ---- Adjust points methods ----
  searchForAdjust(): void {
    const q = this.adjustSearch.toLowerCase();
    this.adjustPlayer = this.players.find(p => p.gamertag.toLowerCase().includes(q)) || null;
  }

  applyAdjustment(): void {
    if (!this.adjustPlayer || this.adjustDelta === 0) return;
    this.adjusting = true;

    const params = new URLSearchParams({ delta: this.adjustDelta.toString() });
    this.http.patch(`${this.apiUrl}/players/${this.adjustPlayer.id}/rank-points?${params.toString()}`, {}).subscribe({
      next: () => {
        this.adjustHistory.unshift({
          playerName: this.adjustPlayer!.gamertag,
          delta: this.adjustDelta,
          reason: this.adjustReason || '(sin motivo)',
          at: new Date()
        });
        this.adjustPlayer!.rankPoints += this.adjustDelta;
        this.adjustDelta = 0;
        this.adjustReason = '';
        this.adjusting = false;
        this.loadPlayers();
      },
      error: (err) => {
        console.error('Error adjusting points:', err);
        this.adjusting = false;
      }
    });
  }

  // ---- Recalculate methods ----
  recalculate(): void {
    this.recalculating = true;
    this.recalcResult = null;
    this.http.post<any>(`${this.apiUrl}/ranking/recalculate`, {}).subscribe({
      next: (result) => {
        this.recalcResult = result;
        this.recalculating = false;
      },
      error: (err) => {
        console.error('Error recalculating:', err);
        this.recalcResult = { error: true };
        this.recalculating = false;
      }
    });
  }
}
