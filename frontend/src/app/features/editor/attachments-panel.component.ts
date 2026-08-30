import { Component, Input, OnInit, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { DocumentsService } from '../../core/documents.service';
import { AttachmentView } from '../../core/models';
import { messageFrom } from '../../core/api-error';

@Component({
  selector: 'app-attachments-panel',
  standalone: true,
  imports: [CommonModule],
  templateUrl: './attachments-panel.component.html',
  styleUrl: './attachments-panel.component.css',
})
export class AttachmentsPanelComponent implements OnInit {
  @Input({ required: true }) documentId!: number;
  @Input() canEdit = false;

  attachments = signal<AttachmentView[]>([]);
  error = signal<string | null>(null);
  busy = signal(false);

  constructor(private documents: DocumentsService) {}

  ngOnInit(): void {
    this.refresh();
  }

  refresh(): void {
    this.documents.listAttachments(this.documentId).subscribe({
      next: (list) => this.attachments.set(list),
      error: (err) => this.error.set(messageFrom(err)),
    });
  }

  upload(event: Event): void {
    const input = event.target as HTMLInputElement;
    const file = input.files?.[0];
    if (!file) {
      return;
    }

    this.busy.set(true);
    this.error.set(null);
    this.documents.uploadAttachment(this.documentId, file).subscribe({
      next: () => {
        this.busy.set(false);
        input.value = '';
        this.refresh();
      },
      error: (err) => {
        this.error.set(messageFrom(err));
        this.busy.set(false);
        input.value = '';
      },
    });
  }

  remove(attachment: AttachmentView): void {
    this.documents.deleteAttachment(this.documentId, attachment.id).subscribe({
      next: () => this.refresh(),
      error: (err) => this.error.set(messageFrom(err)),
    });
  }

  downloadUrl(attachment: AttachmentView): string {
    return this.documents.attachmentUrl(this.documentId, attachment.id);
  }

  size(bytes: number): string {
    if (bytes < 1024) {
      return `${bytes} B`;
    }
    if (bytes < 1024 * 1024) {
      return `${Math.round(bytes / 1024)} KB`;
    }
    return `${(bytes / (1024 * 1024)).toFixed(1)} MB`;
  }
}
