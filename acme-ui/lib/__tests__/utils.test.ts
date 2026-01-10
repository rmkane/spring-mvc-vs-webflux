import { getNextElement, getPreviousElement } from '@/lib/utils';

describe('getNextElement', () => {
  it('should return the next element in the array', () => {
    const array = ['a', 'b', 'c'] as const;
    expect(getNextElement(array, 'a')).toBe('b');
    expect(getNextElement(array, 'b')).toBe('c');
  });

  it('should wrap around to the first element when at the end', () => {
    const array = ['a', 'b', 'c'] as const;
    expect(getNextElement(array, 'c')).toBe('a');
  });

  it('should return the first element if current is not found', () => {
    const array = ['a', 'b', 'c'] as const;
    expect(getNextElement(array, 'x')).toBe('a');
  });

  it('should work with single element array', () => {
    const array = ['a'] as const;
    expect(getNextElement(array, 'a')).toBe('a');
  });

  it('should work with numeric arrays', () => {
    const array = [1, 2, 3] as const;
    expect(getNextElement(array, 1)).toBe(2);
    expect(getNextElement(array, 3)).toBe(1);
  });
});

describe('getPreviousElement', () => {
  it('should return the previous element in the array', () => {
    const array = ['a', 'b', 'c'] as const;
    expect(getPreviousElement(array, 'b')).toBe('a');
    expect(getPreviousElement(array, 'c')).toBe('b');
  });

  it('should wrap around to the last element when at the beginning', () => {
    const array = ['a', 'b', 'c'] as const;
    expect(getPreviousElement(array, 'a')).toBe('c');
  });

  it('should return the last element if current is not found', () => {
    const array = ['a', 'b', 'c'] as const;
    expect(getPreviousElement(array, 'x')).toBe('c');
  });

  it('should work with single element array', () => {
    const array = ['a'] as const;
    expect(getPreviousElement(array, 'a')).toBe('a');
  });

  it('should work with numeric arrays', () => {
    const array = [1, 2, 3] as const;
    expect(getPreviousElement(array, 2)).toBe(1);
    expect(getPreviousElement(array, 1)).toBe(3);
  });
});
