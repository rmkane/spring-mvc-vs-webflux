import Link from 'next/link';
import { getAllBooks } from '@/lib/books';
import { compareBookTitles } from '@/lib/sort';
import BookCard from '@/components/BookCard';
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
        <div className="flex justify-between items-center mb-8">
          <h1 className="text-3xl font-bold text-black dark:text-zinc-50">
            Books
          </h1>
          <Link
            href="/books/new"
            className="px-4 py-2 bg-black text-white rounded-lg hover:bg-zinc-800 dark:bg-zinc-50 dark:text-black dark:hover:bg-zinc-200 transition-colors"
          >
            Add New Book
          </Link>
        </div>

        {error && (
          <div className="mb-4 p-4 bg-red-100 dark:bg-red-900/30 border border-red-400 dark:border-red-700 text-red-700 dark:text-red-300 rounded">
            Error: {error}
          </div>
        )}

        {books.length === 0 && !error ? (
          <div className="text-center py-12 text-zinc-600 dark:text-zinc-400">
            <p className="text-lg mb-4">No books found.</p>
            <Link
              href="/books/new"
              className="text-blue-600 dark:text-blue-400 hover:underline"
            >
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
