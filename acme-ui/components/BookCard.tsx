'use client'

import Link from 'next/link'
import { useRouter } from 'next/navigation'
import { useState } from 'react'

import { ConfirmationDialog } from '@/components/ConfirmationDialog'
import { LoadingSpinner } from '@/components/LoadingSpinner'
import { useToastContext } from '@/components/ToastProvider'
import type { Book } from '@/lib/types'

interface BookCardProps {
  book: Book
}

export function BookCard({ book }: BookCardProps) {
  const router = useRouter()
  const { showSuccess, showError } = useToastContext()
  const [showConfirm, setShowConfirm] = useState(false)
  const [isDeleting, setIsDeleting] = useState(false)

  async function handleDelete() {
    setIsDeleting(true)
    try {
      const response = await fetch(`/api/books/${book.id}`, {
        method: 'DELETE',
      })

      if (!response.ok) {
        throw new Error('Failed to delete book')
      }

      showSuccess(`"${book.title}" has been deleted`)
      router.refresh()
    } catch {
      showError('Failed to delete book. Please try again.')
      setIsDeleting(false)
      setShowConfirm(false)
    }
  }

  return (
    <div className="rounded-lg bg-white p-6 shadow-md transition-shadow hover:shadow-lg dark:bg-zinc-900">
      <Link href={`/books/${book.id}`}>
        <h2 className="mb-2 text-xl font-semibold text-black hover:text-blue-600 dark:text-zinc-50 dark:hover:text-blue-400">
          {book.title}
        </h2>
      </Link>
      <p className="mb-2 text-zinc-600 dark:text-zinc-400">
        <span className="font-medium">Author:</span> {book.author}
      </p>
      <p className="mb-2 text-zinc-600 dark:text-zinc-400">
        <span className="font-medium">ISBN:</span> {book.isbn}
      </p>
      <p className="mb-4 text-zinc-600 dark:text-zinc-400">
        <span className="font-medium">Year:</span> {book.publicationYear}
      </p>
      <div className="flex gap-2">
        <Link
          href={`/books/${book.id}`}
          className="flex-1 rounded bg-zinc-100 px-3 py-2 text-center text-black transition-colors hover:bg-zinc-200 dark:bg-zinc-800 dark:text-zinc-50 dark:hover:bg-zinc-700"
        >
          Edit
        </Link>
        <button
          onClick={() => setShowConfirm(true)}
          disabled={isDeleting}
          className="flex flex-1 items-center justify-center gap-2 rounded bg-red-100 px-3 py-2 text-red-700 transition-colors hover:bg-red-200 disabled:cursor-not-allowed disabled:opacity-50 dark:bg-red-900/30 dark:text-red-300 dark:hover:bg-red-900/50"
          aria-label={`Delete ${book.title}`}
        >
          {isDeleting ? (
            <>
              <LoadingSpinner size="sm" />
              <span>Deleting...</span>
            </>
          ) : (
            'Delete'
          )}
        </button>
      </div>

      <ConfirmationDialog
        isOpen={showConfirm}
        title="Delete Book"
        message={`Are you sure you want to delete "${book.title}"? This action cannot be undone.`}
        confirmLabel="Delete"
        cancelLabel="Cancel"
        variant="danger"
        onConfirm={handleDelete}
        onCancel={() => setShowConfirm(false)}
      />
    </div>
  )
}
