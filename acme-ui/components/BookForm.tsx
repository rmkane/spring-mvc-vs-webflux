'use client'

import { useRouter } from 'next/navigation'
import { useState } from 'react'

import { Alert } from '@/components/Alert'
import { LoadingSpinner } from '@/components/LoadingSpinner'
import { useToastContext } from '@/components/ToastProvider'
import type { Book, CreateBookRequest, UpdateBookRequest } from '@/lib/types'

interface BookFormProps {
  book?: Book
}

export function BookForm({ book }: BookFormProps) {
  const router = useRouter()
  const { showSuccess, showError: showToastError } = useToastContext()
  const isEditing = !!book
  const [loading, setLoading] = useState(false)
  const [error, setError] = useState<string | null>(null)
  const [formData, setFormData] = useState<CreateBookRequest | UpdateBookRequest>({
    title: book?.title || '',
    author: book?.author || '',
    isbn: book?.isbn || '',
    publicationYear: book?.publicationYear || new Date().getFullYear(),
  })

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault()
    setLoading(true)
    setError(null)

    try {
      const url = isEditing ? `/api/books/${book.id}` : '/api/books'
      const method = isEditing ? 'PUT' : 'POST'

      const response = await fetch(url, {
        method,
        headers: {
          'Content-Type': 'application/json',
        },
        body: JSON.stringify(formData),
      })

      if (!response.ok) {
        const errorData = await response.json().catch(() => ({ message: 'Unknown error' }))
        throw new Error(errorData.message || `Failed to ${isEditing ? 'update' : 'create'} book`)
      }

      showSuccess(`Book ${isEditing ? 'updated' : 'created'} successfully`)
      router.push('/books')
      router.refresh()
    } catch (err) {
      const errorMessage = err instanceof Error ? err.message : 'An error occurred'
      setError(errorMessage)
      showToastError(errorMessage)
      setLoading(false)
    }
  }

  const handleChange = (e: React.ChangeEvent<HTMLInputElement>) => {
    const { name, value } = e.target
    setFormData((prev) => ({
      ...prev,
      [name]: name === 'publicationYear' ? parseInt(value, 10) || 0 : value,
    }))
  }

  return (
    <form onSubmit={handleSubmit} className="rounded-lg bg-white p-6 shadow-md dark:bg-zinc-900">
      {error && <Alert type="error" message={error} className="mb-4" />}

      <div className="space-y-4">
        <div>
          <label
            htmlFor="title"
            className="mb-1 block text-sm font-medium text-black dark:text-zinc-50"
          >
            Title *
          </label>
          <input
            type="text"
            id="title"
            name="title"
            required
            value={formData.title}
            onChange={handleChange}
            className="w-full rounded-lg border border-zinc-300 bg-white px-3 py-2 text-black focus:ring-2 focus:ring-blue-500 focus:outline-none dark:border-zinc-700 dark:bg-zinc-800 dark:text-zinc-50"
          />
        </div>

        <div>
          <label
            htmlFor="author"
            className="mb-1 block text-sm font-medium text-black dark:text-zinc-50"
          >
            Author *
          </label>
          <input
            type="text"
            id="author"
            name="author"
            required
            value={formData.author}
            onChange={handleChange}
            className="w-full rounded-lg border border-zinc-300 bg-white px-3 py-2 text-black focus:ring-2 focus:ring-blue-500 focus:outline-none dark:border-zinc-700 dark:bg-zinc-800 dark:text-zinc-50"
          />
        </div>

        <div>
          <label
            htmlFor="isbn"
            className="mb-1 block text-sm font-medium text-black dark:text-zinc-50"
          >
            ISBN *
          </label>
          <input
            type="text"
            id="isbn"
            name="isbn"
            required
            value={formData.isbn}
            onChange={handleChange}
            className="w-full rounded-lg border border-zinc-300 bg-white px-3 py-2 text-black focus:ring-2 focus:ring-blue-500 focus:outline-none dark:border-zinc-700 dark:bg-zinc-800 dark:text-zinc-50"
          />
        </div>

        <div>
          <label
            htmlFor="publicationYear"
            className="mb-1 block text-sm font-medium text-black dark:text-zinc-50"
          >
            Publication Year *
          </label>
          <input
            type="number"
            id="publicationYear"
            name="publicationYear"
            required
            min="1000"
            max={new Date().getFullYear() + 1}
            value={formData.publicationYear}
            onChange={handleChange}
            className="w-full rounded-lg border border-zinc-300 bg-white px-3 py-2 text-black focus:ring-2 focus:ring-blue-500 focus:outline-none dark:border-zinc-700 dark:bg-zinc-800 dark:text-zinc-50"
          />
        </div>
      </div>

      <div className="mt-6 flex gap-4">
        <button
          type="submit"
          disabled={loading}
          className="flex flex-1 items-center justify-center gap-2 rounded-lg bg-black px-4 py-2 text-white transition-colors hover:bg-zinc-800 disabled:cursor-not-allowed disabled:opacity-50 dark:bg-zinc-50 dark:text-black dark:hover:bg-zinc-200"
        >
          {loading && <LoadingSpinner size="sm" />}
          {loading ? 'Saving...' : isEditing ? 'Update Book' : 'Create Book'}
        </button>
        <button
          type="button"
          onClick={() => router.push('/books')}
          className="rounded-lg border border-zinc-300 px-4 py-2 text-black transition-colors hover:bg-zinc-100 dark:border-zinc-700 dark:text-zinc-50 dark:hover:bg-zinc-800"
        >
          Cancel
        </button>
      </div>
    </form>
  )
}
