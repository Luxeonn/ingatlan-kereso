import js from '@eslint/js';
import tseslint from 'typescript-eslint';
import reactHooks from 'eslint-plugin-react-hooks';
import reactRefresh from 'eslint-plugin-react-refresh';

export default tseslint.config(
  { ignores: ['dist', 'node_modules', '*.config.js', '*.config.ts'] },
  {
    extends: [js.configs.recommended, ...tseslint.configs.recommended],
    files: ['**/*.{ts,tsx}'],
    plugins: {
      'react-hooks': reactHooks,
      'react-refresh': reactRefresh,
    },
    rules: {
      ...reactHooks.configs.recommended.rules,
      'react-hooks/preserve-manual-memoization': 'off',
      'react-refresh/only-export-components': [
        'error',
        { allowConstantExport: true },
      ],
      '@typescript-eslint/no-unused-vars': [
        'error',
        { argsIgnorePattern: '^_', varsIgnorePattern: '^_' },
      ],
      '@typescript-eslint/no-explicit-any': 'error',
      '@typescript-eslint/consistent-type-imports': [
        'error',
        { prefer: 'type-imports', fixStyle: 'separate-type-imports' },
      ],
      'complexity': ['error', 15],
      'max-depth': ['error', 4],
      'max-params': ['error', 5],
      'max-lines': ['error', { max: 350, skipBlankLines: true, skipComments: true }],
      'eqeqeq': ['error', 'always'],
      'no-lonely-if': 'error',
      'no-else-return': 'error',
      'object-shorthand': ['error', 'always'],
      'no-var': 'error',
      'prefer-const': 'error',
      'no-console': ['error', { allow: ['warn', 'error'] }],
      'no-restricted-properties': [
        'error',
        {
          object: 'window',
          property: 'open',
          message: 'Direct window.open is forbidden. Use openExternalUrl from src/utils/formatters.ts instead.',
        },
      ],
    },
  },
  {
    files: ['src/types/generated-api.ts'],
    rules: {
      'max-lines': 'off',
    },
  },
  {
    files: [
      'src/components/**/*.{ts,tsx}',
      'src/hooks/**/*.{ts,tsx}',
      'src/utils/listingFilters.ts',
      'src/utils/marketAnalytics*.ts',
    ],
    rules: {
      'no-restricted-syntax': [
        'error',
        {
          selector: 'MemberExpression[property.name="amountHuf"] > MemberExpression[property.name="priceInfo"]',
          message:
            'Direct access to item.priceInfo.amountHuf violates multi-source domain invariants. Use getEffectivePrice(item).priceFt or priceMillion from src/utils/formatters.ts instead.',
        },
        {
          selector: 'MemberExpression[property.name="pricePerSquareMeter"] > MemberExpression[property.name="detailsInfo"]',
          message:
            'Direct access to item.detailsInfo.pricePerSquareMeter violates multi-source domain invariants. Use getEffectiveSqmPrice(item) from src/utils/formatters.ts instead.',
        },
        {
          selector: 'VariableDeclarator[init.property.name="priceInfo"] > ObjectPattern > Property[key.name="amountHuf"]',
          message:
            'Destructuring amountHuf from priceInfo violates multi-source domain invariants. Use getEffectivePrice(item) from src/utils/formatters.ts instead.',
        },
        {
          selector: 'VariableDeclarator[init.property.name="detailsInfo"] > ObjectPattern > Property[key.name="pricePerSquareMeter"]',
          message:
            'Destructuring pricePerSquareMeter from detailsInfo violates multi-source domain invariants. Use getEffectiveSqmPrice(item) from src/utils/formatters.ts instead.',
        },
      ],
    },
  },
  {
    files: ['src/utils/formatters.ts', 'src/test/**/*.{ts,tsx}', 'e2e/**/*.{ts,tsx}'],
    rules: {
      'no-restricted-properties': 'off',
    },
  },
  {
    files: ['src/test/**/*.{ts,tsx}', 'e2e/**/*.{ts,tsx}'],
    rules: {
      'complexity': 'off',
      'max-params': 'off',
      'max-lines': 'off',
    },
  }
);
