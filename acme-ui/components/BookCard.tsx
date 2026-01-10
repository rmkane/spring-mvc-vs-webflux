'use client';

import Link from 'next/link';
import { useRouter } from 'next/navigation';

import type { Book } from '@/lib/types';

interface BookCardProps {
  book: Book;
}

export function BookCard({ book }: BookCardProps) {
  const router = useRouter();

  async function handleDelete() {
    if (!confirm('Are you sure you want to delete this book?')) {
      return;
    }

    try {
      const response = await fetch(`/api/books/${book.id}`, {
        method: 'DELETE',
      });

      if (!response.ok) {
        throw new Error('Failed to delete book');
      }

      router.refresh();
    } catch {
      alert('Failed to delete book');
    }
  }

  return (
    <div className="bg-white dark:bg-zinc-900 rounded-lg shadow-md p-6 hover:shadow-lg transition-shadow">
      <Link href={`/books/${book.id}`}>
        <h2 className="text-xl font-semibold text-black dark:text-zinc-50 mb-2 hover:text-blue-600 dark:hover:text-blue-400">
          {book.title}
        </h2>
      </Link>
      <p className="text-zinc-600 dark:text-zinc-400 mb-2">
        <span className="font-medium">Author:</span> {book.author}
      </p>
      <p className="text-zinc-600 dark:text-zinc-400 mb-2">
        <span className="font-medium">ISBN:</span> {book.isbn}
      </p>
      <p className="text-zinc-600 dark:text-zinc-400 mb-4">
        <span className="font-medium">Year:</span> {book.publicationYear}
      </p>
      <div className="flex gap-2">
        <Link
          href={`/books/${book.id}`}
          className="flex-1 text-center px-3 py-2 bg-zinc-100 dark:bg-zinc-800 text-black dark:text-zinc-50 rounded hover:bg-zinc-200 dark:hover:bg-zinc-700 transition-colors"
        >
          Edit
        </Link>
        <button
          onClick={handleDelete}
          className="flex-1 px-3 py-2 bg-red-100 dark:bg-red-900/30 text-red-700 dark:text-red-300 rounded hover:bg-red-200 dark:hover:bg-red-900/50 transition-colors"
        >
          Delete
        </button>
      </div>
    </div>
  );
}
