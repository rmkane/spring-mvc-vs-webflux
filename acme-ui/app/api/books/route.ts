/**
 * API route for creating books
 */

import { NextResponse } from 'next/server'

import { createBook } from '@/lib/books'
import type { CreateBookRequest } from '@/lib/types'

export async function POST(request: Request) {
  try {
    const body: CreateBookRequest = await request.json()
    // Pass incoming headers to forward SSL client certificate info from ingress
    // Convert Headers to a plain object for the API utility
    const headersObj: Record<string, string> = {}
    request.headers.forEach((value, key) => {
      headersObj[key] = value
    })

    // Debug: log headers to see what we're getting
    console.log('API route headers:', {
      'ssl-client-subject-dn': headersObj['ssl-client-subject-dn'],
      'ssl-client-issuer-dn': headersObj['ssl-client-issuer-dn'],
      allHeaders: Object.keys(headersObj),
    })

    const book = await createBook(body, headersObj)
    return NextResponse.json(book, { status: 201 })
  } catch (error) {
    console.error('Error creating book:', error)
    return NextResponse.json(
      { error: error instanceof Error ? error.message : 'Failed to create book' },
      { status: 500 }
    )
  }
}
