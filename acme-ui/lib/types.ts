/**
 * TypeScript types for Book API models
 */

export interface Book {
  id: number;
  title: string;
  author: string;
  isbn: string;
  publicationYear: number;
  createdAt: string; // ISO date string
  createdBy: string;
  updatedAt: string; // ISO date string
  updatedBy: string;
}

export interface CreateBookRequest {
  title: string;
  author: string;
  isbn: string;
  publicationYear: number;
}

export interface UpdateBookRequest {
  title: string;
  author: string;
  isbn: string;
  publicationYear: number;
}
