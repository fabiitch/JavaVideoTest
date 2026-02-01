//not readyy


@echo off
setlocal enabledelayedexpansion

rem ==================================================
rem  Resolve project root
rem ==================================================
set SCRIPT_DIR=%~dp0
for %%I in ("%SCRIPT_DIR%\..\..") do set PROJECT_ROOT=%%~fI

rem ==================================================
rem  Configure GStreamer root (LOCAL to script)
rem  >>> ADJUST THIS PATH IF NEEDED <<<
rem ==================================================
if "%GSTREAMER_ROOT%"=="" (
  echo [ERROR] GSTREAMER_ROOT is not set.
  exit /b 1
)

rem ==================================================
rem  Derived paths
rem ==================================================
set OUT=%PROJECT_ROOT%\native\generated
set HEADER=%PROJECT_ROOT%\native\gst\gst_min.h

rem ==================================================
rem  Validate GStreamer install
rem ==================================================
if not exist "%GSTREAMER_ROOT%" (
  echo [ERROR] GStreamer root not found:
  echo         %GSTREAMER_ROOT%
  exit /b 1
)

if not exist "%GSTREAMER_ROOT%\include\gstreamer-1.0\gst\gst.h" (
  echo [ERROR] GStreamer headers not found.
  echo         Expected: %GSTREAMER_ROOT%\include\gstreamer-1.0\gst\gst.h
  exit /b 1
)

if not exist "%GSTREAMER_ROOT%\lib\gstreamer-1.0\include\gst\gl\gstglconfig.h" (
  echo [ERROR] GStreamer GL headers not found.
  echo         Expected: %GSTREAMER_ROOT%\lib\gstreamer-1.0\include\gst\gl\gstglconfig.h
  exit /b 1
)

if not exist "%HEADER%" (
  echo [ERROR] Header not found:
  echo         %HEADER%
  exit /b 1
)

rem ==================================================
rem  Clean output
rem ==================================================
if exist "%OUT%" rmdir /s /q "%OUT%"
mkdir "%OUT%"

rem ==================================================
rem  Run jextract
rem ==================================================
echo [INFO] Running jextract...
echo.

jextract ^
  --target-package com.nz.media.gst.panama ^
  --output "%OUT%" ^
  -D NZ_API= ^
  -I "%GSTREAMER_ROOT%\include" ^
  -I "%GSTREAMER_ROOT%\include\gstreamer-1.0" ^
  -I "%GSTREAMER_ROOT%\include\glib-2.0" ^
  -I "%GSTREAMER_ROOT%\lib\glib-2.0\include" ^
  -I "%GSTREAMER_ROOT%\lib\gstreamer-1.0\include" ^
  --include-function "nz_.*" ^
  "%HEADER%"

if errorlevel 1 (
  echo.
  echo [ERROR] jextract failed
  exit /b 1
)

echo.
echo [OK] jextract completed successfully
echo      Output: %OUT%
endlocal
