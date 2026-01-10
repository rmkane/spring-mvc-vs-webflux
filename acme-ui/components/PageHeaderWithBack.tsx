'use client';

import Link from 'next/link';

import { ThemeToggle } from '@/components/ThemeToggle';

interface PageHeaderWithBackProps {
  title: string;
  backHref: string;
  backLabel?: string;
}

export function PageHeaderWithBack({
  title,
  backHref,
  backLabel = '← Back to Books',
}: PageHeaderWithBackProps) {
  return (
    <>
      <div className="mb-6 flex justify-between items-center">
        <Link href={backHref} className="text-blue-600 dark:text-blue-400 hover:underline">
          {backLabel}
        </Link>
        <ThemeToggle />
      </div>
      <h1 className="text-3xl font-bold text-black dark:text-zinc-50 mb-8">{title}</h1>
    </>
  );
}
