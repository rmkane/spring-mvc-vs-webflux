/**
 * Utility functions for sorting books
 */

/**
 * Removes leading articles (The, A, An) from a title for sorting purposes.
 * This follows standard library/bookstore sorting conventions.
 *
 * @param title - The book title
 * @returns The title with leading articles removed, trimmed
 */
function getSortTitle(title: string): string {
  const trimmed = title.trim();

  // Common leading articles to ignore (case-insensitive)
  const articles = ['the ', 'a ', 'an '];

  for (const article of articles) {
    if (trimmed.toLowerCase().startsWith(article)) {
      return trimmed.slice(article.length).trim();
    }
  }

  return trimmed;
}

/**
 * Compares two book titles for sorting, ignoring leading articles.
 * This is the standard way books are sorted in libraries and bookstores.
 *
 * @param titleA - First title
 * @param titleB - Second title
 * @returns Comparison result for use with Array.sort()
 */
export function compareBookTitles(titleA: string, titleB: string): number {
  const sortTitleA = getSortTitle(titleA);
  const sortTitleB = getSortTitle(titleB);

  return sortTitleA.localeCompare(sortTitleB, undefined, {
    sensitivity: 'base', // Case-insensitive, ignore accents
    numeric: true, // Handle numbers naturally (e.g., "Book 2" comes before "Book 10")
  });
}
