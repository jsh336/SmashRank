import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ActivatedRoute, Router } from '@angular/router';
import { AuthService } from '../../core/services/auth.service';

@Component({
  selector: 'app-auth-callback',
  standalone: true,
  imports: [CommonModule],
  templateUrl: './auth-callback.html',
  styleUrls: ['./auth-callback.scss']
})
export class AuthCallbackComponent implements OnInit {
  errorMessage: string | null = null;

  constructor(
    private route: ActivatedRoute,
    private router: Router,
    private authService: AuthService
  ) {}

  ngOnInit() {
    this.route.queryParams.subscribe(params => {
      const code = params['code'];
      if (code) {
        this.processLogin(code);
      } else {
        this.errorMessage = 'No se ha proporcionado el código de autorización en la URL.';
      }
    });
  }

  processLogin(code: string) {
    this.authService.login(code).subscribe({
      next: () => {
        // Redirigir al Dashboard principal tras el inicio de sesión
        this.router.navigate(['/']);
      },
      error: (err) => {
        this.errorMessage = 'Error al verificar las credenciales con Start.gg. Por favor, intente de nuevo.';
        console.error('Error en el flujo de callback:', err);
      }
    });
  }

  retry() {
    this.router.navigate(['/']);
  }
}
