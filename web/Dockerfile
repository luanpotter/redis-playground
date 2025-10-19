FROM oven/bun:1.1.20
WORKDIR /app

# Copy dependency files first for better caching
COPY package.json bun.lockb ./

# Install dependencies with retry logic
RUN bun install --frozen-lockfile || \
    bun install --frozen-lockfile || \
    bun install

# Copy rest of the files
COPY tsconfig.json vite.config.ts eslint.config.mjs .prettierrc.json .prettierignore index.html ./
COPY src ./src

# Build the app
RUN bun run build

EXPOSE 5173
CMD ["bun","run","dev","--","--host","0.0.0.0"]
