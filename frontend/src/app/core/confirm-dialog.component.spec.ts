import { ComponentFixture, TestBed } from '@angular/core/testing';
import { ConfirmDialogComponent } from './confirm-dialog.component';

describe('ConfirmDialogComponent', () => {
  let fixture: ComponentFixture<ConfirmDialogComponent>;
  let component: ConfirmDialogComponent;

  beforeEach(async () => {
    await TestBed.configureTestingModule({ imports: [ConfirmDialogComponent] }).compileComponents();
    fixture = TestBed.createComponent(ConfirmDialogComponent);
    component = fixture.componentInstance;
    component.title = 'Delete this document?';
    component.message = 'This cannot be undone.';
    component.confirmLabel = 'Delete';
    component.destructive = true;
    fixture.detectChanges();
  });

  function buttons(): HTMLButtonElement[] {
    return Array.from(fixture.nativeElement.querySelectorAll('button'));
  }

  it('is announced as a dialog that needs a decision', () => {
    const dialog = fixture.nativeElement.querySelector('.dialog') as HTMLElement;

    expect(dialog.getAttribute('role')).toBe('alertdialog');
    expect(dialog.getAttribute('aria-modal')).toBe('true');
    expect(dialog.getAttribute('aria-label')).toBe('Delete this document?');
  });

  it('shows the message and the caller supplied action label', () => {
    const text = (fixture.nativeElement as HTMLElement).textContent ?? '';

    expect(text).toContain('This cannot be undone.');
    expect(buttons().some((b) => b.textContent?.trim() === 'Delete')).toBeTrue();
  });

  it('marks a destructive action visually rather than leaving it as a primary button', () => {
    const confirmButton = buttons().find((b) => b.textContent?.trim() === 'Delete');

    expect(confirmButton?.classList).toContain('btn-danger');
    expect(confirmButton?.classList).not.toContain('btn-primary');
  });

  it('uses the primary style when the action is not destructive', () => {
    component.destructive = false;
    component.confirmLabel = 'Restore';
    fixture.detectChanges();

    const confirmButton = buttons().find((b) => b.textContent?.trim() === 'Restore');
    expect(confirmButton?.classList).toContain('btn-primary');
  });

  it('emits confirmed only when the action button is pressed', () => {
    const confirmed = jasmine.createSpy('confirmed');
    const cancelled = jasmine.createSpy('cancelled');
    component.confirmed.subscribe(confirmed);
    component.cancelled.subscribe(cancelled);

    buttons().find((b) => b.textContent?.trim() === 'Delete')?.click();

    expect(confirmed).toHaveBeenCalledTimes(1);
    expect(cancelled).not.toHaveBeenCalled();
  });

  it('cancels on the cancel button, on the backdrop, and on Escape', () => {
    const cancelled = jasmine.createSpy('cancelled');
    component.cancelled.subscribe(cancelled);

    buttons().find((b) => b.textContent?.trim() === 'Cancel')?.click();
    (fixture.nativeElement.querySelector('.backdrop') as HTMLElement).click();
    component.onEscape();

    expect(cancelled).toHaveBeenCalledTimes(3);
  });

  it('does not cancel when the dialog body itself is clicked', () => {
    const cancelled = jasmine.createSpy('cancelled');
    component.cancelled.subscribe(cancelled);

    (fixture.nativeElement.querySelector('.dialog') as HTMLElement).click();

    expect(cancelled).not.toHaveBeenCalled();
  });
});
