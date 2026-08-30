import { inject } from '@angular/core';
import { CanActivateFn, Router } from '@angular/router';
import { map } from 'rxjs/operators';
import { of } from 'rxjs';
import { AuthService } from './auth.service';

/**
 * Keeps the dashboard and editor behind a session. On a hard refresh the user
 * is not in memory yet, so we ask the server before deciding.
 */
export const authGuard: CanActivateFn = (_route, state) => {
  const auth = inject(AuthService);
  const router = inject(Router);

  const known = auth.currentUser();
  const check$ = known ? of(true) : auth.restoreSession();

  return check$.pipe(
    map((signedIn) =>
      signedIn ? true : router.createUrlTree(['/login'], { queryParams: { next: state.url } })
    )
  );
};
