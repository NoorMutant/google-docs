import { exactTime, relativeTime } from './relative-time';

describe('relativeTime', () => {
  const now = new Date('2026-08-30T12:00:00Z').getTime();

  function ago(minutes: number): string {
    return new Date(now - minutes * 60000).toISOString();
  }

  it('says just now for anything under a minute', () => {
    expect(relativeTime(ago(0), now)).toBe('just now');
    expect(relativeTime(ago(0.5), now)).toBe('just now');
  });

  it('counts minutes up to an hour', () => {
    expect(relativeTime(ago(1), now)).toBe('1 min ago');
    expect(relativeTime(ago(59), now)).toBe('59 min ago');
  });

  it('switches to hours after an hour', () => {
    expect(relativeTime(ago(60), now)).toBe('1 h ago');
    expect(relativeTime(ago(60 * 5), now)).toBe('5 h ago');
  });

  it('falls back to a date beyond a day', () => {
    const result = relativeTime(ago(60 * 30), now);

    expect(result).not.toContain('ago');
    expect(result.length).toBeGreaterThan(0);
  });

  it('returns an empty string rather than NaN for an unusable value', () => {
    expect(relativeTime('not a date', now)).toBe('');
    expect(exactTime('not a date')).toBe('');
  });

  it('handles a timestamp slightly in the future without going negative', () => {
    const future = new Date(now + 30000).toISOString();

    expect(relativeTime(future, now)).toBe('just now');
  });
});
