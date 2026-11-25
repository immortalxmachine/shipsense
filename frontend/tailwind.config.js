/** @type {import('tailwindcss').Config} */
export default {
  content: ["./index.html", "./src/**/*.{ts,tsx}"],
  theme: {
    extend: {
      colors: {
        brand: {
          DEFAULT: "#4D148C", // FedEx-style purple
          dark: "#3a0f6b",
        },
        accent: "#FF6600", // FedEx-style orange
      },
    },
  },
  plugins: [],
};
