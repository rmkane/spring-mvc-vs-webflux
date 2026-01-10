/**
 * Utility functions
 */

/**
 * Gets the next element in an array, wrapping around to the first element
 * @param array - The array to cycle through
 * @param current - The current element
 * @returns The next element in the array
 */
export function getNextElement<T>(array: readonly T[], current: T): T {
  const currentIndex = array.indexOf(current)
  if (currentIndex === -1) {
    return array[0]
  }
  return array[(currentIndex + 1) % array.length]
}

/**
 * Gets the previous element in an array, wrapping around to the last element
 * @param array - The array to cycle through
 * @param current - The current element
 * @returns The previous element in the array
 */
export function getPreviousElement<T>(array: readonly T[], current: T): T {
  const currentIndex = array.indexOf(current)
  if (currentIndex === -1) {
    return array[array.length - 1]
  }
  return array[(currentIndex - 1 + array.length) % array.length]
}
