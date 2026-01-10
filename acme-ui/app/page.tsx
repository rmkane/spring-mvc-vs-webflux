import Link from 'next/link'

import { ThemeToggle } from '@/components/ThemeToggle'

export default function Home() {
  return (
    <div className="flex min-h-screen items-center justify-center bg-zinc-50 font-sans dark:bg-black">
      <main className="flex min-h-screen w-full max-w-3xl flex-col items-center justify-between bg-white px-16 py-32 sm:items-start dark:bg-black">
        <div className="absolute top-8 right-8">
          <ThemeToggle />
        </div>
        <div className="flex flex-col items-center gap-6 text-center sm:items-start sm:text-left">
          <h1 className="max-w-xs text-3xl leading-10 font-semibold tracking-tight text-black dark:text-zinc-50">
            Acme Book Management
          </h1>
          <p className="max-w-md text-lg leading-8 text-zinc-600 dark:text-zinc-400">
            Manage your book collection with our Spring Boot backend API.
          </p>
        </div>
        <div className="flex flex-col gap-4 text-base font-medium sm:flex-row">
          <Link
            href="/books"
            className="flex h-12 w-full items-center justify-center gap-2 rounded-full bg-black px-5 text-white transition-colors hover:bg-zinc-800 md:w-[158px] dark:bg-zinc-50 dark:text-black dark:hover:bg-zinc-200"
          >
            View Books
          </Link>
          <Link
            href="/books/new"
            className="flex h-12 w-full items-center justify-center rounded-full border border-solid border-black/8 px-5 transition-colors hover:border-transparent hover:bg-black/4 md:w-[158px] dark:border-white/14 dark:hover:bg-zinc-800"
          >
            Add Book
          </Link>
        </div>
      </main>
    </div>
  )
}
