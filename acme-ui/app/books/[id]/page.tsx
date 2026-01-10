import Link from 'next/link'
import { notFound } from 'next/navigation'

import { BookForm } from '@/components/BookForm'
import { PageHeaderWithBack } from '@/components/PageHeaderWithBack'
import { getBookById } from '@/lib/books'
import type { Book } from '@/lib/types'

interface PageProps {
  params: Promise<{ id: string }>
}

export default async function BookEditPage({ params }: PageProps) {
  const { id } = await params
  const bookId = parseInt(id, 10)

  if (isNaN(bookId)) {
    notFound()
  }

  let book: Book | null = null
  let error: string | null = null

  try {
    book = await getBookById(bookId)
  } catch (err) {
    if (err instanceof Error && err.message.includes('404')) {
      notFound()
    }
    error = err instanceof Error ? err.message : 'Failed to load book'
  }

  if (error) {
    return (
      <div className="min-h-screen bg-zinc-50 dark:bg-black py-8 px-4">
        <div className="max-w-2xl mx-auto">
          <div className="mb-4 flex justify-between items-center">
            <Link href="/books" className="text-blue-600 dark:text-blue-400 hover:underline">
              ← Back to Books
            </Link>
          </div>
          <div className="p-4 bg-red-100 dark:bg-red-900/30 border border-red-400 dark:border-red-700 text-red-700 dark:text-red-300 rounded">
            Error: {error}
          </div>
        </div>
      </div>
    )
  }

  if (!book) {
    notFound()
  }

  return (
    <div className="min-h-screen bg-zinc-50 dark:bg-black py-8 px-4">
      <div className="max-w-2xl mx-auto">
        <PageHeaderWithBack title="Edit Book" backHref="/books" />
        <BookForm book={book} />
      </div>
    </div>
  )
}
