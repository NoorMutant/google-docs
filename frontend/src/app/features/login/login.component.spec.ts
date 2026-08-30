import { ComponentFixture, TestBed } from '@angular/core/testing';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { provideHttpClient } from '@angular/common/http';
import { ActivatedRoute, provideRouter, Router } from '@angular/router';
import { LoginComponent } from './login.component';

describe('LoginComponent', () => {
  let fixture: ComponentFixture<LoginComponent>;
  let component: LoginComponent;
  let http: HttpTestingController;
  let router: Router;

  const seeded = [
    { id: 1, email: 'alice@ajaia.test', displayName: 'Alice Bennett' },
    { id: 2, email: 'bob@ajaia.test', displayName: 'Bob Carter' },
  ];

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [LoginComponent],
      providers: [provideHttpClient(), provideHttpClientTesting(), provideRouter([])],
    }).compileComponents();

    fixture = TestBed.createComponent(LoginComponent);
    component = fixture.componentInstance;
    http = TestBed.inject(HttpTestingController);
    router = TestBed.inject(Router);
    spyOn(router, 'navigateByUrl');
  });

  afterEach(() => http.verify());

  function loadSeededUsers(): void {
    fixture.detectChanges();
    http.expectOne('/api/auth/demo-users').flush(seeded);
    fixture.detectChanges();
  }

  it('lists the seeded accounts and preselects the first one', () => {
    loadSeededUsers();

    expect(component.users().length).toBe(2);
    expect(component.email).toBe('alice@ajaia.test');
  });

  it('shows the demo password so a reviewer does not have to guess it', () => {
    loadSeededUsers();
    const text = (fixture.nativeElement as HTMLElement).textContent ?? '';

    expect(component.demoPassword).toBe('demo123');
    expect(text).toContain('demo123');
  });

  it('still renders when the account list cannot be loaded', () => {
    fixture.detectChanges();
    http.expectOne('/api/auth/demo-users').error(new ProgressEvent('offline'));
    fixture.detectChanges();

    expect(component.users()).toEqual([]);
    expect(component.error()).toBeNull();
  });

  it('refuses to submit without a password and does not call the API', () => {
    loadSeededUsers();
    component.password = '';

    component.submit();

    expect(component.error()).toBe('Pick an account and enter the password.');
    http.expectNone('/api/auth/login');
  });

  it('shows the server message when the password is wrong', () => {
    loadSeededUsers();
    component.password = 'wrong';

    component.submit();
    http.expectOne('/api/auth/login').flush(
      { status: 401, message: 'Email or password is not correct' },
      { status: 401, statusText: 'Unauthorized' }
    );

    expect(component.error()).toBe('Email or password is not correct');
    expect(component.busy()).toBeFalse();
    expect(router.navigateByUrl).not.toHaveBeenCalled();
  });

  it('sends the user to the dashboard after a successful sign in', () => {
    loadSeededUsers();
    component.password = 'demo123';

    component.submit();
    http.expectOne('/api/auth/login').flush(seeded[0]);

    expect(router.navigateByUrl).toHaveBeenCalledWith('/documents');
  });

  it('returns the user to the page they were trying to reach', () => {
    setNextParam('/documents/12');
    loadSeededUsers();
    component.password = 'demo123';

    component.submit();
    http.expectOne('/api/auth/login').flush(seeded[0]);

    expect(router.navigateByUrl).toHaveBeenCalledWith('/documents/12');
  });

  it('ignores an off site next parameter and uses the dashboard instead', () => {
    // Without the leading slash check this would be an open redirect.
    setNextParam('https://evil.example.com');
    loadSeededUsers();
    component.password = 'demo123';

    component.submit();
    http.expectOne('/api/auth/login').flush(seeded[0]);

    expect(router.navigateByUrl).toHaveBeenCalledWith('/documents');
  });

  function setNextParam(value: string): void {
    const route = TestBed.inject(ActivatedRoute);
    (route as { snapshot: unknown }).snapshot = {
      queryParamMap: { get: (key: string) => (key === 'next' ? value : null) },
    };
  }
});
