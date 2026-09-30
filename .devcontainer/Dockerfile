# Android (Kotlin/Java) development container
# JDK 17 (AGP 8+/9) + Android SDK + Gradle + emulador com KVM
FROM mcr.microsoft.com/devcontainers/java:17-bookworm

ARG ANDROID_CMDLINE_TOOLS_VERSION=16111833
ARG ANDROID_API_LEVEL=36
ARG ANDROID_BUILD_TOOLS_VERSION=36.1.0
ARG GRADLE_VERSION=9.8.0
ARG GRADLE_SHA256=bafd5ce9cfaea0fbccfdc8439a1ac42fbd4cd9c89dc9a988228d8a2639a58e6c

ENV ANDROID_HOME=/opt/android-sdk
ENV ANDROID_SDK_ROOT=/opt/android-sdk

RUN apt-get update && export DEBIAN_FRONTEND=noninteractive \
    && apt-get -y install --no-install-recommends \
        ca-certificates \
        curl \
        git \
        unzip \
        zip \
        tmux \
        usbutils \
        libgl1 \
        libglu1-mesa \
        libpulse0 \
        libnss3 \
        libnspr4 \
        libx11-6 \
        libxcb1 \
        libxcomposite1 \
        libxdamage1 \
        libxext6 \
        libxi6 \
        libxrandr2 \
        libxcursor1 \
        libxtst6 \
        libasound2 \
    && rm -rf /var/lib/apt/lists/*

RUN mkdir -p "${ANDROID_HOME}/cmdline-tools" \
    && curl -fsSL -o /tmp/cmdline-tools.zip \
        "https://dl.google.com/android/repository/commandlinetools-linux-${ANDROID_CMDLINE_TOOLS_VERSION}_latest.zip" \
    && unzip -q /tmp/cmdline-tools.zip -d "${ANDROID_HOME}/cmdline-tools" \
    && mv "${ANDROID_HOME}/cmdline-tools/cmdline-tools" "${ANDROID_HOME}/cmdline-tools/latest" \
    && rm /tmp/cmdline-tools.zip

ENV PATH="${ANDROID_HOME}/cmdline-tools/latest/bin:${ANDROID_HOME}/platform-tools:${ANDROID_HOME}/emulator:${PATH}"

RUN yes | sdkmanager --licenses > /dev/null \
    && sdkmanager --install \
        "platform-tools" \
        "platforms;android-${ANDROID_API_LEVEL}" \
        "build-tools;${ANDROID_BUILD_TOOLS_VERSION}" \
        "emulator" \
        "system-images;android-${ANDROID_API_LEVEL};google_apis;x86_64" \
    && rm -rf "${ANDROID_HOME}/.temp" "${ANDROID_HOME}/.downloadIntermediates"

RUN curl -fsSL -o /tmp/gradle.zip "https://services.gradle.org/distributions/gradle-${GRADLE_VERSION}-bin.zip" \
    && echo "${GRADLE_SHA256}  /tmp/gradle.zip" | sha256sum -c - \
    && unzip -q /tmp/gradle.zip -d /opt \
    && ln -s "/opt/gradle-${GRADLE_VERSION}/bin/gradle" /usr/local/bin/gradle \
    && rm /tmp/gradle.zip

RUN chown -R vscode:vscode "${ANDROID_HOME}"
