#!/bin/bash
# ============================================================
# Smart Student ERP — Run Script
# Usage: chmod +x run.sh && ./run.sh
# ============================================================

PROJECT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
OUT_DIR="$PROJECT_DIR/backend/out"
LIB_DIR="$PROJECT_DIR/backend/lib"
JDBC_JAR=$(find "$LIB_DIR" -name "postgresql-*.jar" 2>/dev/null | head -1)

# Database config (edit these or set as environment variables)
export DB_HOST="${DB_HOST:-localhost}"
export DB_PORT="${DB_PORT:-5432}"
export DB_NAME="${DB_NAME:-smart_erp}"
export DB_USER="${DB_USER:-postgres}"
export DB_PASSWORD="${DB_PASSWORD:-postgres}"
export PORT="${PORT:-8080}"

echo "╔══════════════════════════════════════════╗"
echo "║   Smart Student ERP — Starting Server     ║"
echo "╚══════════════════════════════════════════╝"
echo "  DB Host:  $DB_HOST:$DB_PORT/$DB_NAME"
echo "  API Port: $PORT"
echo ""

# Build first if out directory doesn't exist
if [ ! -d "$OUT_DIR" ] || [ -z "$(ls -A $OUT_DIR)" ]; then
  echo "Building first..."
  bash "$PROJECT_DIR/build.sh"
fi

java -cp "$OUT_DIR:$JDBC_JAR" Main
