/**
 * API routes for updating and deleting books
 */

import { NextResponse } from 'next/server'

import { deleteBook, getBookById, updateBook } from '@/lib/books'
import type { UpdateBookRequest } from '@/lib/types'

interface RouteParams {
  params: Promise<{ id: string }>
}

export async function GET(request: Request, { params }: RouteParams) {
  try {
    const { id } = await params
    const bookId = parseInt(id, 10)

    if (isNaN(bookId)) {
      return NextResponse.json({ error: 'Invalid book ID' }, { status: 400 })
    }

    const book = await getBookById(bookId)
    return NextResponse.json(book)
  } catch (error) {
    console.error('Error fetching book:', error)
    if (error instanceof Error && error.message.includes('404')) {
      return NextResponse.json({ error: 'Book not found' }, { status: 404 })
    }
    return NextResponse.json(
      { error: error instanceof Error ? error.message : 'Failed to fetch book' },
      { status: 500 }
    )
  }
}

export async function PUT(request: Request, { params }: RouteParams) {
  try {
    const { id } = await params
    const bookId = parseInt(id, 10)

    if (isNaN(bookId)) {
      return NextResponse.json({ error: 'Invalid book ID' }, { status: 400 })
    }

    const body: UpdateBookRequest = await request.json()
    const book = await updateBook(bookId, body)
    return NextResponse.json(book)
  } catch (error) {
    console.error('Error updating book:', error)
    return NextResponse.json(
      { error: error instanceof Error ? error.message : 'Failed to update book' },
      { status: 500 }
    )
  }
}

export async function DELETE(request: Request, { params }: RouteParams) {
  try {
    const { id } = await params
    const bookId = parseInt(id, 10)

    if (isNaN(bookId)) {
      return NextResponse.json({ error: 'Invalid book ID' }, { status: 400 })
    }

    await deleteBook(bookId)
    return NextResponse.json({}, { status: 204 })
  } catch (error) {
    console.error('Error deleting book:', error)
    if (error instanceof Error && error.message.includes('404')) {
      return NextResponse.json({ error: 'Book not found' }, { status: 404 })
    }
    return NextResponse.json(
      { error: error instanceof Error ? error.message : 'Failed to delete book' },
      { status: 500 }
    )
  }
}
