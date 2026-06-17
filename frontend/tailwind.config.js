/** @type {import('tailwindcss').Config} */
export default {
  content: [
    "./index.html",
    "./src/**/*.{vue,js,ts,jsx,tsx}",
  ],
  theme: {
    extend: {
      fontFamily: {
        display: ['Playfair Display', 'Georgia', 'serif'],
        body: ['DM Sans', 'ui-sans-serif', 'system-ui', 'sans-serif'],
      },
      colors: {
        cream: '#f8f6f3',
        clay: '#c65f39',
        sage: '#5d7a5a',
        ink: '#2d2a24',
        stone: '#8a857f',
        border: '#e8e4de',
      }
    },
  },
  plugins: [],
}