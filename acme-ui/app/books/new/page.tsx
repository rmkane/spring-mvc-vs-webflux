import { BookForm } from '@/components/BookForm'
import { PageHeaderWithBack } from '@/components/PageHeaderWithBack'

export default function NewBookPage() {
  return (
    <div className="min-h-screen bg-zinc-50 px-4 py-8 dark:bg-black">
      <div className="mx-auto max-w-2xl">
        <PageHeaderWithBack title="Create New Book" backHref="/books" />
        <BookForm />
      </div>
    </div>
  )
}
