/** @type {import('tailwindcss').Config} */
export default {
  content: ['./index.html', './src/**/*.{ts,tsx}'],
  theme: {
    extend: {
      colors: {
        // Warm terracotta primary, cream background, leaf-green accent
        brand: { 50: '#fff4ee', 100: '#ffe4d4', 200: '#fbc6a6', 300: '#f5a173', 400: '#ec7a47', 500: '#d95f2b', 600: '#c24e21', 700: '#a13f1d', 800: '#82361e', 900: '#6a301c' },
        cream: { 50: '#fffdf9', 100: '#fff8ef', 200: '#fbefdc', 300: '#f3e0c3' },
        leaf: { 50: '#f1f7ec', 100: '#dfecd2', 500: '#5b8a3c', 600: '#4a7330', 700: '#3b5c27' },
        ink: { 900: '#2b2118', 700: '#4d3f33', 500: '#7a6a5b', 300: '#b9ab9c' },
      },
      fontFamily: { sans: ['Nunito', 'ui-rounded', 'system-ui', 'sans-serif'] },
      boxShadow: { card: '0 2px 10px rgba(106, 48, 28, 0.08)' },
    },
  },
  plugins: [],
}
