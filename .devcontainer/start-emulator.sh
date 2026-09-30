#!/bin/bash
# Sobe o emulador Android do devcontainer em modo headless e espera o boot.
# Uso: ./.devcontainer/start-emulator.sh [nome-do-avd]

set -euo pipefail

AVD_NAME="${1:-${AVD_NAME:-docgen}}"
BOOT_TIMEOUT="${BOOT_TIMEOUT:-300}"

if [ ! -r /dev/kvm ]; then
    echo "❌ /dev/kvm indisponível. Remova --device=/dev/kvm do devcontainer.json ou habilite KVM na host."
    exit 1
fi

if ! avdmanager list avd -c 2>/dev/null | grep -qx "${AVD_NAME}"; then
    echo "❌ AVD '${AVD_NAME}' não existe. Rode ./.devcontainer/postCreate.sh para criá-lo."
    exit 1
fi

if adb devices 2>/dev/null | grep -qE "^${AVD_NAME}-emulator-[0-9]+\s+(device|bootloader|online)$"; then
    echo "✅ Emulador '${AVD_NAME}' já está em execução."
    adb devices
    exit 0
fi

echo "🚀 Iniciando emulador '${AVD_NAME}' (headless)..."
nohup emulator -avd "${AVD_NAME}" \
    -no-window \
    -no-audio \
    -no-boot-anim \
    -gpu swiftshader_indirect \
    -camera-back none \
    -camera-front none \
    > "${HOME}/${AVD_NAME}-emulator.log" 2>&1 &

echo "⏳ Aguardando boot do Android (até ${BOOT_TIMEOUT}s)..."
adb wait-for-device

deadline=$((SECONDS + BOOT_TIMEOUT))
until [ "$(adb shell getprop sys.boot_completed 2>/dev/null | tr -d '\r')" = "1" ]; do
    if [ "${SECONDS}" -ge "${deadline}" ]; then
        echo "❌ Timeout esperando o boot. Log: ${HOME}/${AVD_NAME}-emulator.log"
        exit 1
    fi
    sleep 3
done

echo "✅ Emulador pronto:"
adb devices
echo "📋 Log do emulador: ${HOME}/${AVD_NAME}-emulator.log"
