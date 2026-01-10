import { render, screen } from '@testing-library/react';

import type { Book } from '@/lib/types';

import { BookCard } from '../BookCard';

// Mock Next.js router
jest.mock('next/navigation', () => ({
  useRouter: () => ({
    refresh: jest.fn(),
  }),
}));

// Mock window.confirm and window.alert
global.confirm = jest.fn(() => true);
global.alert = jest.fn();

// Mock fetch
global.fetch = jest.fn();

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
};

describe('BookCard', () => {
  beforeEach(() => {
    jest.clearAllMocks();
  });

  it('should render book information', () => {
    render(<BookCard book={mockBook} />);

    expect(screen.getByText('The Great Gatsby')).toBeInTheDocument();
    expect(screen.getByText(/F. Scott Fitzgerald/)).toBeInTheDocument();
    expect(screen.getByText(/978-0-7432-7356-5/)).toBeInTheDocument();
    expect(screen.getByText(/1925/)).toBeInTheDocument();
  });

  it('should render Edit and Delete buttons', () => {
    render(<BookCard book={mockBook} />);

    expect(screen.getByText('Edit')).toBeInTheDocument();
    expect(screen.getByText('Delete')).toBeInTheDocument();
  });

  it('should have a link to the book edit page', () => {
    render(<BookCard book={mockBook} />);

    const editLink = screen.getByText('Edit').closest('a');
    expect(editLink).toHaveAttribute('href', '/books/1');
  });
});
