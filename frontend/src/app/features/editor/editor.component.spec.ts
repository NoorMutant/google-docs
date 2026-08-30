import { ComponentFixture, TestBed, fakeAsync, tick } from '@angular/core/testing';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { provideHttpClient } from '@angular/common/http';
import { ActivatedRoute, provideRouter } from '@angular/router';
import { Title } from '@angular/platform-browser';
import { of } from 'rxjs';
import { convertToParamMap } from '@angular/router';
import { EditorComponent } from './editor.component';
import { AccessLevel, DocumentDetail } from '../../core/models';

describe('EditorComponent', () => {
  let fixture: ComponentFixture<EditorComponent>;
  let component: EditorComponent;
  let http: HttpTestingController;

  const owner = { id: 1, email: 'alice@ajaia.test', displayName: 'Alice Bennett' };

  function documentWith(access: AccessLevel): DocumentDetail {
    return {
      id: 4,
      title: 'Team charter',
      contentHtml: '<p>Existing text</p>',
      owner,
      access,
      createdAt: new Date().toISOString(),
      updatedAt: new Date().toISOString(),
    };
  }

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [EditorComponent],
      providers: [
        provideHttpClient(),
        provideHttpClientTesting(),
        provideRouter([]),
        {
          provide: ActivatedRoute,
          useValue: { paramMap: of(convertToParamMap({ id: '4' })) },
        },
      ],
    }).compileComponents();

    fixture = TestBed.createComponent(EditorComponent);
    component = fixture.componentInstance;
    http = TestBed.inject(HttpTestingController);
  });

  afterEach(() => http.verify());

  function open(access: AccessLevel): void {
    fixture.detectChanges();
    http.expectOne('/api/documents/4').flush(documentWith(access));
    fixture.detectChanges();
  }

  function surface(): HTMLElement {
    return fixture.nativeElement.querySelector('.surface') as HTMLElement;
  }

  describe('as an owner', () => {
    it('writes the saved content into the editing surface', () => {
      open('OWNER');

      expect(surface().innerHTML).toBe('<p>Existing text</p>');
      expect(surface().getAttribute('contenteditable')).toBe('true');
    });

    it('puts the document name in the browser tab', () => {
      open('OWNER');

      expect(TestBed.inject(Title).getTitle()).toBe('Team charter - Docs');
    });

    it('counts words and characters from the loaded content', () => {
      open('OWNER');

      expect(component.words()).toBe(2);
      expect(component.characters()).toBe(13);
    });

    it('waits for a pause in typing before saving, then saves once', fakeAsync(() => {
      open('OWNER');

      surface().innerHTML = '<p>Changed once</p>';
      component.onInput();
      tick(300);
      surface().innerHTML = '<p>Changed twice</p>';
      component.onInput();

      // Still inside the debounce window, so nothing has gone out yet.
      http.expectNone('/api/documents/4');
      expect(component.saveState()).toBe('saving');

      tick(900);
      const request = http.expectOne('/api/documents/4');
      expect(request.request.body).toEqual({ contentHtml: '<p>Changed twice</p>' });
      request.flush(documentWith('OWNER'));

      expect(component.saveState()).toBe('saved');
    }));

    it('reports a failed save instead of pretending it worked', fakeAsync(() => {
      open('OWNER');

      surface().innerHTML = '<p>Doomed</p>';
      component.onInput();
      tick(900);

      http.expectOne('/api/documents/4').flush(
        { status: 500, message: 'Something went wrong on our side' },
        { status: 500, statusText: 'Server Error' }
      );

      expect(component.saveState()).toBe('error');
      expect(component.error()).toBe('Something went wrong on our side');
    }));

    it('does not send a rename when the title has not actually changed', () => {
      open('OWNER');
      component.title = 'Team charter';

      component.saveTitle();

      http.expectNone('/api/documents/4');
    });

    it('puts the old title back if the rename is refused', () => {
      open('OWNER');
      component.title = 'New name';

      component.saveTitle();
      http.expectOne('/api/documents/4').flush(
        { status: 403, message: 'You have view only access to this document' },
        { status: 403, statusText: 'Forbidden' }
      );

      expect(component.title).toBe('Team charter');
      expect(component.saveState()).toBe('error');
    });
  });

  describe('as a viewer', () => {
    it('renders the surface as read only', () => {
      open('VIEWER');

      expect(component.canEdit).toBeFalse();
      expect(surface().getAttribute('contenteditable')).toBe('false');
    });

    it('disables every toolbar button', () => {
      open('VIEWER');
      const buttons: HTMLButtonElement[] = Array.from(
        fixture.nativeElement.querySelectorAll('.tool')
      );

      expect(buttons.length).toBeGreaterThan(0);
      expect(buttons.every((button) => button.disabled)).toBeTrue();
    });

    it('explains why editing is off', () => {
      open('VIEWER');
      const text = (fixture.nativeElement as HTMLElement).textContent ?? '';

      expect(text).toContain('shared this with you as a viewer');
    });

    it('never sends a save even if an input event arrives', fakeAsync(() => {
      open('VIEWER');

      component.onInput();
      tick(900);

      http.expectNone('/api/documents/4');
    }));

    it('cannot restore a version', () => {
      open('VIEWER');
      component.openPanel('history');
      fixture.detectChanges();
      http.expectOne('/api/documents/4/versions').flush([]);
      fixture.detectChanges();

      const panel = fixture.nativeElement.querySelector('app-history-panel');
      expect(panel).toBeTruthy();
      expect(component.canEdit).toBeFalse();
    });
  });

  describe('when the document cannot be opened', () => {
    it('shows a readable message rather than an empty editor', () => {
      fixture.detectChanges();
      http.expectOne('/api/documents/4').flush(
        { status: 404, message: 'Document not found' },
        { status: 404, statusText: 'Not Found' }
      );
      fixture.detectChanges();

      const text = (fixture.nativeElement as HTMLElement).textContent ?? '';
      expect(text).toContain('Document not available');
      expect(text).toContain('Document not found');
      expect(fixture.nativeElement.querySelector('.surface')).toBeNull();
    });
  });

  describe('the side panel', () => {
    it('opens on the tab that was clicked and closes when clicked again', () => {
      open('OWNER');

      component.openPanel('files');
      expect(component.showPanel()).toBeTrue();
      expect(component.panelTab()).toBe('files');

      fixture.detectChanges();
      http.expectOne('/api/documents/4/attachments').flush([]);

      component.openPanel('files');
      expect(component.showPanel()).toBeFalse();
    });
  });
});
