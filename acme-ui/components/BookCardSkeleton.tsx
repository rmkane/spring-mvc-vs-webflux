export function BookCardSkeleton() {
  return (
    <div className="animate-pulse rounded-lg bg-white p-6 shadow-md dark:bg-zinc-900">
      <div className="mb-2 h-6 w-3/4 rounded bg-zinc-200 dark:bg-zinc-700"></div>
      <div className="mb-2 h-4 w-1/2 rounded bg-zinc-200 dark:bg-zinc-700"></div>
      <div className="mb-2 h-4 w-2/3 rounded bg-zinc-200 dark:bg-zinc-700"></div>
      <div className="mb-4 h-4 w-1/4 rounded bg-zinc-200 dark:bg-zinc-700"></div>
      <div className="flex gap-2">
        <div className="h-10 flex-1 rounded bg-zinc-200 dark:bg-zinc-700"></div>
        <div className="h-10 flex-1 rounded bg-zinc-200 dark:bg-zinc-700"></div>
      </div>
    </div>
  )
}
