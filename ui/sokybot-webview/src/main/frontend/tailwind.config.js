import animate from "tailwindcss-animate"

/** @type {import('tailwindcss').Config} */
export default {
    darkMode: ["class"],
    content: [
        "./pages/**/*.{ts,tsx}",
        "./components/**/*.{ts,tsx}",
        "./app/**/*.{ts,tsx}",
        "./src/**/*.{ts,tsx}",
        "../../../../sokybot-frontend-shared/src/**/*.{ts,tsx}",
        "../../../../../scripts/pages/**/*.json",
    ],
    presets: [require('../../../../sokybot-frontend-shared/tailwind-preset.js')],
    prefix: "",
    prefix: "",
    theme: {
        extend: {},
    },
}
