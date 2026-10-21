/** @type {import('tailwindcss').Config} */
export default {
  content: ['./index.html', './src/**/*.{ts,tsx}'],
  theme: {
    extend: {
      colors: {
        // Fresh green primary, cream background, leaf-green accent
        brand: { 50: '#f0fdf4', 100: '#dcfce7', 200: '#bbf7d0', 300: '#86efac', 400: '#4ade80', 500: '#15803d', 600: '#166534', 700: '#14532d', 800: '#064e3b', 900: '#052e16' },
        cream: { 50: '#fffdf9', 100: '#fff8ef', 200: '#fbefdc', 300: '#f3e0c3' },
        leaf: { 50: '#f1f7ec', 100: '#dfecd2', 500: '#5b8a3c', 600: '#4a7330', 700: '#3b5c27' },
        ink: { 900: '#2b2118', 700: '#4d3f33', 500: '#7a6a5b', 300: '#b9ab9c' },
      },
      fontFamily: { sans: ['Nunito', 'ui-rounded', 'system-ui', 'sans-serif'] },
      boxShadow: { card: '0 2px 10px rgba(20, 83, 45, 0.08)' },
    },
  },
  plugins: [],
}
