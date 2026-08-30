import { Component, EventEmitter, HostListener, Input, Output } from '@angular/core';
import { CommonModule } from '@angular/common';

/**
 * Replaces window.confirm for destructive actions.
 *
 * The native dialog cannot be styled, cannot say which button is the dangerous
 * one, and on some browsers it is suppressed entirely. This is a small modal
 * that traps Escape, labels the destructive action, and can be read by a screen
 * reader.
 */
@Component({
  selector: 'app-confirm-dialog',
  standalone: true,
  imports: [CommonModule],
  template: `
    <div class="backdrop" (click)="cancelled.emit()">
      <div
        class="dialog card"
        role="alertdialog"
        aria-modal="true"
        [attr.aria-label]="title"
        (click)="$event.stopPropagation()">
        <h2 class="title">{{ title }}</h2>
        <p class="body">{{ message }}</p>
        <div class="actions">
          <button class="btn" type="button" (click)="cancelled.emit()">Cancel</button>
          <button
            class="btn"
            [class.btn-danger]="destructive"
            [class.btn-primary]="!destructive"
            type="button"
            autofocus
            (click)="confirmed.emit()">
            {{ confirmLabel }}
          </button>
        </div>
      </div>
    </div>
  `,
  styles: [
    `
      .backdrop {
        position: fixed;
        inset: 0;
        background: rgba(24, 28, 38, 0.45);
        display: grid;
        place-items: center;
        padding: 20px;
        z-index: 40;
      }

      .dialog {
        width: 100%;
        max-width: 400px;
        padding: 22px;
      }

      .title {
        font-size: 17px;
        margin: 0 0 8px;
      }

      .body {
        margin: 0 0 18px;
        color: var(--text-muted);
        font-size: 14px;
      }

      .actions {
        display: flex;
        justify-content: flex-end;
        gap: 8px;
      }
    `,
  ],
})
export class ConfirmDialogComponent {
  @Input() title = 'Are you sure?';
  @Input() message = '';
  @Input() confirmLabel = 'Confirm';
  @Input() destructive = false;

  @Output() confirmed = new EventEmitter<void>();
  @Output() cancelled = new EventEmitter<void>();

  /** Escape closes the dialog without acting, which is what people expect. */
  @HostListener('document:keydown.escape')
  onEscape(): void {
    this.cancelled.emit();
  }
}
