import { Injectable, signal, computed } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable, of } from 'rxjs';
import { tap, catchError } from 'rxjs/operators';
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
   * Cuenta con fallback local para poder probarlo offline.
   */
  getLoginUrl(): Observable<{ url: string }> {
    return this.http.get<{ url: string }>(`${this.apiUrl}/login-url`).pipe(
      catchError((error) => {
        console.warn('API de autenticación no disponible. Generando URL de autorización mock.', error);
        // Fallback local: Redirige directamente a nuestra página de callback con un código ficticio
        const mockUrl = `${window.location.origin}/auth/callback?code=mock_auth_code_for_offline_testing`;
        return of({ url: mockUrl });
      })
    );
  }

  /**
   * Envía el código de autorización al backend para iniciar sesión.
   * En caso de error (API offline), simula un login de administrador mock.
   */
  login(code: string): Observable<{ token: string; user: UserProfile }> {
    return this.http.post<{ token: string; user: UserProfile }>(`${this.apiUrl}/callback`, { code }).pipe(
      tap(response => {
        this.saveSession(response.token, response.user);
      }),
      catchError((error) => {
        console.warn('Fallo en la comunicación con el servidor. Iniciando sesión de desarrollo mock.', error);
        
        // Simular inicio de sesión mock de Administrador para pruebas sin servidor
        const mockResponse = {
          token: 'mock_jwt_token_for_local_testing_purposes_only',
          user: {
            id: 1,
            startGgUserId: '123456',
            name: 'Jose Developer',
            gamerTag: 'JoseDev',
            email: 'jose@smashrank.gg',
            avatarUrl: '', // Quedará el icono por defecto
            role: 'ADMIN'
          }
        };
        
        this.saveSession(mockResponse.token, mockResponse.user);
        return of(mockResponse);
      })
    );
  }

  /**
   * Cierra la sesión activa borrando los tokens de LocalStorage.
   */
  logout() {
    localStorage.removeItem('smashrank_token');
    localStorage.removeItem('smashrank_user');
    this.currentUserSignal.set(null);
  }

  /**
   * Retorna el token JWT actual.
   */
  getToken(): string | null {
    return localStorage.getItem('smashrank_token');
  }

  private saveSession(token: string, user: UserProfile) {
    localStorage.setItem('smashrank_token', token);
    localStorage.setItem('smashrank_user', JSON.stringify(user));
    this.currentUserSignal.set(user);
  }

  private loadSessionFromStorage() {
    const token = this.getToken();
    const userStr = localStorage.getItem('smashrank_user');
    
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
