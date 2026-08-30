import { Routes } from '@angular/router';
import { authGuard } from './core/auth.guard';

export const routes: Routes = [
  { path: '', pathMatch: 'full', redirectTo: 'documents' },
  {
    path: 'login',
    title: 'Sign in',
    loadComponent: () => import('./features/login/login.component').then((m) => m.LoginComponent),
  },
  {
    path: 'documents',
    title: 'My documents',
    canActivate: [authGuard],
    loadComponent: () =>
      import('./features/dashboard/dashboard.component').then((m) => m.DashboardComponent),
  },
  {
    path: 'documents/:id',
    canActivate: [authGuard],
    loadComponent: () =>
      import('./features/editor/editor.component').then((m) => m.EditorComponent),
  },
  { path: '**', redirectTo: 'documents' },
];
