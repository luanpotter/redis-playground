FROM oven/bun:1.1.20
WORKDIR /app
COPY package.json bun.lockb tsconfig.json vite.config.ts eslint.config.mjs .prettierrc.json .prettierignore index.html ./
COPY src ./src
RUN bun install --frozen-lockfile || bun install --frozen-lockfile || bun install
RUN bun run build
EXPOSE 5173
CMD ["bun","run","dev","--","--host","0.0.0.0"]
