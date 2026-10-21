#!/usr/bin/env bash
# Roda a Lumi inteira com MongoDB e Groq falsos e salva prints em target/preview (Linux + Xvfb).
set -euo pipefail
cd "$(dirname "$0")/.."
mvn -q -B test-compile dependency:build-classpath -Dmdep.outputFile=target/cp.txt -Dmdep.includeScope=test
CP="target/classes:target/test-classes:$(cat target/cp.txt)"
xvfb-run -a -s "-screen 0 1400x900x24" java -Dprism.order=sw -cp "$CP" 'com.matheushenrique.lumi.PreviewTelas$Iniciar'
