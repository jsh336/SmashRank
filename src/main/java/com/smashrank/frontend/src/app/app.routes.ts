import { Routes } from '@angular/router';
import { HomeComponent } from './features/home/home.component';
import { AuthCallbackComponent } from './features/auth-callback/auth-callback';
import { RankingRegionalComponent } from './features/ranking-regional/ranking-regional.component';
import { SavedRankingsComponent } from './features/saved-rankings/saved-rankings.component';
import { AdminComponent } from './features/admin/admin.component';

export const routes: Routes = [
  { path: '', component: HomeComponent },
  { path: 'ranking-regional', component: RankingRegionalComponent },
  { path: 'rankings-guardados', component: SavedRankingsComponent },
  { path: 'admin', component: AdminComponent },
  { path: 'auth/callback', component: AuthCallbackComponent },
  { path: '**', redirectTo: '' }
];
