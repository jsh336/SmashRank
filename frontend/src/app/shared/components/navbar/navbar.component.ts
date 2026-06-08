import { Component } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterLink, RouterLinkActive } from '@angular/router';
import { AuthService } from '../../../core/services/auth.service';

@Component({
  selector: 'app-navbar',
  standalone: true,
  imports: [CommonModule, RouterLink, RouterLinkActive],
  templateUrl: './navbar.component.html',
  styleUrls: ['./navbar.component.scss']
})
export class NavbarComponent {
  constructor(public authService: AuthService) {}

  login() {
    this.authService.getLoginUrl().subscribe({
      next: (res) => {
        if (res && res.url) {
          window.location.href = res.url;
        }
      },
      error: (err) => {
        console.error('Error al obtener la URL de inicio de sesión:', err);
      }
    });
  }

  logout() {
    this.authService.logout();
  }
}
