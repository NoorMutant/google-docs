import { Injectable, signal } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable, of, tap } from 'rxjs';
import { catchError, map } from 'rxjs/operators';
import { UserSummary } from './models';

/**
 * Holds the signed in user. The session itself lives in an httpOnly cookie, so
 * this is only a cache of who the server says we are.
 */
@Injectable({ providedIn: 'root' })
export class AuthService {
  readonly currentUser = signal<UserSummary | null>(null);

  constructor(private http: HttpClient) {}

  login(email: string, password: string): Observable<UserSummary> {
    return this.http
      .post<UserSummary>('/api/auth/login', { email, password })
      .pipe(tap((user) => this.currentUser.set(user)));
  }

  logout(): Observable<void> {
    return this.http.post<void>('/api/auth/logout', {}).pipe(tap(() => this.currentUser.set(null)));
  }

  /** Called once when the app boots so a refresh keeps you signed in. */
  restoreSession(): Observable<boolean> {
    return this.http.get<UserSummary>('/api/auth/me').pipe(
      tap((user) => this.currentUser.set(user)),
      map(() => true),
      catchError(() => {
        this.currentUser.set(null);
        return of(false);
      })
    );
  }

  demoUsers(): Observable<UserSummary[]> {
    return this.http.get<UserSummary[]>('/api/auth/demo-users');
  }
}
