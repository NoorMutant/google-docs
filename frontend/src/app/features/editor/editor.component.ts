import {
  Component,
  ElementRef,
  HostListener,
  OnDestroy,
  OnInit,
  ViewChild,
  signal,
} from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { Title } from '@angular/platform-browser';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';
import { Subject, Subscription, debounceTime } from 'rxjs';
import { DocumentsService } from '../../core/documents.service';
import { AuthService } from '../../core/auth.service';
import { DocumentDetail } from '../../core/models';
import { messageFrom } from '../../core/api-error';
import { cleanPastedHtml, countWords, plainTextToHtml } from '../../core/html-clean';
import { SharePanelComponent } from './share-panel.component';
import { AttachmentsPanelComponent } from './attachments-panel.component';
import { HistoryPanelComponent } from './history-panel.component';

type SaveState = 'idle' | 'saving' | 'saved' | 'error';
type PanelTab = 'share' | 'files' | 'history';

interface ToolbarButton {
  command: string;
  value?: string;
  label: string;
  title: string;
  shortcut?: string;
  style?: string;
  /** Block level buttons highlight by block name rather than by command state. */
  block?: string;
}

@Component({
  selector: 'app-editor',
  standalone: true,
  imports: [
    CommonModule,
    FormsModule,
    RouterLink,
    SharePanelComponent,
    AttachmentsPanelComponent,
    HistoryPanelComponent,
  ],
  templateUrl: './editor.component.html',
  styleUrl: './editor.component.css',
})
export class EditorComponent implements OnInit, OnDestroy {
  private surfaceEl?: HTMLDivElement;

  /**
   * The surface only exists once the document has loaded, so this is a setter
   * rather than a plain ViewChild. It fires the moment the element appears,
   * which is when the saved content can be written into it.
   */
  @ViewChild('surface')
  set surfaceRef(ref: ElementRef<HTMLDivElement> | undefined) {
    this.surfaceEl = ref?.nativeElement;
    this.paintContent();
  }

  readonly historyTools: ToolbarButton[] = [
    { command: 'undo', label: 'Undo', title: 'Undo', shortcut: 'Ctrl+Z' },
    { command: 'redo', label: 'Redo', title: 'Redo', shortcut: 'Ctrl+Y' },
  ];

  readonly inlineTools: ToolbarButton[] = [
    { command: 'bold', label: 'B', title: 'Bold', shortcut: 'Ctrl+B', style: 'font-weight:700' },
    { command: 'italic', label: 'I', title: 'Italic', shortcut: 'Ctrl+I', style: 'font-style:italic' },
    {
      command: 'underline',
      label: 'U',
      title: 'Underline',
      shortcut: 'Ctrl+U',
      style: 'text-decoration:underline',
    },
  ];

  readonly blockTools: ToolbarButton[] = [
    { command: 'formatBlock', value: 'h1', label: 'H1', title: 'Large heading', block: 'h1' },
    { command: 'formatBlock', value: 'h2', label: 'H2', title: 'Medium heading', block: 'h2' },
    { command: 'formatBlock', value: 'p', label: 'Body', title: 'Normal text', block: 'p' },
  ];

  readonly listTools: ToolbarButton[] = [
    { command: 'insertUnorderedList', label: 'List', title: 'Bulleted list' },
    { command: 'insertOrderedList', label: '1. List', title: 'Numbered list' },
  ];

  doc = signal<DocumentDetail | null>(null);
  title = '';
  loading = signal(true);
  saveState = signal<SaveState>('idle');
  error = signal<string | null>(null);
  showPanel = signal(false);
  panelTab = signal<PanelTab>('share');
  isEmpty = signal(true);
  words = signal(0);
  characters = signal(0);
  activeBlock = signal('p');
  activeInline = signal<Record<string, boolean>>({});

  private readonly contentChanges = new Subject<void>();
  private subscription = new Subscription();
  private contentLoaded = false;
  private dirty = false;
  private documentId = 0;

  constructor(
    private route: ActivatedRoute,
    private router: Router,
    private documents: DocumentsService,
    private pageTitle: Title,
    public auth: AuthService
  ) {}

  get canEdit(): boolean {
    const access = this.doc()?.access;
    return access === 'OWNER' || access === 'EDITOR';
  }

  get isOwner(): boolean {
    return this.doc()?.access === 'OWNER';
  }

