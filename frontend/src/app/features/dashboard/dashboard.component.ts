import { Component, OnInit, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { Router } from '@angular/router';
import { AuthService } from '../../core/auth.service';
import { DocumentsService } from '../../core/documents.service';
import { DocumentSummary } from '../../core/models';
import { relativeTime, exactTime } from '../../core/relative-time';
import { ConfirmDialogComponent } from '../../core/confirm-dialog.component';
import { messageFrom } from '../../core/api-error';

@Component({
  selector: 'app-dashboard',
  standalone: true,
  imports: [CommonModule, ConfirmDialogComponent],
  templateUrl: './dashboard.component.html',
  styleUrl: './dashboard.component.css',
})
export class DashboardComponent implements OnInit {
  readonly acceptedImports = '.txt,.md,.markdown,.docx';

  owned = signal<DocumentSummary[]>([]);
  shared = signal<DocumentSummary[]>([]);
  loading = signal(true);
  busy = signal(false);
  error = signal<string | null>(null);
  pendingDelete = signal<DocumentSummary | null>(null);

  constructor(
    public auth: AuthService,
    private documents: DocumentsService,
    private router: Router
  ) {}

  ngOnInit(): void {
    this.refresh();
  }

  refresh(): void {
    this.loading.set(true);
    this.documents.list().subscribe({
      next: (lists) => {
        this.owned.set(lists.owned);
        this.shared.set(lists.sharedWithMe);
        this.loading.set(false);
      },
      error: (err) => {
        this.error.set(messageFrom(err));
        this.loading.set(false);
      },
    });
  }

  createBlank(): void {
    if (this.busy()) {
      return;
    }
    this.busy.set(true);
    this.error.set(null);
    this.documents.create().subscribe({
      next: (doc) => this.router.navigate(['/documents', doc.id]),
      error: (err) => {
        this.error.set(messageFrom(err));
        this.busy.set(false);
      },
    });
  }

  importSelected(event: Event): void {
    const input = event.target as HTMLInputElement;
    const file = input.files?.[0];
    if (!file) {
      return;
    }

    this.busy.set(true);
    this.error.set(null);
    this.documents.importFile(file).subscribe({
      next: (doc) => this.router.navigate(['/documents', doc.id]),
      error: (err) => {
        this.error.set(messageFrom(err));
        this.busy.set(false);
        // Clearing the input lets the same file be picked again after a fix.
        input.value = '';
      },
    });
  }

  deleteMessage(doc: DocumentSummary): string {
    return `"${doc.title}" and its attachments and history will be removed. This cannot be undone.`;
  }

  askToDelete(doc: DocumentSummary, event: MouseEvent): void {
    event.stopPropagation();
    this.pendingDelete.set(doc);
  }

  confirmDelete(): void {
    const doc = this.pendingDelete();
    if (!doc) {
      return;
    }
    this.pendingDelete.set(null);
    this.documents.remove(doc.id).subscribe({
      next: () => this.refresh(),
      error: (err) => this.error.set(messageFrom(err)),
    });
  }

  open(doc: DocumentSummary): void {
    this.router.navigate(['/documents', doc.id]);
  }

  signOut(): void {
    this.auth.logout().subscribe({
      next: () => this.router.navigate(['/login']),
      error: () => this.router.navigate(['/login']),
    });
  }

  when(iso: string): string {
    return relativeTime(iso);
  }

  exact(iso: string): string {
    return exactTime(iso);
  }
}
