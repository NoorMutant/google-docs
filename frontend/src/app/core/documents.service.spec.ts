import { TestBed } from '@angular/core/testing';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { HttpErrorResponse, provideHttpClient } from '@angular/common/http';
import { DocumentsService } from './documents.service';
import { messageFrom } from './api-error';

describe('DocumentsService', () => {
  let service: DocumentsService;
  let http: HttpTestingController;

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [DocumentsService, provideHttpClient(), provideHttpClientTesting()],
    });
    service = TestBed.inject(DocumentsService);
    http = TestBed.inject(HttpTestingController);
  });

  afterEach(() => http.verify());

  describe('sending the right request', () => {
    it('sends only the fields that changed when autosaving content', () => {
      service.update(7, { contentHtml: '<p>hello</p>' }).subscribe();

      const request = http.expectOne('/api/documents/7');
      expect(request.request.method).toBe('PATCH');
      expect(request.request.body).toEqual({ contentHtml: '<p>hello</p>' });
      expect(request.request.body.title).toBeUndefined();
      request.flush({});
    });

    it('sends a rename without touching the content', () => {
      service.update(7, { title: 'Renamed' }).subscribe();

      const request = http.expectOne('/api/documents/7');
      expect(request.request.body).toEqual({ title: 'Renamed' });
      request.flush({});
    });

    it('posts imports as multipart form data under the field name the API expects', () => {
      const file = new File(['# Title'], 'notes.md', { type: 'text/markdown' });
      service.importFile(file).subscribe();

      const request = http.expectOne('/api/documents/import');
      expect(request.request.method).toBe('POST');
      const body = request.request.body as FormData;
      expect(body instanceof FormData).toBeTrue();
      expect((body.get('file') as File).name).toBe('notes.md');
      request.flush({});
    });

    it('builds attachment download urls scoped to their document', () => {
      expect(service.attachmentUrl(3, 12)).toBe('/api/documents/3/attachments/12');
    });

    it('restores a version with a post, not a patch', () => {
      service.restoreVersion(3, 9).subscribe();

      const request = http.expectOne('/api/documents/3/versions/9/restore');
      expect(request.request.method).toBe('POST');
      request.flush({});
    });
  });

  describe('when the server refuses', () => {
    it('surfaces a 403 to the caller rather than swallowing it', () => {
      let caught: HttpErrorResponse | null = null;
      service.update(7, { contentHtml: '<p>x</p>' }).subscribe({
        next: () => fail('the update should not have succeeded'),
        error: (err: HttpErrorResponse) => (caught = err),
      });

      http.expectOne('/api/documents/7').flush(
        { status: 403, message: 'You have view only access to this document' },
        { status: 403, statusText: 'Forbidden' }
      );

      expect(caught!.status).toBe(403);
      expect(messageFrom(caught!)).toBe('You have view only access to this document');
    });

    it('surfaces a 404 when the document is not visible to this user', () => {
      let caught: HttpErrorResponse | null = null;
      service.get(99).subscribe({
        next: () => fail('the load should not have succeeded'),
        error: (err: HttpErrorResponse) => (caught = err),
      });

      http.expectOne('/api/documents/99').flush(
        { status: 404, message: 'Document not found' },
        { status: 404, statusText: 'Not Found' }
      );

      expect(messageFrom(caught!)).toBe('Document not found');
    });

    it('reports an unreachable server differently from a rejected request', () => {
      let caught: HttpErrorResponse | null = null;
      service.list().subscribe({
        next: () => fail('the list should not have succeeded'),
        error: (err: HttpErrorResponse) => (caught = err),
      });

      http.expectOne('/api/documents').error(new ProgressEvent('network error'));

      expect(messageFrom(caught!)).toContain('Cannot reach the server');
    });

    it('falls back to a readable message when the body has no message field', () => {
      let caught: HttpErrorResponse | null = null;
      service.remove(4).subscribe({
        next: () => fail('the delete should not have succeeded'),
        error: (err: HttpErrorResponse) => (caught = err),
      });

      http.expectOne('/api/documents/4').flush(null, { status: 500, statusText: 'Server Error' });

      expect(messageFrom(caught!)).toBe('Something went wrong. Please try again.');
    });

    it('explains an upload that is too large', () => {
      let caught: HttpErrorResponse | null = null;
      const file = new File(['x'], 'big.bin');
      service.uploadAttachment(1, file).subscribe({
        next: () => fail('the upload should not have succeeded'),
        error: (err: HttpErrorResponse) => (caught = err),
      });

      http
        .expectOne('/api/documents/1/attachments')
        .flush(null, { status: 413, statusText: 'Payload Too Large' });

      expect(messageFrom(caught!)).toContain('too large');
    });
  });
});