  ngOnInit(): void {
    // Typing fires on every keystroke, so the save is held back until the
    // user pauses. One request per pause instead of one per character.
    this.subscription.add(
      this.contentChanges.pipe(debounceTime(900)).subscribe(() => this.saveContent())
    );

    // Reading the id from the stream rather than the snapshot means moving
    // straight from one document to another loads the new one.
    this.subscription.add(
      this.route.paramMap.subscribe((params) => {
        const id = Number(params.get('id'));
        if (id && id !== this.documentId) {
          this.documentId = id;
          this.contentLoaded = false;
          this.loading.set(true);
          this.load();
        }
      })
    );
  }

  ngOnDestroy(): void {
    // Leaving mid edit should not lose the last few words.
    if (this.dirty) {
      this.saveContent();
    }
    this.subscription.unsubscribe();
    this.pageTitle.setTitle('Docs');
  }

  /** Only fires when the tab or window is closing, where a flush is not reliable. */
  @HostListener('window:beforeunload', ['$event'])
  warnAboutUnsavedWork(event: BeforeUnloadEvent): void {
    if (this.dirty) {
      event.preventDefault();
    }
  }

  private load(): void {
    this.documents.get(this.documentId).subscribe({
      next: (doc) => {
        this.doc.set(doc);
        this.title = doc.title;
        this.pageTitle.setTitle(`${doc.title} - Docs`);
        this.loading.set(false);
        this.contentLoaded = false;
        this.paintContent();
      },
      error: (err) => {
        this.error.set(messageFrom(err));
        this.loading.set(false);
      },
    });
  }

  /**
   * The editing surface is written to once, not bound. Rebinding innerHTML on
   * every change would move the caret back to the start while typing.
   */
  private paintContent(): void {
    const host = this.surfaceEl;
    const doc = this.doc();
    if (!host || !doc || this.contentLoaded) {
      return;
    }
    host.innerHTML = doc.contentHtml || '<p><br></p>';
    this.contentLoaded = true;
    this.updateCounts();
  }

  onInput(): void {
    if (!this.canEdit) {
      return;
    }
    this.dirty = true;
    this.saveState.set('saving');
    this.updateCounts();
    this.contentChanges.next();
  }

  /**
   * Pasted markup is cleaned to the same set of tags the server keeps. Without
   * this a paste from Word looks right until the next reload, then silently
   * loses its colours and fonts.
   */
  onPaste(event: ClipboardEvent): void {
    if (!this.canEdit) {
      return;
    }
    event.preventDefault();

    const clipboard = event.clipboardData;
    const html = clipboard?.getData('text/html') ?? '';
    const text = clipboard?.getData('text/plain') ?? '';
    const cleaned = html.trim().length > 0 ? cleanPastedHtml(html) : plainTextToHtml(text);

    document.execCommand('insertHTML', false, cleaned);
    this.onInput();
  }

  /**
   * Tab inside a list should nest the item, which is what every editor does.
   * The browser default is to move focus out of the document entirely.
   */
  onKeydown(event: KeyboardEvent): void {
    if (event.key !== 'Tab' || !this.canEdit) {
      return;
    }
    if (!this.isInsideList()) {
      return;
    }
    event.preventDefault();
    document.execCommand(event.shiftKey ? 'outdent' : 'indent');
    this.onInput();
  }

  onSelectionChanged(): void {
    this.refreshToolbarState();
  }

  applyCommand(tool: ToolbarButton): void {
    if (!this.canEdit) {
      return;
    }
    this.surfaceEl?.focus();
    // execCommand is deprecated but it is still the only thing every browser
    // implements for contenteditable formatting without pulling in an editor
    // framework. The tradeoff is written up in the architecture note.
    document.execCommand(tool.command, false, tool.value);

    // Undo and redo replace the content wholesale, so the counts and the
    // toolbar have to be recomputed rather than nudged.
    this.onInput();
    this.refreshToolbarState();
  }

  isActive(tool: ToolbarButton): boolean {
    if (tool.block) {
      return this.activeBlock() === tool.block;
    }
    return this.activeInline()[tool.command] === true;
  }

  tooltip(tool: ToolbarButton): string {
    return tool.shortcut ? `${tool.title} (${tool.shortcut})` : tool.title;
  }

