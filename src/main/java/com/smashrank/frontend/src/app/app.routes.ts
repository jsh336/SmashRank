import { Routes } from '@angular/router';
import { HomeComponent } from './features/home/home.component';
import { AuthCallbackComponent } from './features/auth-callback/auth-callback';
import { CalcularRankingComponent } from './features/calcular-ranking/calcular-ranking.component';
import { MostrarRankingComponent } from './features/mostrar-ranking/mostrar-ranking.component';
import { SavedRankingsComponent } from './features/saved-rankings/saved-rankings.component';
import { AdminComponent } from './features/admin/admin.component';

export const routes: Routes = [
  { path: '', component: HomeComponent },
  { path: 'calcular-ranking', component: CalcularRankingComponent },
  { path: 'mostrar-ranking', component: MostrarRankingComponent },
  { path: 'rankings-guardados', component: SavedRankingsComponent },
  { path: 'admin', component: AdminComponent },
  { path: 'auth/callback', component: AuthCallbackComponent },
  // Redirección de rutas antiguas para no romper marcadores
  { path: 'ranking-regional', redirectTo: 'calcular-ranking', pathMatch: 'full' },
  { path: '**', redirectTo: '' }
];
