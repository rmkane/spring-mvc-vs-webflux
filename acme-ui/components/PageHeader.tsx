'use client'

import Link from 'next/link'

import { ThemeToggle } from '@/components/ThemeToggle'

interface PageHeaderProps {
  title: string
  action?: {
    label: string
    href: string
  }
}

export function PageHeader({ title, action }: PageHeaderProps) {
  return (
    <div className="mb-8 flex items-center justify-between">
      <h1 className="text-3xl font-bold text-black dark:text-zinc-50">{title}</h1>
      <div className="flex items-center gap-3">
        {action && (
          <Link
            href={action.href}
            className="rounded-lg bg-black px-4 py-2 text-white transition-colors hover:bg-zinc-800 dark:bg-zinc-50 dark:text-black dark:hover:bg-zinc-200"
          >
            {action.label}
          </Link>
        )}
        <ThemeToggle />
      </div>
    </div>
  )
}
