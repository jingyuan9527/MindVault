/** @type {import('tailwindcss').Config} */
export default {
  content: [
    "./index.html",
    "./src/**/*.{vue,js,ts,jsx,tsx}",
  ],
  theme: {
    extend: {
      fontFamily: {
        display: ['Outfit', 'ui-sans-serif', 'system-ui', 'sans-serif'],
        body: ['Inter', 'ui-sans-serif', 'system-ui', 'sans-serif'],
        mono: ['JetBrains Mono', 'monospace'],
      },
      colors: {
        glass: {
          DEFAULT: 'rgba(255, 255, 255, 0.06)',
          light: 'rgba(255, 255, 255, 0.10)',
          lighter: 'rgba(255, 255, 255, 0.03)',
          border: 'rgba(255, 255, 255, 0.08)',
          hover: 'rgba(255, 255, 255, 0.12)',
        },
        primary: {
          DEFAULT: '#8B5CF6',
          hover: '#A78BFA',
          light: 'rgba(139, 92, 246, 0.15)',
        },
        accent: {
          DEFAULT: '#F59E0B',
          hover: '#FBBF24',
        },
        surface: {
          DEFAULT: '#0B0E17',
          light: '#111827',
        }
      },
      backdropBlur: {
        glass: '20px',
      },
    },
  },
  plugins: [],
}