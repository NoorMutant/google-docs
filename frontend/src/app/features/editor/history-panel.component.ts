import { Component, EventEmitter, Input, OnInit, Output, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { DocumentsService } from '../../core/documents.service';
import { VersionDetail, VersionSummary } from '../../core/models';
import { messageFrom } from '../../core/api-error';
import { relativeTime, exactTime } from '../../core/relative-time';
import { ConfirmDialogComponent } from '../../core/confirm-dialog.component';

@Component({
  selector: 'app-history-panel',
  standalone: true,
  imports: [CommonModule, ConfirmDialogComponent],
  templateUrl: './history-panel.component.html',
  styleUrl: './history-panel.component.css',
})
export class HistoryPanelComponent implements OnInit {
  @Input({ required: true }) documentId!: number;
  @Input() canRestore = false;

  /** Tells the editor to reload after a restore has changed the document. */
  @Output() restored = new EventEmitter<void>();

  versions = signal<VersionSummary[]>([]);
  preview = signal<VersionDetail | null>(null);
  loading = signal(true);
  busy = signal(false);
  error = signal<string | null>(null);
  pendingRestore = signal<VersionSummary | null>(null);

  constructor(private documents: DocumentsService) {}

  ngOnInit(): void {
    this.refresh();
  }

  refresh(): void {
    this.loading.set(true);
    this.documents.listVersions(this.documentId).subscribe({
      next: (list) => {
        this.versions.set(list);
        this.loading.set(false);
      },
      error: (err) => {
        this.error.set(messageFrom(err));
        this.loading.set(false);
      },
    });
  }

  open(version: VersionSummary): void {
    if (this.preview()?.id === version.id) {
      this.preview.set(null);
      return;
    }
    this.error.set(null);
    this.documents.getVersion(this.documentId, version.id).subscribe({
      next: (detail) => this.preview.set(detail),
      error: (err) => this.error.set(messageFrom(err)),
    });
  }

  askToRestore(version: VersionSummary): void {
    this.pendingRestore.set(version);
  }

  restoreMessage(version: VersionSummary): string {
    return (
      `Version ${version.versionNumber} will become the current text. ` +
      'Nothing is lost, the version you are on now stays in the history.'
    );
  }

  confirmRestore(): void {
    const version = this.pendingRestore();
    if (!version || this.busy()) {
      return;
    }
    this.pendingRestore.set(null);
    this.busy.set(true);
    this.error.set(null);
    this.documents.restoreVersion(this.documentId, version.id).subscribe({
      next: () => {
        this.busy.set(false);
        this.preview.set(null);
        this.refresh();
        this.restored.emit();
      },
      error: (err) => {
        this.error.set(messageFrom(err));
        this.busy.set(false);
      },
    });
  }

  when(iso: string): string {
    return relativeTime(iso);
  }

  exact(iso: string): string {
    return exactTime(iso);
  }
}
