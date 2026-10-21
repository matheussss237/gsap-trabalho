#!/usr/bin/env bash
# Gera um pacote da aplicação no Linux/macOS (app-image por padrão; deb/rpm/dmg/pkg se informado).
# O instalador oficial do trabalho é o Windows (scripts/build-installer.bat).
set -euo pipefail
cd "$(dirname "$0")/.."
TYPE="${1:-app-image}"
mvn -B clean package
rm -rf target/installer
jpackage --type "$TYPE" --name Lumi --app-version 1.0.0 --vendor "Lumi" \
  --input target/dist --main-jar lumi.jar --main-class com.matheushenrique.lumi.LumiApplication \
  --icon packaging/icon.png --dest target/installer --java-options "-Dfile.encoding=UTF-8" \
  --add-modules java.base,java.desktop,java.logging,java.management,java.naming,java.net.http,java.prefs,java.scripting,java.security.jgss,java.sql,java.xml,java.xml.crypto,jdk.crypto.ec,jdk.jsobject,jdk.unsupported,jdk.xml.dom,jdk.localedata,jdk.charsets,jdk.naming.dns,jdk.net,jdk.zipfs
ls -la target/installer
