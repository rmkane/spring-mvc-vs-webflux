import Link from 'next/link'

import { Alert } from '@/components/Alert'
import { BookCard } from '@/components/BookCard'
import { PageHeader } from '@/components/PageHeader'
import { getAllBooks } from '@/lib/books'
import { compareBookTitles } from '@/lib/sort'
import type { Book } from '@/lib/types'

export default async function BooksPage() {
  let books: Book[] = []
  let error: string | null = null

  try {
    books = await getAllBooks()
    // Sort books by title, ignoring leading articles (The, A, An)
    // This follows standard library/bookstore sorting conventions
    books.sort((a, b) => compareBookTitles(a.title, b.title))
  } catch (err) {
    error = err instanceof Error ? err.message : 'Failed to load books'
  }

  return (
    <div className="min-h-screen bg-zinc-50 px-4 py-8 dark:bg-black">
      <div className="mx-auto max-w-6xl">
        <PageHeader title="Books" action={{ label: 'Add New Book', href: '/books/new' }} />

        {error && <Alert type="error" message={error} className="mb-4" />}

        {books.length === 0 && !error ? (
          <div className="py-12 text-center text-zinc-600 dark:text-zinc-400">
            <p className="mb-4 text-lg">No books found.</p>
            <Link
              href="/books/new"
              className="text-blue-600 hover:underline dark:text-blue-400"
              aria-label="Create your first book"
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
  )
}
