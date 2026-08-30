import { cleanPastedHtml, countWords, plainTextToHtml } from './html-clean';

describe('cleanPastedHtml', () => {
  it('keeps the formatting the editor supports', () => {
    const html = '<h1>Title</h1><p><strong>bold</strong> <em>italic</em></p><ul><li>one</li></ul>';

    expect(cleanPastedHtml(html)).toBe(html);
  });

  it('strips inline styles so the paste matches what the server will store', () => {
    const cleaned = cleanPastedHtml('<p style="color:red;font-size:40px">red</p>');

    expect(cleaned).toBe('<p>red</p>');
  });

  it('removes scripts along with their contents', () => {
    const cleaned = cleanPastedHtml('<p>safe</p><script>alert("x")</script>');

    expect(cleaned).toBe('<p>safe</p>');
    expect(cleaned).not.toContain('alert');
  });

  it('removes event handler attributes', () => {
    const cleaned = cleanPastedHtml('<p onclick="steal()">text</p>');

    expect(cleaned).toBe('<p>text</p>');
  });

  it('unwraps unsupported tags but keeps their text', () => {
    const cleaned = cleanPastedHtml('<table><tr><td>cell</td></tr></table>');

    expect(cleaned).toBe('cell');
  });

  it('collapses headings below h3 rather than dropping them', () => {
    expect(cleanPastedHtml('<h5>deep heading</h5>')).toBe('<h3>deep heading</h3>');
  });

  it('keeps http and mailto links', () => {
    expect(cleanPastedHtml('<a href="https://example.com">x</a>')).toBe(
      '<a href="https://example.com">x</a>'
    );
    expect(cleanPastedHtml('<a href="mailto:a@b.com">x</a>')).toBe(
      '<a href="mailto:a@b.com">x</a>'
    );
  });

  it('drops javascript and data urls from links but keeps the text', () => {
    expect(cleanPastedHtml('<a href="javascript:alert(1)">click</a>')).toBe('<a>click</a>');
    expect(cleanPastedHtml('<a href="data:text/html,<script>x</script>">click</a>')).toBe(
      '<a>click</a>'
    );
  });

  it('removes images, iframes and objects entirely', () => {
    expect(cleanPastedHtml('<p>before<img src="x.png"><iframe src="y"></iframe>after</p>')).toBe(
      '<p>beforeafter</p>'
    );
  });

  it('returns an empty string for empty input', () => {
    expect(cleanPastedHtml('')).toBe('');
  });
});

describe('plainTextToHtml', () => {
  it('splits blank line separated blocks into paragraphs', () => {
    expect(plainTextToHtml('one\n\ntwo')).toBe('<p>one</p><p>two</p>');
  });

  it('escapes markup rather than letting it through', () => {
    const html = plainTextToHtml('<script>alert(1)</script>');

    expect(html).not.toContain('<script>');
    expect(html).toContain('&lt;script&gt;');
  });

  it('returns a single empty paragraph for empty input', () => {
    expect(plainTextToHtml('')).toBe('<p></p>');
    expect(plainTextToHtml('   \n\n  ')).toBe('<p></p>');
  });
});

describe('countWords', () => {
  it('counts words separated by any amount of whitespace', () => {
    expect(countWords('one  two\nthree\t four')).toBe(4);
  });

  it('counts nothing for empty or whitespace only text', () => {
    expect(countWords('')).toBe(0);
    expect(countWords('   \n \t ')).toBe(0);
  });

  it('counts a single word', () => {
    expect(countWords(' word ')).toBe(1);
  });
});
