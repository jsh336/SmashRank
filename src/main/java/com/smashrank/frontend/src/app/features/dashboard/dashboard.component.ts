import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { RankingService } from '../../core/services/ranking.service';
import { AuthService } from '../../core/services/auth.service';
import { PlayerRanking } from '../../models/player-ranking.model';

@Component({
  selector: 'app-dashboard',
  standalone: true,
  imports: [CommonModule, FormsModule],
  templateUrl: './dashboard.component.html',
  styleUrls: ['./dashboard.component.scss']
})
export class DashboardComponent implements OnInit {
  // Listas de datos
  rankingList: PlayerRanking[] = [];
  filteredRankingList: PlayerRanking[] = [];
  availableCountries: string[] = [];

  // Estado de filtros
  searchQuery: string = '';
  selectedCountry: string = 'ALL';

  // Estados de carga
  loading: boolean = true;
  recalculating: boolean = false;

  // Jugador Top 1 (Podio)
  topPlayer: PlayerRanking | null = null;

  constructor(
    private rankingService: RankingService,
    public authService: AuthService
  ) {}

  ngOnInit() {
    this.loadData();
  }

  // Carga de datos desde la API
  loadData() {
    this.loading = true;
    this.rankingService.getRanking().subscribe({
      next: (data) => {
        this.rankingList = data.sort((a, b) => a.position - b.position);
        this.topPlayer = this.rankingList.find(p => p.position === 1) || this.rankingList[0] || null;
        
        const countriesSet = new Set(this.rankingList.map(p => p.country));
        this.availableCountries = Array.from(countriesSet).filter(c => !!c).sort();
        
        this.filterPlayers();
        this.loading = false;
      },
      error: (err) => {
        console.error('Error al cargar ranking:', err);
        this.loading = false;
      }
    });
  }

  // Filtrado reactivo local
  filterPlayers() {
    this.filteredRankingList = this.rankingList.filter(player => {
      const matchesSearch = player.gamertag.toLowerCase().includes(this.searchQuery.toLowerCase()) ||
                            player.country.toLowerCase().includes(this.searchQuery.toLowerCase());
      
      const matchesCountry = this.selectedCountry === 'ALL' || player.country === this.selectedCountry;
      
      return matchesSearch && matchesCountry;
    });
  }

  onSearchChange() {
    this.filterPlayers();
  }

  onCountryChange(country: string) {
    this.selectedCountry = country;
    this.filterPlayers();
  }

  // Acción de recálculo (llamando al POST del backend)
  triggerRecalculate() {
    this.recalculating = true;
    this.rankingService.recalculateRanking().subscribe({
      next: (data) => {
        this.rankingList = data.sort((a, b) => a.position - b.position);
        this.topPlayer = this.rankingList.find(p => p.position === 1) || this.rankingList[0] || null;
        this.filterPlayers();
        this.recalculating = false;
      },
      error: (err) => {
        console.error('Error al recalcular:', err);
        this.recalculating = false;
      }
    });
  }
}
