import { BookForm } from '@/components/BookForm';
import { PageHeaderWithBack } from '@/components/PageHeaderWithBack';

export default function NewBookPage() {
  return (
    <div className="min-h-screen bg-zinc-50 dark:bg-black py-8 px-4">
      <div className="max-w-2xl mx-auto">
        <PageHeaderWithBack title="Create New Book" backHref="/books" />
        <BookForm />
      </div>
    </div>
  );
}
