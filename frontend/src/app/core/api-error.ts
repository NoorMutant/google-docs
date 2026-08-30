import { HttpErrorResponse } from '@angular/common/http';

/**
 * The backend always answers failures with { status, message }. Anything that
 * does not follow that shape is a network or server problem, so we fall back to
 * a message the user can act on.
 */
export function messageFrom(error: unknown): string {
  if (error instanceof HttpErrorResponse) {
    if (error.status === 0) {
      return 'Cannot reach the server. Check your connection and try again.';
    }
    const body = error.error as { message?: string } | null;
    if (body && typeof body.message === 'string' && body.message.trim().length > 0) {
      return body.message;
    }
    if (error.status === 413) {
      return 'That file is too large to upload.';
    }
  }
  return 'Something went wrong. Please try again.';
}
