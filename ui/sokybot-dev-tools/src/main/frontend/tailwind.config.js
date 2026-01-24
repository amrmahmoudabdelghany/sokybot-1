/** @type {import('tailwindcss').Config} */
export default {
  darkMode: ["class"],
  content: [
    "./index.html",
    "./src/**/*.{js,ts,jsx,tsx}",
    "../../../../sokybot-frontend-shared/src/**/*.{ts,tsx}",
  ],
  presets: [require('../../../../sokybot-frontend-shared/tailwind-preset.js')],
  theme: {
    extend: {},
  },
}
