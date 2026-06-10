import { Injectable, signal, computed } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { tap } from 'rxjs/operators';
import { environment } from '../../../environments/environment';

export interface UserProfile {
  id: number;
  startGgUserId: string;
  name: string;
  gamerTag: string;
  email: string;
  avatarUrl: string;
  role: string;
}

@Injectable({
  providedIn: 'root'
})
export class AuthService {
  private apiUrl = `${environment.apiUrl}/auth`;

  // Estado del usuario reactivo usando Signals de Angular 17
  private currentUserSignal = signal<UserProfile | null>(null);
  
  // Selectores de señales
  currentUser = computed(() => this.currentUserSignal());
  isLoggedIn = computed(() => this.currentUserSignal() !== null);
  isAdmin = computed(() => this.currentUserSignal()?.role === 'ADMIN');

  constructor(private http: HttpClient) {
    this.loadSessionFromStorage();
  }

  /**
   * Obtiene la URL de redirección a Start.gg desde el backend.
   */
  getLoginUrl(): Observable<{ url: string }> {
    return this.http.get<{ url: string }>(`${this.apiUrl}/login-url`);
  }

  /**
   * Envía el código de autorización al backend para iniciar sesión.
   */
  login(code: string): Observable<{ token: string; user: UserProfile }> {
    return this.http.post<{ token: string; user: UserProfile }>(`${this.apiUrl}/callback`, { code }).pipe(
      tap(response => {
        this.saveSession(response.token, response.user);
      })
    );
  }

  /**
   * Cierra la sesión activa borrando los tokens de SessionStorage.
   */
  logout() {
    sessionStorage.removeItem('smashrank_token');
    sessionStorage.removeItem('smashrank_user');
    this.currentUserSignal.set(null);
  }

  /**
   * Retorna el token JWT actual.
   */
  getToken(): string | null {
    return sessionStorage.getItem('smashrank_token');
  }

  private saveSession(token: string, user: UserProfile) {
    sessionStorage.setItem('smashrank_token', token);
    sessionStorage.setItem('smashrank_user', JSON.stringify(user));
    this.currentUserSignal.set(user);
  }

  private loadSessionFromStorage() {
    const token = this.getToken();
    const userStr = sessionStorage.getItem('smashrank_user');
    
    if (token && userStr) {
      try {
        const user = JSON.parse(userStr) as UserProfile;
        this.currentUserSignal.set(user);
      } catch (e) {
        this.logout();
      }
    }
  }
}
