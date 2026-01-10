/**
 * API route for creating books
 */

import { NextResponse } from 'next/server';
import { createBook } from '@/lib/books';
import type { CreateBookRequest } from '@/lib/types';

export async function POST(request: Request) {
  try {
    const body: CreateBookRequest = await request.json();
    const book = await createBook(body);
    return NextResponse.json(book, { status: 201 });
  } catch (error) {
    console.error('Error creating book:', error);
    return NextResponse.json(
      { error: error instanceof Error ? error.message : 'Failed to create book' },
      { status: 500 }
    );
  }
}
