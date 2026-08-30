import { Component, Input, OnInit, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { DocumentsService } from '../../core/documents.service';
import { ShareRole, ShareView, UserSummary } from '../../core/models';
import { messageFrom } from '../../core/api-error';
import { AuthService } from '../../core/auth.service';

@Component({
  selector: 'app-share-panel',
  standalone: true,
  imports: [CommonModule, FormsModule],
  templateUrl: './share-panel.component.html',
  styleUrl: './share-panel.component.css',
})
export class SharePanelComponent implements OnInit {
  @Input({ required: true }) documentId!: number;
  @Input() canManage = false;
  @Input() ownerName = '';

  shares = signal<ShareView[]>([]);
  people = signal<UserSummary[]>([]);
  email = '';
  role: ShareRole = 'VIEWER';
  error = signal<string | null>(null);
  busy = signal(false);

  constructor(
    private documents: DocumentsService,
    private auth: AuthService
  ) {}

  ngOnInit(): void {
    this.refresh();
    if (this.canManage) {
      // The seeded account list doubles as the people picker for this demo.
      this.auth.demoUsers().subscribe({
        next: (users) => this.people.set(users),
        error: () => this.people.set([]),
      });
    }
  }

  refresh(): void {
    this.documents.listShares(this.documentId).subscribe({
      next: (shares) => this.shares.set(shares),
      error: (err) => this.error.set(messageFrom(err)),
    });
  }

  /** Accounts that are not the owner and do not already have access. */
  get selectable(): UserSummary[] {
    const me = this.auth.currentUser();
    const taken = new Set(this.shares().map((share) => share.email.toLowerCase()));
    return this.people().filter(
      (user) => user.email !== me?.email && !taken.has(user.email.toLowerCase())
    );
  }

  add(): void {
    if (!this.email.trim()) {
      this.error.set('Pick someone to share with.');
      return;
    }
    this.busy.set(true);
    this.error.set(null);
    this.documents.share(this.documentId, this.email.trim(), this.role).subscribe({
      next: () => {
        this.email = '';
        this.busy.set(false);
        this.refresh();
      },
      error: (err) => {
        this.error.set(messageFrom(err));
        this.busy.set(false);
      },
    });
  }

  changeRole(share: ShareView, role: ShareRole): void {
    this.documents.share(this.documentId, share.email, role).subscribe({
      next: () => this.refresh(),
      error: (err) => this.error.set(messageFrom(err)),
    });
  }

  remove(share: ShareView): void {
    this.documents.unshare(this.documentId, share.userId).subscribe({
      next: () => this.refresh(),
      error: (err) => this.error.set(messageFrom(err)),
    });
  }
}
