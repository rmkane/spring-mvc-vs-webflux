'use client';

import { useRouter } from 'next/navigation';
import { useState } from 'react';

import type { Book, CreateBookRequest, UpdateBookRequest } from '@/lib/types';

interface BookFormProps {
  book?: Book;
}

export function BookForm({ book }: BookFormProps) {
  const router = useRouter();
  const isEditing = !!book;
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [formData, setFormData] = useState<CreateBookRequest | UpdateBookRequest>({
    title: book?.title || '',
    author: book?.author || '',
    isbn: book?.isbn || '',
    publicationYear: book?.publicationYear || new Date().getFullYear(),
  });

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    setLoading(true);
    setError(null);

    try {
      const url = isEditing ? `/api/books/${book.id}` : '/api/books';
      const method = isEditing ? 'PUT' : 'POST';

      const response = await fetch(url, {
        method,
        headers: {
          'Content-Type': 'application/json',
        },
        body: JSON.stringify(formData),
      });

      if (!response.ok) {
        const errorData = await response.json().catch(() => ({ message: 'Unknown error' }));
        throw new Error(errorData.message || `Failed to ${isEditing ? 'update' : 'create'} book`);
      }

      router.push('/books');
      router.refresh();
    } catch (err) {
      setError(err instanceof Error ? err.message : 'An error occurred');
      setLoading(false);
    }
  };

  const handleChange = (e: React.ChangeEvent<HTMLInputElement>) => {
    const { name, value } = e.target;
    setFormData((prev) => ({
      ...prev,
      [name]: name === 'publicationYear' ? parseInt(value, 10) || 0 : value,
    }));
  };

  return (
    <form onSubmit={handleSubmit} className="bg-white dark:bg-zinc-900 rounded-lg shadow-md p-6">
      {error && (
        <div className="mb-4 p-4 bg-red-100 dark:bg-red-900/30 border border-red-400 dark:border-red-700 text-red-700 dark:text-red-300 rounded">
          {error}
        </div>
      )}

      <div className="space-y-4">
        <div>
          <label
            htmlFor="title"
            className="block text-sm font-medium text-black dark:text-zinc-50 mb-1"
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
            className="w-full px-3 py-2 border border-zinc-300 dark:border-zinc-700 rounded-lg bg-white dark:bg-zinc-800 text-black dark:text-zinc-50 focus:outline-none focus:ring-2 focus:ring-blue-500"
          />
        </div>

        <div>
          <label
            htmlFor="author"
            className="block text-sm font-medium text-black dark:text-zinc-50 mb-1"
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
            className="w-full px-3 py-2 border border-zinc-300 dark:border-zinc-700 rounded-lg bg-white dark:bg-zinc-800 text-black dark:text-zinc-50 focus:outline-none focus:ring-2 focus:ring-blue-500"
          />
        </div>

        <div>
          <label
            htmlFor="isbn"
            className="block text-sm font-medium text-black dark:text-zinc-50 mb-1"
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
            className="w-full px-3 py-2 border border-zinc-300 dark:border-zinc-700 rounded-lg bg-white dark:bg-zinc-800 text-black dark:text-zinc-50 focus:outline-none focus:ring-2 focus:ring-blue-500"
          />
        </div>

        <div>
          <label
            htmlFor="publicationYear"
            className="block text-sm font-medium text-black dark:text-zinc-50 mb-1"
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
            className="w-full px-3 py-2 border border-zinc-300 dark:border-zinc-700 rounded-lg bg-white dark:bg-zinc-800 text-black dark:text-zinc-50 focus:outline-none focus:ring-2 focus:ring-blue-500"
          />
        </div>
      </div>

      <div className="mt-6 flex gap-4">
        <button
          type="submit"
          disabled={loading}
          className="flex-1 px-4 py-2 bg-black text-white rounded-lg hover:bg-zinc-800 dark:bg-zinc-50 dark:text-black dark:hover:bg-zinc-200 transition-colors disabled:opacity-50 disabled:cursor-not-allowed"
        >
          {loading ? 'Saving...' : isEditing ? 'Update Book' : 'Create Book'}
        </button>
        <button
          type="button"
          onClick={() => router.push('/books')}
          className="px-4 py-2 border border-zinc-300 dark:border-zinc-700 text-black dark:text-zinc-50 rounded-lg hover:bg-zinc-100 dark:hover:bg-zinc-800 transition-colors"
        >
          Cancel
        </button>
      </div>
    </form>
  );
}
