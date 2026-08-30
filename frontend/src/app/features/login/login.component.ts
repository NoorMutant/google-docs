import { Component, OnInit, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { ActivatedRoute, Router } from '@angular/router';
import { AuthService } from '../../core/auth.service';
import { UserSummary } from '../../core/models';
import { messageFrom } from '../../core/api-error';

@Component({
  selector: 'app-login',
  standalone: true,
  imports: [CommonModule, FormsModule],
  templateUrl: './login.component.html',
  styleUrl: './login.component.css',
})
export class LoginComponent implements OnInit {
  readonly demoPassword = 'demo123';

  users = signal<UserSummary[]>([]);
  email = '';
  password = '';
  error = signal<string | null>(null);
  busy = signal(false);

  constructor(
    private auth: AuthService,
    private router: Router,
    private route: ActivatedRoute
  ) {}

  ngOnInit(): void {
    // This also gives the browser its first CSRF cookie, which the login
    // request needs.
    this.auth.demoUsers().subscribe({
      next: (users) => {
        this.users.set(users);
        if (users.length > 0 && !this.email) {
          this.email = users[0].email;
        }
      },
      error: () => this.users.set([]),
    });
  }

  pick(user: UserSummary): void {
    this.email = user.email;
    this.error.set(null);
  }

  submit(): void {
    if (this.busy()) {
      return;
    }
    if (!this.email.trim() || !this.password) {
      this.error.set('Pick an account and enter the password.');
      return;
    }

    this.busy.set(true);
    this.error.set(null);
    this.auth.login(this.email.trim(), this.password).subscribe({
      next: () => {
        const next = this.route.snapshot.queryParamMap.get('next');
        this.router.navigateByUrl(next && next.startsWith('/') ? next : '/documents');
      },
      error: (err) => {
        this.error.set(messageFrom(err));
        this.busy.set(false);
      },
    });
  }
}
