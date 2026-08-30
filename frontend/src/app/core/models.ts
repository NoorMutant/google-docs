export type AccessLevel = 'OWNER' | 'EDITOR' | 'VIEWER' | 'NONE';
export type ShareRole = 'VIEWER' | 'EDITOR';

export interface UserSummary {
  id: number;
  email: string;
  displayName: string;
}

export interface DocumentSummary {
  id: number;
  title: string;
  owner: UserSummary;
  access: AccessLevel;
  updatedAt: string;
}

export interface DocumentDetail extends DocumentSummary {
  contentHtml: string;
  createdAt: string;
}

export interface DocumentLists {
  owned: DocumentSummary[];
  sharedWithMe: DocumentSummary[];
}

export interface ShareView {
  userId: number;
  email: string;
  displayName: string;
  role: ShareRole;
}

export interface AttachmentView {
  id: number;
  filename: string;
  contentType: string;
  sizeBytes: number;
  uploadedAt: string;
}

export interface VersionSummary {
  id: number;
  versionNumber: number;
  title: string;
  savedBy: UserSummary;
  savedAt: string;
  restoredFromVersion: number | null;
  current: boolean;
}

export interface VersionDetail {
  id: number;
  versionNumber: number;
  title: string;
  contentHtml: string;
  savedBy: UserSummary;
  savedAt: string;
}
