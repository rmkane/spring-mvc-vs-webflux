import { compareBookTitles } from '../sort';

describe('compareBookTitles', () => {
  it('should sort books alphabetically ignoring leading articles', () => {
    const titles = [
      'The Great Gatsby',
      'A Tale of Two Cities',
      '1984',
      'An Introduction to Programming',
      'Brave New World',
    ];

    const sorted = titles.toSorted(compareBookTitles);

    expect(sorted).toEqual([
      '1984',
      'Brave New World',
      'The Great Gatsby',
      'An Introduction to Programming',
      'A Tale of Two Cities',
    ]);
  });

  it('should handle titles without leading articles', () => {
    expect(compareBookTitles('1984', 'Brave New World')).toBeLessThan(0);
    expect(compareBookTitles('Brave New World', '1984')).toBeGreaterThan(0);
  });

  it('should be case-insensitive', () => {
    expect(compareBookTitles('the great gatsby', 'THE GREAT GATSBY')).toBe(0);
    expect(compareBookTitles('A Tale', 'a tale')).toBe(0);
  });

  it('should handle "The" article', () => {
    expect(compareBookTitles('The Great Gatsby', 'Great Expectations')).toBeGreaterThan(0);
    expect(compareBookTitles('Great Expectations', 'The Great Gatsby')).toBeLessThan(0);
  });

  it('should handle "A" article', () => {
    // "A Tale of Two Cities" -> "Tale of Two Cities"
    // "Tale of Three Cities" -> "Tale of Three Cities"
    // "Tale of Two" > "Tale of Three", so A Tale should come after
    expect(compareBookTitles('A Tale of Two Cities', 'Tale of Three Cities')).toBeGreaterThan(0);
  });

  it('should handle "An" article', () => {
    // "An Introduction" -> "Introduction"
    // "Introduction to Programming" -> "Introduction to Programming"
    // "Introduction" < "Introduction to Programming", so An Introduction should come first
    expect(compareBookTitles('An Introduction', 'Introduction to Programming')).toBeLessThan(0);
  });

  it('should handle numeric sorting', () => {
    const titles = ['Book 10', 'Book 2', 'Book 1'];
    const sorted = titles.toSorted(compareBookTitles);
    expect(sorted).toEqual(['Book 1', 'Book 2', 'Book 10']);
  });
});
