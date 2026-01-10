import { render, screen, waitFor } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import React from 'react'

import { BookCard } from '@/components/BookCard'
import { ToastProvider } from '@/components/ToastProvider'
import type { Book } from '@/lib/types'

// Mock Next.js router
const mockRefresh = jest.fn()
jest.mock('next/navigation', () => ({
  useRouter: () => ({
    refresh: mockRefresh,
  }),
}))

// Mock fetch
const mockFetch = jest.fn()
global.fetch = mockFetch

const mockBook: Book = {
  id: 1,
  title: 'The Great Gatsby',
  author: 'F. Scott Fitzgerald',
  isbn: '978-0-7432-7356-5',
  publicationYear: 1925,
  createdAt: '2024-01-01T00:00:00Z',
  createdBy: 'cn=admin,dc=corp,dc=acme,dc=org',
  updatedAt: '2024-01-01T00:00:00Z',
  updatedBy: 'cn=admin,dc=corp,dc=acme,dc=org',
}

const renderWithToast = (component: React.ReactElement) => {
  return render(<ToastProvider>{component}</ToastProvider>)
}

describe('BookCard', () => {
  beforeEach(() => {
    jest.clearAllMocks()
    mockFetch.mockResolvedValue({
      ok: true,
      json: async () => ({}),
    })
  })

  it('should render book information', () => {
    renderWithToast(<BookCard book={mockBook} />)

    expect(screen.getByText('The Great Gatsby')).toBeInTheDocument()
    expect(screen.getByText(/F. Scott Fitzgerald/)).toBeInTheDocument()
    expect(screen.getByText(/978-0-7432-7356-5/)).toBeInTheDocument()
    expect(screen.getByText(/1925/)).toBeInTheDocument()
  })

  it('should render Edit and Delete buttons', () => {
    renderWithToast(<BookCard book={mockBook} />)

    expect(screen.getByText('Edit')).toBeInTheDocument()
    expect(screen.getByText('Delete')).toBeInTheDocument()
  })

  it('should have a link to the book edit page', () => {
    renderWithToast(<BookCard book={mockBook} />)

    const editLink = screen.getByText('Edit').closest('a')
    expect(editLink).toHaveAttribute('href', '/books/1')
  })

  it('should show confirmation dialog when delete is clicked', async () => {
    const user = userEvent.setup()
    renderWithToast(<BookCard book={mockBook} />)

    const deleteButton = screen.getByText('Delete')
    await user.click(deleteButton)

    expect(screen.getByText('Delete Book')).toBeInTheDocument()
    expect(screen.getByText(/Are you sure you want to delete/)).toBeInTheDocument()
  })

  it('should delete book when confirmed', async () => {
    const user = userEvent.setup()
    renderWithToast(<BookCard book={mockBook} />)

    const deleteButton = screen.getByLabelText('Delete The Great Gatsby')
    await user.click(deleteButton)

    // Wait for dialog to appear
    await waitFor(() => {
      expect(screen.getByText('Delete Book')).toBeInTheDocument()
    })

    // Find the confirm button in the dialog (the one with red background)
    const confirmButton = screen
      .getByRole('dialog')
      .querySelector('button.bg-red-600') as HTMLButtonElement
    expect(confirmButton).toBeInTheDocument()
    await user.click(confirmButton!)

    await waitFor(() => {
      expect(mockFetch).toHaveBeenCalledWith('/api/books/1', {
        method: 'DELETE',
      })
    })
  })
})
