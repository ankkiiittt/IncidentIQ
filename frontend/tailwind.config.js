/** @type {import('tailwindcss').Config} */
export default {
  content: [
    "./index.html",
    "./src/**/*.{js,ts,jsx,tsx}",
  ],
  theme: {
    extend: {
      colors: {
        bg: '#F8FAFC',
        surface: '#FFFFFF',
        border: '#E2E8F0',
        text: '#0F172A',
        muted: '#64748B',
        primary: {
          DEFAULT: '#4F46E5',
          hover: '#4338CA',
          light: '#EEF2FF',
        },
        p1: {
          text: '#B91C1C',
          bg: '#FEE2E2',
        },
        p2: {
          text: '#C2410C',
          bg: '#FFEDD5',
        },
        p3: {
          text: '#A16207',
          bg: '#FEF9C3',
        },
        p4: {
          text: '#475569',
          bg: '#F1F5F9',
        },
        success: {
          text: '#15803D',
          bg: '#DCFCE7',
        },
        ai: {
          text: '#6D28D9',
          bg: '#EDE9FE',
          border: '#DDD6FE',
        },
        series: {
          1: '#4F46E5',
          2: '#0EA5E9',
          3: '#F59E0B',
          4: '#10B981',
          5: '#EC4899',
          6: '#64748B',
        },
      },
      fontFamily: {
        sans: ['Inter', 'system-ui', '-apple-system', 'sans-serif'],
        mono: ['JetBrains Mono', 'monospace'],
      },
      maxWidth: {
        layout: '1440px',
      },
      borderRadius: {
        card: '8px',
      },
    },
  },
  plugins: [],
};
