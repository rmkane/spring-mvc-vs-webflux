import Link from 'next/link';

import { BookCard } from '@/components/BookCard';
import { PageHeader } from '@/components/PageHeader';
import { getAllBooks } from '@/lib/books';
import { compareBookTitles } from '@/lib/sort';
import type { Book } from '@/lib/types';

export default async function BooksPage() {
  let books: Book[] = [];
  let error: string | null = null;

  try {
    books = await getAllBooks();
    // Sort books by title, ignoring leading articles (The, A, An)
    // This follows standard library/bookstore sorting conventions
    books.sort((a, b) => compareBookTitles(a.title, b.title));
  } catch (err) {
    error = err instanceof Error ? err.message : 'Failed to load books';
  }

  return (
    <div className="min-h-screen bg-zinc-50 dark:bg-black py-8 px-4">
      <div className="max-w-6xl mx-auto">
        <PageHeader title="Books" action={{ label: 'Add New Book', href: '/books/new' }} />

        {error && (
          <div className="mb-4 p-4 bg-red-100 dark:bg-red-900/30 border border-red-400 dark:border-red-700 text-red-700 dark:text-red-300 rounded">
            Error: {error}
          </div>
        )}

        {books.length === 0 && !error ? (
          <div className="text-center py-12 text-zinc-600 dark:text-zinc-400">
            <p className="text-lg mb-4">No books found.</p>
            <Link href="/books/new" className="text-blue-600 dark:text-blue-400 hover:underline">
              Create your first book
            </Link>
          </div>
        ) : (
          <div className="grid gap-4 md:grid-cols-2 lg:grid-cols-3">
            {books.map((book) => (
              <BookCard key={book.id} book={book} />
            ))}
          </div>
        )}
      </div>
    </div>
  );
}
