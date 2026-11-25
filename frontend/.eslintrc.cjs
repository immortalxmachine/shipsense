module.exports = {
  root: true,
  env: { browser: true, es2020: true },
  extends: [
    "eslint:recommended",
    "plugin:@typescript-eslint/recommended",
  ],
  parser: "@typescript-eslint/parser",
  parserOptions: { ecmaVersion: "latest", sourceType: "module" },
  plugins: ["react-refresh", "react-hooks"],
  ignorePatterns: ["dist", ".eslintrc.cjs", "vite.config.ts", "postcss.config.js", "tailwind.config.js"],
  rules: {
    "react-hooks/rules-of-hooks": "error",
    "react-hooks/exhaustive-deps": "off",
    "react-refresh/only-export-components": ["warn", { allowConstantExport: true }],
  },
};
