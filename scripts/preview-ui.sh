#!/usr/bin/env bash
# Abre as telas com dados em memória e salva capturas em target/preview (Linux + Xvfb).
set -euo pipefail
cd "$(dirname "$0")/.."
mvn -q -B test-compile dependency:build-classpath -Dmdep.outputFile=target/cp.txt -Dmdep.includeScope=test
CP="target/classes:target/test-classes:$(cat target/cp.txt)"
xvfb-run -a -s "-screen 0 1400x900x24" java -Dprism.order=sw -cp "$CP" br.com.projeto.support.UiPreviewLauncher
