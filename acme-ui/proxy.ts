import type { NextRequest } from 'next/server'
import { NextResponse } from 'next/server'

import { SSL_CLIENT_ISSUER_HEADER, SSL_CLIENT_SUBJECT_HEADER } from '@/lib/auth-headers'

/**
 * Middleware to extract SSL client certificate headers from ingress
 * and make them available to Server Components via request headers.
 *
 * In Kubernetes, the ingress passes subject and issuer DN headers (names from
 * auth-headers). This middleware ensures they're available throughout the request.
 */
export function proxy(request: NextRequest) {
  // Extract SSL client certificate headers from ingress
  const subjectDn = request.headers.get(SSL_CLIENT_SUBJECT_HEADER)
  const issuerDn = request.headers.get(SSL_CLIENT_ISSUER_HEADER)

  // Create a response
  const response = NextResponse.next()

  // If headers are present (from ingress), set them as request headers
  // so they're available to Server Components via headers()
  if (subjectDn) {
    response.headers.set(SSL_CLIENT_SUBJECT_HEADER, subjectDn)
  }
  if (issuerDn) {
    response.headers.set(SSL_CLIENT_ISSUER_HEADER, issuerDn)
  }

  return response
}

// Only run middleware on API routes and pages (not static assets)
export const config = {
  matcher: [
    /*
     * Match all request paths except for the ones starting with:
     * - _next/static (static files)
     * - _next/image (image optimization files)
     * - favicon.ico (favicon file)
     * - public files (public folder)
     */
    '/((?!_next/static|_next/image|favicon.ico|.*\\.(?:svg|png|jpg|jpeg|gif|webp)$).*)',
  ],
}
