'use client'

import Link from 'next/link'

import { ThemeToggle } from '@/components/ThemeToggle'

interface PageHeaderWithBackProps {
  title: string
  backHref: string
  backLabel?: string
}

export function PageHeaderWithBack({
  title,
  backHref,
  backLabel = '← Back to Books',
}: PageHeaderWithBackProps) {
  return (
    <>
      <div className="mb-6 flex items-center justify-between">
        <Link href={backHref} className="text-blue-600 hover:underline dark:text-blue-400">
          {backLabel}
        </Link>
        <ThemeToggle />
      </div>
      <h1 className="mb-8 text-3xl font-bold text-black dark:text-zinc-50">{title}</h1>
    </>
  )
}
