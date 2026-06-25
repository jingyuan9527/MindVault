import js from '@eslint/js'
import pluginVue from 'eslint-plugin-vue'
import prettier from 'eslint-config-prettier'
import globals from 'globals'

export default [
  js.configs.recommended,
  ...pluginVue.configs['flat/recommended'],
  prettier,
  {
    ignores: ['dist/**', 'node_modules/**', '*.config.*', 'web-clipper/**'],
  },
  {
    languageOptions: {
      globals: {
        ...globals.browser,
        ...globals.worker,
        useDialog: 'readonly',
        useMessage: 'readonly',
        useNotification: 'readonly',
        useLoadingBar: 'readonly',
      },
    },
    rules: {
      'vue/multi-word-component-names': 'off',
      'vue/require-default-prop': 'off',
      'no-unused-vars': ['warn', { argsIgnorePattern: '^_' }],
      'no-empty': ['warn', { allowEmptyCatch: true }],
    },
  },
  {
    files: ['src/__tests__/**'],
    languageOptions: {
      globals: {
        ...globals.browser,
        ...globals.vitest,
        global: 'readonly',
      },
    },
    rules: {
      'no-empty': 'off',
    },
  },
]