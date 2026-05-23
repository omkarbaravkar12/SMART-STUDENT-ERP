#!/bin/bash
# ============================================================
# Smart Student ERP — Build Script
# Usage: chmod +x build.sh && ./build.sh
# ============================================================

set -e

PROJECT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
SRC_DIR="$PROJECT_DIR/backend/src"
OUT_DIR="$PROJECT_DIR/backend/out"
LIB_DIR="$PROJECT_DIR/backend/lib"

echo "╔══════════════════════════════════════════╗"
echo "║   Smart Student ERP — Build Script        ║"
echo "╚══════════════════════════════════════════╝"

# Check Java
if ! command -v javac &> /dev/null; then
  echo "❌ Java not found. Install JDK 17+ from https://adoptium.net"
  exit 1
fi

JAVA_VER=$(java -version 2>&1 | awk -F '"' '/version/ {print $2}' | cut -d'.' -f1)
echo "✓ Java $JAVA_VER found"

# Check JDBC driver
JDBC_JAR=$(find "$LIB_DIR" -name "postgresql-*.jar" 2>/dev/null | head -1)
if [ -z "$JDBC_JAR" ]; then
  echo ""
  echo "⚠  PostgreSQL JDBC driver not found in $LIB_DIR"
  echo "   Download from: https://jdbc.postgresql.org/download/"
  echo "   Save as: $LIB_DIR/postgresql-42.x.x.jar"
  echo ""
  echo "   Auto-downloading latest..."
  mkdir -p "$LIB_DIR"
  if command -v curl &> /dev/null; then
    LATEST_JAR="https://jdbc.postgresql.org/download/postgresql-42.7.3.jar"
    curl -L "$LATEST_JAR" -o "$LIB_DIR/postgresql-42.7.3.jar" && echo "✓ JDBC driver downloaded"
    JDBC_JAR="$LIB_DIR/postgresql-42.7.3.jar"
  else
    echo "   curl not found. Please download manually."
    exit 1
  fi
fi
echo "✓ JDBC driver: $(basename $JDBC_JAR)"

# Clean
rm -rf "$OUT_DIR"
mkdir -p "$OUT_DIR"

# Compile
echo ""
echo "🔨 Compiling Java sources..."

find "$SRC_DIR" -name "*.java" | head -5 | while read f; do echo "   $f"; done
echo "   ..."

javac \
  -cp "$JDBC_JAR" \
  -d "$OUT_DIR" \
  -sourcepath "$SRC_DIR" \
  $(find "$SRC_DIR" -name "*.java" | tr '\n' ' ')

echo "✅ Compilation successful!"
echo ""
echo "🚀 To run the server:"
echo "   java -cp \"$OUT_DIR:$JDBC_JAR\" Main"
echo ""
echo "📋 Or use run.sh"
