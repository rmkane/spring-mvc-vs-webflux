/**
 * Book API functions using the API utility
 */

import { apiRequestJson, apiRequest } from './api';
import type { Book, CreateBookRequest, UpdateBookRequest } from './types';

const BOOKS_API_PATH = '/api/v1/books';

/**
 * Get all books
 */
export async function getAllBooks(): Promise<Book[]> {
  return apiRequestJson<Book[]>(BOOKS_API_PATH);
}

/**
 * Get a book by ID
 */
export async function getBookById(id: number): Promise<Book> {
  return apiRequestJson<Book>(`${BOOKS_API_PATH}/${id}`);
}

/**
 * Create a new book
 */
export async function createBook(book: CreateBookRequest): Promise<Book> {
  return apiRequestJson<Book>(BOOKS_API_PATH, {
    method: 'POST',
    headers: {
      'Content-Type': 'application/json',
    },
    body: JSON.stringify(book),
  });
}

/**
 * Update a book
 */
export async function updateBook(
  id: number,
  book: UpdateBookRequest
): Promise<Book> {
  return apiRequestJson<Book>(`${BOOKS_API_PATH}/${id}`, {
    method: 'PUT',
    headers: {
      'Content-Type': 'application/json',
    },
    body: JSON.stringify(book),
  });
}

/**
 * Delete a book
 */
export async function deleteBook(id: number): Promise<void> {
  const response = await apiRequest(`${BOOKS_API_PATH}/${id}`, {
    method: 'DELETE',
  });
  if (!response.ok) {
    throw new Error(`Failed to delete book: ${response.status} ${response.statusText}`);
  }
}
