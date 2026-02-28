/**
 * API route for creating books
 */

import { NextResponse } from 'next/server'

import { SSL_CLIENT_ISSUER_HEADER, SSL_CLIENT_SUBJECT_HEADER } from '@/lib/auth-headers'
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
      [SSL_CLIENT_SUBJECT_HEADER]: headersObj[SSL_CLIENT_SUBJECT_HEADER],
      [SSL_CLIENT_ISSUER_HEADER]: headersObj[SSL_CLIENT_ISSUER_HEADER],
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
