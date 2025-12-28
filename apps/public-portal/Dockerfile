FROM docker.io/library/node:20-slim

WORKDIR /app

# Copy package.json and cache deps
COPY package*.json ./
RUN npm install

# Copy source
COPY . .

# Build
RUN npm run build

# Expose port (Nuxt default is 3000)
EXPOSE 3000

ENV NUXT_HOST=0.0.0.0
ENV NUXT_PORT=3000

CMD ["node", ".output/server/index.mjs"]
