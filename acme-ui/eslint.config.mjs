import { defineConfig, globalIgnores } from 'eslint/config';
import nextVitals from 'eslint-config-next/core-web-vitals';
import nextTs from 'eslint-config-next/typescript';
import prettier from 'eslint-config-prettier';
import simpleImportSort from 'eslint-plugin-simple-import-sort';

const eslintConfig = defineConfig([
  ...nextVitals,
  ...nextTs,
  {
    plugins: {
      'simple-import-sort': simpleImportSort,
    },
    rules: {
      'simple-import-sort/imports': 'error',
      'simple-import-sort/exports': 'error',
      // Prohibit relative imports for TypeScript/JavaScript files
      // Allow relative imports for CSS, images, and other assets
      'no-restricted-imports': [
        'error',
        {
          patterns: [
            {
              group: [
                './*.ts',
                './*.tsx',
                './*.js',
                './*.jsx',
                '../*.ts',
                '../*.tsx',
                '../*.js',
                '../*.jsx',
              ],
              message:
                'Use absolute imports with @/ alias instead of relative imports for TypeScript/JavaScript files.',
            },
          ],
        },
      ],
    },
  },
  prettier, // Must be last to override other configs
  // Override default ignores of eslint-config-next.
  globalIgnores([
    // Default ignores of eslint-config-next:
    '.next/**',
    'out/**',
    'build/**',
    'next-env.d.ts',
    'coverage/**',
    'jest.config.js', // Jest config uses CommonJS require
  ]),
]);

export default eslintConfig;
