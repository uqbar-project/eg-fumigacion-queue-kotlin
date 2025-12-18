import js from '@eslint/js'
import globals from 'globals'

export default [
  {
    ignores: [
      'node_modules/**',
      'dist/**',
      'build/**',
      '*.log',
    ],
  },
  js.configs.recommended,
  {
    files: ['**/*.js'],
    languageOptions: {
      ecmaVersion: 'latest',
      sourceType: 'module',
      globals: {
        ...globals.node,
      },
    },
    rules: {
      // estilo
      semi: ['error', 'never'],
      quotes: ['error', 'single'],
      indent: ['error', 2],
      'comma-dangle': ['error', 'always-multiline'],
      'object-curly-spacing': ['error', 'never'],

      // calidad
      'no-unused-vars': ['error', {argsIgnorePattern: '^_'}],
      'no-console': 'off',
      eqeqeq: ['error', 'always'],
      'prefer-const': 'error',

      // async / promesas
      'no-async-promise-executor': 'error',
    },
  },
]
