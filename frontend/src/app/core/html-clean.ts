/**
 * Client side cleanup for pasted content.
 *
 * The server sanitizes everything before it is stored, and that is the check
 * that actually protects the data. This exists for a different reason: without
 * it, a paste from Word or a web page shows its colours and fonts on screen,
 * then quietly loses them on the next reload once the server has stripped them.
 * Cleaning on paste means what you see is what gets saved.
 *
 * ALLOWED_TAGS is deliberately the same set as the server allowlist in
 * HtmlSanitizer.java. If one changes the other has to change with it.
 */
const ALLOWED_TAGS = new Set([
  'P', 'BR', 'DIV', 'SPAN',
  'B', 'STRONG', 'I', 'EM', 'U',
  'H1', 'H2', 'H3',
  'UL', 'OL', 'LI',
  'BLOCKQUOTE', 'CODE', 'PRE', 'A',
]);

/** Headings below h3 collapse rather than disappear, so structure survives. */
const TAG_REPLACEMENTS: Record<string, string> = {
  H4: 'H3',
  H5: 'H3',
  H6: 'H3',
};

export function cleanPastedHtml(html: string): string {
  const parsed = new DOMParser().parseFromString(html, 'text/html');
  const output = document.createElement('div');

  for (const node of Array.from(parsed.body.childNodes)) {
    const cleaned = cleanNode(node);
    if (cleaned) {
      output.appendChild(cleaned);
    }
  }

  return output.innerHTML;
}

function cleanNode(node: Node): Node | null {
  if (node.nodeType === Node.TEXT_NODE) {
    return document.createTextNode(node.textContent ?? '');
  }

  if (node.nodeType !== Node.ELEMENT_NODE) {
    return null;
  }

  const element = node as HTMLElement;
  const tag = TAG_REPLACEMENTS[element.tagName] ?? element.tagName;

  // Scripts and styles are dropped along with everything inside them.
  if (tag === 'SCRIPT' || tag === 'STYLE' || tag === 'LINK' || tag === 'META') {
    return null;
  }

  const children = Array.from(element.childNodes)
    .map(cleanNode)
    .filter((child): child is Node => child !== null);

  if (!ALLOWED_TAGS.has(tag)) {
    // Keep the text of an unsupported wrapper, drop the wrapper itself.
    const fragment = document.createDocumentFragment();
    children.forEach((child) => fragment.appendChild(child));
    return fragment;
  }

  const replacement = document.createElement(tag.toLowerCase());

  // Only links keep an attribute, and only a safe href.
  if (tag === 'A') {
    const href = element.getAttribute('href') ?? '';
    if (/^(https?:|mailto:)/i.test(href)) {
      replacement.setAttribute('href', href);
    }
  }

  children.forEach((child) => replacement.appendChild(child));
  return replacement;
}

/** Used when the clipboard only carries plain text. */
export function plainTextToHtml(text: string): string {
  const blocks = text.replace(/\r\n/g, '\n').split(/\n[ \t]*\n/);
  const paragraphs = blocks
    .filter((block) => block.trim().length > 0)
    .map((block) => {
      const paragraph = document.createElement('p');
      // Assigning textContent escapes the value, so nothing here is parsed
      // as markup.
      paragraph.textContent = block.trim();
      return paragraph.outerHTML.replace(/\n/g, '<br>');
    });

  return paragraphs.join('') || '<p></p>';
}

/** Words counted the way a person would count them, not by splitting on every space. */
export function countWords(text: string): number {
  const trimmed = text.replace(/\s+/g, ' ').trim();
  return trimmed.length === 0 ? 0 : trimmed.split(' ').length;
}
