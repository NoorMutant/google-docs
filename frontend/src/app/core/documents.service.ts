import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import {
  AttachmentView,
  DocumentDetail,
  DocumentLists,
  ShareRole,
  ShareView,
  VersionDetail,
  VersionSummary,
} from './models';

@Injectable({ providedIn: 'root' })
export class DocumentsService {
  constructor(private http: HttpClient) {}

  list(): Observable<DocumentLists> {
    return this.http.get<DocumentLists>('/api/documents');
  }

  get(id: number): Observable<DocumentDetail> {
    return this.http.get<DocumentDetail>(`/api/documents/${id}`);
  }

  create(title?: string): Observable<DocumentDetail> {
    return this.http.post<DocumentDetail>('/api/documents', { title: title ?? null });
  }

  /** Send only what changed. The editor autosaves content and renames send the title. */
  update(id: number, changes: { title?: string; contentHtml?: string }): Observable<DocumentDetail> {
    return this.http.patch<DocumentDetail>(`/api/documents/${id}`, changes);
  }

  remove(id: number): Observable<void> {
    return this.http.delete<void>(`/api/documents/${id}`);
  }

  importFile(file: File): Observable<DocumentDetail> {
    const form = new FormData();
    form.append('file', file);
    return this.http.post<DocumentDetail>('/api/documents/import', form);
  }

  listShares(documentId: number): Observable<ShareView[]> {
    return this.http.get<ShareView[]>(`/api/documents/${documentId}/shares`);
  }

  share(documentId: number, email: string, role: ShareRole): Observable<ShareView> {
    return this.http.post<ShareView>(`/api/documents/${documentId}/shares`, { email, role });
  }

  unshare(documentId: number, userId: number): Observable<void> {
    return this.http.delete<void>(`/api/documents/${documentId}/shares/${userId}`);
  }

  listAttachments(documentId: number): Observable<AttachmentView[]> {
    return this.http.get<AttachmentView[]>(`/api/documents/${documentId}/attachments`);
  }

  uploadAttachment(documentId: number, file: File): Observable<AttachmentView> {
    const form = new FormData();
    form.append('file', file);
    return this.http.post<AttachmentView>(`/api/documents/${documentId}/attachments`, form);
  }

  deleteAttachment(documentId: number, attachmentId: number): Observable<void> {
    return this.http.delete<void>(`/api/documents/${documentId}/attachments/${attachmentId}`);
  }

  listVersions(documentId: number): Observable<VersionSummary[]> {
    return this.http.get<VersionSummary[]>(`/api/documents/${documentId}/versions`);
  }

  getVersion(documentId: number, versionId: number): Observable<VersionDetail> {
    return this.http.get<VersionDetail>(`/api/documents/${documentId}/versions/${versionId}`);
  }

  restoreVersion(documentId: number, versionId: number): Observable<DocumentDetail> {
    return this.http.post<DocumentDetail>(
      `/api/documents/${documentId}/versions/${versionId}/restore`,
      {}
    );
  }

  attachmentUrl(documentId: number, attachmentId: number): string {
    return `/api/documents/${documentId}/attachments/${attachmentId}`;
  }
}
