import type { NextRequest } from 'next/server'
import { NextResponse } from 'next/server'

/**
 * Middleware to extract SSL client certificate headers from ingress
 * and make them available to Server Components via request headers.
 *
 * In Kubernetes, the ingress passes ssl-client-subject-dn and ssl-client-issuer-dn
 * as headers. This middleware ensures they're available throughout the request.
 */
export function middleware(request: NextRequest) {
  // Extract SSL client certificate headers from ingress
  const subjectDn = request.headers.get('ssl-client-subject-dn')
  const issuerDn = request.headers.get('ssl-client-issuer-dn')

  // Create a response
  const response = NextResponse.next()

  // If headers are present (from ingress), set them as request headers
  // so they're available to Server Components via headers()
  if (subjectDn) {
    response.headers.set('ssl-client-subject-dn', subjectDn)
  }
  if (issuerDn) {
    response.headers.set('ssl-client-issuer-dn', issuerDn)
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
