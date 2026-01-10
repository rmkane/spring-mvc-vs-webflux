import Link from 'next/link'
import { notFound } from 'next/navigation'

import { Alert } from '@/components/Alert'
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
      <div className="min-h-screen bg-zinc-50 px-4 py-8 dark:bg-black">
        <div className="mx-auto max-w-2xl">
          <div className="mb-4 flex items-center justify-between">
            <Link href="/books" className="text-blue-600 hover:underline dark:text-blue-400">
              ← Back to Books
            </Link>
          </div>
          <Alert type="error" message={error} />
        </div>
      </div>
    )
  }

  if (!book) {
    notFound()
  }

  return (
    <div className="min-h-screen bg-zinc-50 px-4 py-8 dark:bg-black">
      <div className="mx-auto max-w-2xl">
        <PageHeaderWithBack title="Edit Book" backHref="/books" />
        <BookForm book={book} />
      </div>
    </div>
  )
}
