import { ComponentFixture, TestBed } from '@angular/core/testing';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { provideHttpClient } from '@angular/common/http';
import { SharePanelComponent } from './share-panel.component';
import { AuthService } from '../../core/auth.service';

describe('SharePanelComponent', () => {
  let fixture: ComponentFixture<SharePanelComponent>;
  let component: SharePanelComponent;
  let http: HttpTestingController;

  const everyone = [
    { id: 1, email: 'alice@ajaia.test', displayName: 'Alice Bennett' },
    { id: 2, email: 'bob@ajaia.test', displayName: 'Bob Carter' },
    { id: 3, email: 'carol@ajaia.test', displayName: 'Carol Diaz' },
  ];

  const existingShares = [
    { userId: 2, email: 'bob@ajaia.test', displayName: 'Bob Carter', role: 'EDITOR' as const },
  ];

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [SharePanelComponent],
      providers: [provideHttpClient(), provideHttpClientTesting()],
    }).compileComponents();

    fixture = TestBed.createComponent(SharePanelComponent);
    component = fixture.componentInstance;
    component.documentId = 5;
    http = TestBed.inject(HttpTestingController);

    // The signed in user is the owner in these tests.
    TestBed.inject(AuthService).currentUser.set(everyone[0]);
  });

  afterEach(() => http.verify());

  function initAsOwner(): void {
    component.canManage = true;
    fixture.detectChanges();
    http.expectOne('/api/documents/5/shares').flush(existingShares);
    http.expectOne('/api/auth/demo-users').flush(everyone);
    fixture.detectChanges();
  }

  it('offers only people who do not already have access', () => {
    initAsOwner();

    const emails = component.selectable.map((user) => user.email);
    expect(emails).toEqual(['carol@ajaia.test']);
  });

  it('never offers the current user to themselves', () => {
    initAsOwner();

    expect(component.selectable.some((user) => user.email === 'alice@ajaia.test')).toBeFalse();
  });

  it('does not load the people picker for someone who cannot manage sharing', () => {
    component.canManage = false;
    fixture.detectChanges();
    http.expectOne('/api/documents/5/shares').flush(existingShares);

    // An editor or viewer sees the list but gets no picker, so no second call.
    http.expectNone('/api/auth/demo-users');
    expect(component.shares().length).toBe(1);
  });

  it('refuses to share when nobody has been chosen', () => {
    initAsOwner();
    component.email = '';

    component.add();

    expect(component.error()).toBe('Pick someone to share with.');
    http.expectNone({ method: 'POST', url: '/api/documents/5/shares' });
  });

  it('shows the server message when the address has no account', () => {
    initAsOwner();
    component.email = 'ghost@ajaia.test';

    component.add();
    http.expectOne('/api/documents/5/shares').flush(
      { status: 404, message: 'No user is registered with ghost@ajaia.test' },
      { status: 404, statusText: 'Not Found' }
    );

    expect(component.error()).toBe('No user is registered with ghost@ajaia.test');
    expect(component.busy()).toBeFalse();
  });

  it('reloads the list and clears the picker after a successful share', () => {
    initAsOwner();
    component.email = 'carol@ajaia.test';

    component.add();
    http.expectOne('/api/documents/5/shares').flush({
      userId: 3,
      email: 'carol@ajaia.test',
      displayName: 'Carol Diaz',
      role: 'VIEWER',
    });

    expect(component.email).toBe('');
    http.expectOne('/api/documents/5/shares').flush([
      ...existingShares,
      { userId: 3, email: 'carol@ajaia.test', displayName: 'Carol Diaz', role: 'VIEWER' },
    ]);
    expect(component.shares().length).toBe(2);
  });

  it('changes a role by resharing with the same address', () => {
    initAsOwner();

    component.changeRole(existingShares[0], 'VIEWER');

    const request = http.expectOne('/api/documents/5/shares');
    expect(request.request.method).toBe('POST');
    expect(request.request.body).toEqual({ email: 'bob@ajaia.test', role: 'VIEWER' });
    request.flush({ ...existingShares[0], role: 'VIEWER' });
    http.expectOne('/api/documents/5/shares').flush([{ ...existingShares[0], role: 'VIEWER' }]);
  });

  it('surfaces a refusal when a non owner tries to remove a share', () => {
    initAsOwner();

    component.remove(existingShares[0]);
    http.expectOne('/api/documents/5/shares/2').flush(
      { status: 403, message: 'Only the document owner can do this' },
      { status: 403, statusText: 'Forbidden' }
    );

    expect(component.error()).toBe('Only the document owner can do this');
  });
});
