/** @type {import('tailwindcss').Config} */
export default {
  content: ['./index.html', './src/**/*.{ts,tsx}'],
  theme: {
    extend: {
      colors: {
        brand: { 50: '#fffaf0', 100: '#f6e8c5', 200: '#ead49d', 300: '#d8b260', 400: '#bc8e2c', 500: '#8c600e', 600: '#754f0b', 700: '#5b3e0a', 800: '#422e09', 900: '#2e2108' },
        cream: { 50: '#fffdf8', 100: '#fff9ed', 200: '#f8efd9', 300: '#eee0bf' },
        leaf: { 50: '#f1f7ec', 100: '#dfecd2', 500: '#5b8a3c', 600: '#4a7330', 700: '#3b5c27' },
        ink: { 900: '#2b2118', 700: '#4d3f33', 500: '#7a6a5b', 300: '#b9ab9c' },
      },
      fontFamily: { sans: ['Manrope', 'system-ui', 'sans-serif'] },
      boxShadow: { card: '0 2px 10px rgba(116, 73, 11, 0.09)' },
    },
  },
  plugins: [],
}
