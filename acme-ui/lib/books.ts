/**
 * Book API functions using the API utility
 */

import { apiRequest, apiRequestJson } from '@/lib/api'
import type { Book, CreateBookRequest, UpdateBookRequest } from '@/lib/types'

const BOOKS_API_PATH = '/api/v1/books'

/**
 * Get all books
 * Headers are automatically retrieved from Next.js headers() in Server Components,
 * or can be passed explicitly from API routes.
 * @param incomingHeaders - Optional headers from the incoming request (for API routes only)
 */
export async function getAllBooks(
  incomingHeaders?: Headers | Record<string, string>
): Promise<Book[]> {
  return apiRequestJson<Book[]>(BOOKS_API_PATH, {}, incomingHeaders)
}

/**
 * Get a book by ID
 * Headers are automatically retrieved from Next.js headers() in Server Components,
 * or can be passed explicitly from API routes.
 * @param incomingHeaders - Optional headers from the incoming request (for API routes only)
 */
export async function getBookById(
  id: number,
  incomingHeaders?: Headers | Record<string, string>
): Promise<Book> {
  return apiRequestJson<Book>(`${BOOKS_API_PATH}/${id}`, {}, incomingHeaders)
}

/**
 * Create a new book
 * @param incomingHeaders - Optional headers from the incoming request (for Kubernetes/ingress)
 */
export async function createBook(
  book: CreateBookRequest,
  incomingHeaders?: Headers | Record<string, string>
): Promise<Book> {
  return apiRequestJson<Book>(
    BOOKS_API_PATH,
    {
      method: 'POST',
      headers: {
        'Content-Type': 'application/json',
      },
      body: JSON.stringify(book),
    },
    incomingHeaders
  )
}

/**
 * Update a book
 * @param incomingHeaders - Optional headers from the incoming request (for Kubernetes/ingress)
 */
export async function updateBook(
  id: number,
  book: UpdateBookRequest,
  incomingHeaders?: Headers | Record<string, string>
): Promise<Book> {
  return apiRequestJson<Book>(
    `${BOOKS_API_PATH}/${id}`,
    {
      method: 'PUT',
      headers: {
        'Content-Type': 'application/json',
      },
      body: JSON.stringify(book),
    },
    incomingHeaders
  )
}

/**
 * Delete a book
 * @param incomingHeaders - Optional headers from the incoming request (for Kubernetes/ingress)
 */
export async function deleteBook(
  id: number,
  incomingHeaders?: Headers | Record<string, string>
): Promise<void> {
  const response = await apiRequest(
    `${BOOKS_API_PATH}/${id}`,
    {
      method: 'DELETE',
    },
    incomingHeaders
  )
  if (!response.ok) {
    throw new Error(`Failed to delete book: ${response.status} ${response.statusText}`)
  }
}
