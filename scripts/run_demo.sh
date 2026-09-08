#!/bin/bash
set -e

DIR="$( cd "$( dirname "${BASH_SOURCE[0]}" )/.." && pwd )"
cd "$DIR"

echo "================================================="
echo "   🚀 LEGACYVAULT ONE-CLICK DEMO LAUNCHER       "
echo "================================================="

# 1. Check/Start Backend
if lsof -Pi :8000 -sTCP:LISTEN -t >/dev/null ; then
    echo "✅ Backend is already active on http://127.0.0.1:8000"
else
    echo "⚙️ Starting Backend server in background..."
    nohup python3 backend/main.py > backend.log 2>&1 &
    sleep 2
    echo "✅ Backend running on http://127.0.0.1:8000"
fi

# Reset to clean demo state
echo "🔄 Resetting vault state for clean demo..."
curl -s -X POST http://127.0.0.1:8000/demo/reset >/dev/null || true

# 2. Check ADB & Device
ADB="/Users/salman_malvasi/Library/Android/sdk/platform-tools/adb"
if [ ! -f "$ADB" ]; then
    ADB="adb"
fi

DEVICE=$($ADB devices | grep -E "device$" | head -n 1 | awk '{print $1}')

if [ -z "$DEVICE" ]; then
    echo "⚠️ No device found. Starting Pixel Emulator..."
    nohup /Users/salman_malvasi/Library/Android/sdk/emulator/emulator -avd Pixel_9_Pro_API_35 -no-snapshot-load -no-boot-anim >/dev/null 2>&1 &
    echo "Waiting for emulator to connect..."
    $ADB wait-for-device
    DEVICE=$($ADB devices | grep -E "device$" | head -n 1 | awk '{print $1}')
fi

echo "📱 Target device: $DEVICE"

# 3. Setup Port Forwarding
echo "🔗 Setting up ADB Reverse (Port 8000)..."
$ADB -s "$DEVICE" reverse tcp:8000 tcp:8000

# 4. Install & Launch
echo "📦 Ensuring latest app is installed and launched..."
APK="$DIR/app/build/outputs/apk/debug/app-debug.apk"
if [ -f "$APK" ]; then
    $ADB -s "$DEVICE" install -r "$APK" >/dev/null 2>&1 || true
fi

$ADB -s "$DEVICE" shell am start -n com.example.legacyvault/com.example.legacyvault.MainActivity
echo "================================================="
echo "🎉 READY! App is open on your Pixel screen."
echo "================================================="