  private refreshToolbarState(): void {
    if (!this.contentLoaded) {
      return;
    }

    const block = this.currentBlockName();
    this.activeBlock.set(block);

    const state: Record<string, boolean> = {};
    for (const tool of this.inlineTools) {
      try {
        // Headings are bold by their own styling, so reporting bold as active
        // inside one would be misleading rather than useful.
        const suppressed = tool.command === 'bold' && block.startsWith('h');
        state[tool.command] = !suppressed && document.queryCommandState(tool.command);
      } catch {
        state[tool.command] = false;
      }
    }
    for (const tool of this.listTools) {
      try {
        state[tool.command] = document.queryCommandState(tool.command);
      } catch {
        state[tool.command] = false;
      }
    }
    this.activeInline.set(state);
  }

  private currentBlockName(): string {
    const node = this.selectedNode();
    const host = this.surfaceEl;
    if (!node || !host) {
      return 'p';
    }

    let element: HTMLElement | null =
      node.nodeType === Node.ELEMENT_NODE ? (node as HTMLElement) : node.parentElement;

    while (element && element !== host) {
      const tag = element.tagName.toLowerCase();
      if (tag === 'h1' || tag === 'h2' || tag === 'h3') {
        return tag;
      }
      if (tag === 'p' || tag === 'li' || tag === 'div') {
        return tag === 'li' ? 'li' : 'p';
      }
      element = element.parentElement;
    }
    return 'p';
  }

  private isInsideList(): boolean {
    const node = this.selectedNode();
    const host = this.surfaceEl;
    let element: HTMLElement | null =
      node && node.nodeType === Node.ELEMENT_NODE
        ? (node as HTMLElement)
        : (node?.parentElement ?? null);

    while (element && element !== host) {
      if (element.tagName === 'LI') {
        return true;
      }
      element = element.parentElement;
    }
    return false;
  }

  private selectedNode(): Node | null {
    const selection = window.getSelection();
    if (!selection || selection.rangeCount === 0) {
      return null;
    }
    const node = selection.getRangeAt(0).startContainer;
    // Ignore a selection that sits outside this editor.
    return this.surfaceEl?.contains(node) ? node : null;
  }

  private updateCounts(): void {
    const text = this.surfaceEl?.innerText ?? '';
    this.words.set(countWords(text));
    this.characters.set(text.replace(/\s+$/g, '').length);
    this.isEmpty.set(text.trim().length === 0);
  }

  private saveContent(): void {
    const host = this.surfaceEl;
    if (!host || !this.canEdit) {
      return;
    }
    this.documents.update(this.documentId, { contentHtml: host.innerHTML }).subscribe({
      next: () => {
        this.dirty = false;
        this.saveState.set('saved');
      },
      error: (err) => {
        this.saveState.set('error');
        this.error.set(messageFrom(err));
      },
    });
  }

  saveTitle(): void {
    const doc = this.doc();
    const trimmed = this.title.trim();
    if (!doc || !this.canEdit || trimmed === doc.title) {
      return;
    }
    this.dirty = true;
    this.saveState.set('saving');
    this.documents.update(this.documentId, { title: trimmed }).subscribe({
      next: (updated) => {
        this.doc.set({ ...doc, title: updated.title });
        this.title = updated.title;
        this.pageTitle.setTitle(`${updated.title} - Docs`);
        this.saveState.set('saved');
      },
      error: (err) => {
        this.title = doc.title;
        this.saveState.set('error');
        this.error.set(messageFrom(err));
      },
    });
  }

  /** Called after a version is restored, so the surface shows the restored text. */
  reloadAfterRestore(): void {
    this.contentLoaded = false;
    this.dirty = false;
    this.documents.get(this.documentId).subscribe({
      next: (doc) => {
        this.doc.set(doc);
        this.title = doc.title;
        this.pageTitle.setTitle(`${doc.title} - Docs`);
        if (this.surfaceEl) {
          this.surfaceEl.innerHTML = doc.contentHtml || '<p><br></p>';
          this.contentLoaded = true;
          this.updateCounts();
        }
        this.saveState.set('saved');
      },
      error: (err) => this.error.set(messageFrom(err)),
    });
  }

  openPanel(tab: PanelTab): void {
    if (this.showPanel() && this.panelTab() === tab) {
      this.showPanel.set(false);
      return;
    }
    this.panelTab.set(tab);
    this.showPanel.set(true);
  }

  backToList(): void {
    this.router.navigate(['/documents']);
  }

  dismissError(): void {
    this.error.set(null);
  }

  get saveLabel(): string {
    switch (this.saveState()) {
      case 'saving':
        return 'Saving...';
      case 'saved':
        return 'All changes saved';
      case 'error':
        return 'Not saved';
      default:
        return '';
    }
  }
}
