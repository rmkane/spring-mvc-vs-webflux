# Acme UI

Next.js web application providing a user interface for book management.

## Purpose

This module provides a web-based user interface for managing books, built with Next.js and React. It communicates with the backend REST APIs (MVC or WebFlux) to perform CRUD operations on book resources.

## Key Features

- Book listing with alphabetical sorting (ignoring leading articles)
- Create, edit, and delete book operations
- Server-side rendering with Next.js App Router
- Automatic `x-dn` header injection for backend authentication
- Environment-based configuration for local development
- Responsive design with dark mode support
- TypeScript for type safety

## Port

Runs on **port 3001** (HTTP, development server).

## Technology Stack

- **Next.js 16.1** - React framework with App Router
- **React 19.2** - UI library
- **TypeScript 5** - Type-safe JavaScript
- **Tailwind CSS 4** - Utility-first CSS framework
- **pnpm** - Package manager

## Environment Configuration

For local development, create a `.env.local` file in the `acme-ui` directory:

```bash
# Local development DN (Distinguished Name)
# This DN will be automatically added as the x-dn header to all backend API requests
LOCAL_DN=cn=John Doe,ou=Engineering,ou=Users,dc=corp,dc=acme,dc=org

# Optional: Backend API base URL (defaults to http://localhost:8080)
# NEXT_PUBLIC_API_URL=http://localhost:8080
```

The `LOCAL_DN` environment variable is used by server-side API utilities to automatically include the `x-dn` header when making requests to the backend. This is necessary for local development since there's no ingress to provide authentication headers.

**Note:** The `LOCAL_DN` environment variable is only available on the server side (API routes, Server Components, Server Actions). It is not exposed to the client for security reasons.

## Running the Application

### Using Make

```bash
make run-ui
```

This will automatically install dependencies if needed and start the development server.

### Manual Start

```bash
cd acme-ui
pnpm install  # First time only
pnpm run dev
```

Open [http://localhost:3001](http://localhost:3001) in your browser.

## Development Scripts

### Linting

```bash
# Check for linting errors
pnpm run lint

# Fix auto-fixable linting errors
pnpm run lint:fix
```

### Formatting

```bash
# Format all code
pnpm run format

# Check if code is formatted (CI-friendly)
pnpm run format:check
```

### Testing

```bash
# Run tests once
pnpm run test

# Run tests in watch mode
pnpm run test:watch
```

Tests use Jest and React Testing Library. Test files should be placed in `__tests__` directories or have `.test.ts`/`.test.tsx` extensions.

## Project Structure

```text
acme-ui/
├── app/                    # Next.js App Router
│   ├── books/              # Book management pages
│   │   ├── page.tsx        # Book list page
│   │   ├── new/            # Create book page
│   │   └── [id]/           # Edit book page
│   ├── api/                # API routes (proxies to backend)
│   │   └── books/          # Book API endpoints
│   ├── layout.tsx          # Root layout
│   └── page.tsx            # Home page
├── components/             # React components
│   ├── BookCard.tsx        # Book card component
│   └── BookForm.tsx        # Book form component
├── lib/                    # Utility functions
│   ├── api.ts              # API client utilities
│   ├── books.ts            # Book-specific API functions
│   ├── sort.ts             # Book sorting utilities
│   └── types.ts            # TypeScript type definitions
└── public/                 # Static assets
```

## Making Backend API Requests

The project includes utility functions in `lib/api.ts` for making requests to the backend API. These utilities automatically include the `x-dn` header from the `LOCAL_DN` environment variable.

**Example usage in API routes or Server Components:**

```typescript
import { apiRequestJson } from '@/lib/api'

// In an API route or Server Component
const books = await apiRequestJson<Book[]>('/api/v1/books')
```

**Example API route:**

```typescript
// app/api/books/route.ts
import { NextResponse } from 'next/server'
import { createBook } from '@/lib/books'

export async function POST(request: Request) {
  const body = await request.json()
  const book = await createBook(body)
  return NextResponse.json(book, { status: 201 })
}
```

## Book Sorting

Books are sorted alphabetically by title, ignoring leading articles ("The", "A", "An") following standard library/bookstore conventions. This is implemented in `lib/sort.ts` using the `compareBookTitles()` function.

## Dependencies

- **Backend APIs** - Communicates with `acme-api-mvc` (port 8080) or `acme-api-webflux` (port 8081)
- **Authentication** - Uses `x-dn` header for authentication (configured via `LOCAL_DN` environment variable)
