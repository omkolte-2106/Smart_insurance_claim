/** @type {import('tailwindcss').Config} */
export default {
  content: ["./index.html", "./src/**/*.{js,ts,jsx,tsx}"],
  theme: {
    extend: {
      fontFamily: {
        sans: ["\"DM Sans\"", "Inter", "system-ui", "sans-serif"],
        display: ["\"Outfit\"", "Inter", "system-ui", "sans-serif"],
      },
      colors: {
        surface: "#f0f4f8",
        canvas: "#fafbfd",
        card: "#ffffff",
        brand: {
          50: "#ecfaf8",
          100: "#c9f2ec",
          200: "#93e0d4",
          300: "#5fbfb3",
          500: "#0d9488",
          600: "#0f766e",
          700: "#115e59",
        },
        accent: {
          500: "#2563eb",
          600: "#1d4ed8",
        },
        ink: {
          950: "#0b1220",
          900: "#0f172a",
          700: "#334155",
          600: "#475569",
          500: "#64748b",
          400: "#94a3b8",
        },
      },
      boxShadow: {
        soft: "0 12px 40px -12px rgba(15, 23, 42, 0.12)",
        glow: "0 0 0 1px rgba(13, 148, 136, 0.12), 0 20px 50px -20px rgba(13, 148, 136, 0.25)",
      },
      backgroundImage: {
        "mesh-light":
          "radial-gradient(at 40% 20%, rgba(13, 148, 136, 0.08) 0px, transparent 50%), radial-gradient(at 80% 0%, rgba(37, 99, 235, 0.06) 0px, transparent 45%), radial-gradient(at 0% 50%, rgba(15, 23, 42, 0.04) 0px, transparent 50%)",
        "hero-fade": "linear-gradient(180deg, #fafbfd 0%, #f0f4f8 100%)",
      },
      animation: {
        "fade-in": "fadeIn 0.5s ease-out forwards",
        float: "float 6s ease-in-out infinite",
      },
      keyframes: {
        fadeIn: {
          from: { opacity: "0", transform: "translateY(8px)" },
          to: { opacity: "1", transform: "translateY(0)" },
        },
        float: {
          "0%, 100%": { transform: "translateY(0)" },
          "50%": { transform: "translateY(-6px)" },
        },
      },
    },
  },
  plugins: [],
};
