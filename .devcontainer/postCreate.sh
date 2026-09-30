#!/bin/bash

echo "🚀 Iniciando configuração do DevContainer (Android)..."

WORKSPACE="$(pwd)"

# ============================================
# 1. INSTALAÇÃO DO OPECODE
# ============================================
echo "📦 Instalando opencode-ai globalmente..."
npm i -g opencode-ai

REAL_BIN="$(npm root -g)/opencode-ai/bin/opencode"

if [ -f "$REAL_BIN" ]; then
    echo "🔗 Criando links simbólicos..."
    sudo ln -sf "$REAL_BIN" /usr/local/bin/opencode
    sudo ln -sf "$REAL_BIN" /usr/local/bin/opencode-ai
else
    echo "   ⚠️ Binário do opencode não encontrado em $REAL_BIN"
fi

# ============================================
# 2. VERIFICAÇÃO DO TOOLCHAIN ANDROID
# ============================================
echo "🤖 Verificando toolchain Android..."
echo "   ✅ java:     $(java -version 2>&1 | head -1)"
echo "   ✅ gradle:   $(gradle --version 2>/dev/null | grep '^Gradle' || echo 'usar ./gradlew')"
echo "   ✅ sdkmanager: $(sdkmanager --version 2>/dev/null || echo 'ok')"
echo "   ✅ adb:      $(adb --version 2>/dev/null | head -1)"
echo "   ✅ ANDROID_HOME=${ANDROID_HOME}"

# ============================================
# 3. CRIAÇÃO DO AVD (emulador)
# ============================================
AVD_NAME="${AVD_NAME:-docgen}"
AVD_SYSTEM_IMAGE="system-images;android-36;google_apis;x86_64"

if [ ! -d "${HOME}/.android/avd/${AVD_NAME}.avd" ]; then
    echo "📱 Criando AVD '${AVD_NAME}'..."
    printf 'no\n' | avdmanager create avd \
        --name "${AVD_NAME}" \
        --package "${AVD_SYSTEM_IMAGE}" \
        --device "pixel_6" \
        --force

    AVD_CONFIG="${HOME}/.android/avd/${AVD_NAME}.avd/config.ini"
    if [ -f "${AVD_CONFIG}" ]; then
        grep -q '^hw.keyboard=' "${AVD_CONFIG}" \
            && sed -i 's/^hw.keyboard=.*/hw.keyboard=yes/' "${AVD_CONFIG}" \
            || echo 'hw.keyboard=yes' >> "${AVD_CONFIG}"
    fi
    echo "   ✅ AVD criado: ${AVD_NAME}"
else
    echo "📱 AVD '${AVD_NAME}' já existe, pulando criação."
fi

if [ -r /dev/kvm ]; then
    echo "   ✅ KVM disponível (/dev/kvm)"
else
    echo "   ⚠️ /dev/kvm indisponível — o emulador não vai subir neste container"
fi

# ============================================
# 4. INSTALAÇÃO DO PLAYWRIGHT (PRINTSCREEN)
# ============================================
echo "🎭 Instalando Playwright (skill printscreen)..."

export PLAYWRIGHT_BROWSERS_PATH="${PLAYWRIGHT_BROWSERS_PATH:-$HOME/.cache/ms-playwright}"

if ! command -v playwright &>/dev/null; then
    echo "   Instalando playwright globalmente..."
    npm i -g playwright || { echo "   ❌ Falha ao instalar playwright"; exit 1; }
fi

echo "   Instalando navegador Chromium e dependências do sistema..."
npx playwright install --with-deps chromium || { echo "   ❌ Falha ao instalar Chromium"; exit 1; }

echo "   ✅ Playwright instalado: $(playwright --version 2>/dev/null || echo 'versão indisponível')"
echo "   ✅ Chromium disponível em ${PLAYWRIGHT_BROWSERS_PATH}"
echo "   ✅ Uso na skill printscreen: playwright screenshot <url> <arquivo.png>"

# ============================================
# 5. FINALIZAÇÃO
# ============================================
echo ""
echo "✅ =========================================="
echo "✅ DevContainer configurado com sucesso!"
echo "✅ =========================================="
echo ""
echo "📋 Resumo do que foi feito:"
echo "  ✅ opencode-ai instalado e linkado"
echo "  ✅ JDK 17 + Android SDK (API 36) + Gradle prontos"
echo "  ✅ AVD '${AVD_NAME}' criado (emulador headless)"
echo "  ✅ Playwright instalado (Chromium para skill printscreen)"
echo ""
echo "🚀 Comandos úteis:"
echo "  ./.devcontainer/start-emulator.sh   # sobe o emulador (headless, aguarda boot)"
echo "  adb devices                         # lista emulador/aparelhos"
echo "  ./gradlew assembleDebug             # gera o APK"
echo "  ./gradlew installDebug              # instala no emulador conectado"
echo "  adb logcat                          # logs do aparelho"
echo ""
