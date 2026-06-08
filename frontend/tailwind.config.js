/** @type {import('tailwindcss').Config} */
module.exports = {
  content: [
    "./src/**/*.{html,ts}",
    "./src/app/**/*.{html,ts}"
  ],
  theme: {
    extend: {
      colors: {
        background: '#0a0d14',
        surface: '#121622',
        'surface-hover': '#1a2032',
        primary: {
          DEFAULT: '#3b82f6',
          dark: '#1d4ed8',
          light: '#60a5fa'
        },
        accent: {
          DEFAULT: '#f59e0b',
          purple: '#8b5cf6',
          pink: '#ec4899',
          green: '#10b981'
        },
        card: {
          gold: 'rgba(245, 158, 11, 0.15)',
          silver: 'rgba(156, 163, 175, 0.15)',
          bronze: 'rgba(180, 83, 9, 0.15)'
        }
      },
      fontFamily: {
        sans: ['Outfit', 'Inter', 'system-ui', 'sans-serif'],
      },
      boxShadow: {
        'glow-primary': '0 0 15px rgba(59, 130, 246, 0.5)',
        'glow-accent': '0 0 15px rgba(245, 158, 11, 0.5)',
        'glow-purple': '0 0 15px rgba(139, 92, 246, 0.5)'
      }
    },
  },
  plugins: [],
}
