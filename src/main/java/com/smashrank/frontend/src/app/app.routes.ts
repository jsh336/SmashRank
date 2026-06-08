import { Routes } from '@angular/router';
import { HomeComponent } from './features/home/home.component';
import { DashboardComponent } from './features/dashboard/dashboard.component';
import { AuthCallbackComponent } from './features/auth-callback/auth-callback';

export const routes: Routes = [
  { path: '', component: HomeComponent },
  { path: 'ranking', component: DashboardComponent },
  { path: 'auth/callback', component: AuthCallbackComponent },
  { path: '**', redirectTo: '' }
];
