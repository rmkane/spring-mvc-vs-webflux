import Link from 'next/link';

import { BookForm } from '@/components/BookForm';

export default function NewBookPage() {
  return (
    <div className="min-h-screen bg-zinc-50 dark:bg-black py-8 px-4">
      <div className="max-w-2xl mx-auto">
        <div className="mb-6">
          <Link href="/books" className="text-blue-600 dark:text-blue-400 hover:underline">
            ← Back to Books
          </Link>
        </div>
        <h1 className="text-3xl font-bold text-black dark:text-zinc-50 mb-8">Create New Book</h1>
        <BookForm />
      </div>
    </div>
  );
}
