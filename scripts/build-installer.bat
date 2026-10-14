@echo off
REM ==========================================================================
REM  Gera o instalador Windows (Lumi-1.0.0.exe) com jpackage.
REM  Requisitos: JDK 21 (com jpackage) no PATH, Maven e WiX Toolset 3.x
REM  (https://github.com/wixtoolset/wix3/releases) no PATH.
REM  Uso:  scripts\build-installer.bat          -> instalador .exe
REM        scripts\build-installer.bat msi      -> instalador .msi
REM        scripts\build-installer.bat app-image -> pasta executável (sem WiX)
REM ==========================================================================
setlocal
cd /d "%~dp0\.."

set TYPE=%1
if "%TYPE%"=="" set TYPE=exe

echo [1/2] Compilando e copiando dependencias (mvn package)...
call mvn -B clean package
if errorlevel 1 (
  echo Falha no build Maven.
  exit /b 1
)

echo [2/2] Gerando pacote do tipo %TYPE% com jpackage...
if exist target\installer rmdir /s /q target\installer

set COMMON=--name Lumi --app-version 1.0.0 --vendor "Lumi" ^
 --description "Chat desktop com IA (JavaFX, MongoDB Atlas, RAG e Groq)" ^
 --input target\dist --main-jar lumi.jar --main-class br.com.projeto.Main$Launcher ^
 --icon packaging\icon.ico --dest target\installer ^
 --java-options "-Dfile.encoding=UTF-8" ^
 --add-modules java.base,java.desktop,java.logging,java.management,java.naming,java.net.http,java.prefs,java.scripting,java.security.jgss,java.sql,java.xml,java.xml.crypto,jdk.crypto.ec,jdk.jsobject,jdk.unsupported,jdk.xml.dom,jdk.localedata,jdk.charsets,jdk.naming.dns,jdk.net,jdk.zipfs

if "%TYPE%"=="app-image" (
  jpackage --type app-image %COMMON%
) else (
  jpackage --type %TYPE% %COMMON% ^
   --win-shortcut --win-menu --win-menu-group "Lumi" --win-dir-chooser ^
   --win-shortcut-prompt --win-upgrade-uuid 6f1c2b7e-4d3a-4f6e-9a51-2b8c0e7d4a13
)
if errorlevel 1 (
  echo Falha no jpackage. Verifique se o WiX Toolset 3.x esta instalado e no PATH.
  exit /b 1
)

echo.
echo Pronto! Arquivo gerado em target\installer
dir target\installer
endlocal
