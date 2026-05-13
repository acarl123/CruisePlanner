@echo off
echo Getting SHA-1 fingerprint for debug keystore...
echo.

REM Try common Java locations
set "JAVA_LOCATIONS=C:\Program Files\Android\Android Studio\jbr\bin;C:\Program Files\Java\jdk*\bin;C:\Program Files\Eclipse Adoptium\*\bin"

for %%i in (%JAVA_LOCATIONS%) do (
    if exist "%%i\keytool.exe" (
        echo Found keytool at: %%i
        "%%i\keytool.exe" -list -v -keystore "%USERPROFILE%\.android\debug.keystore" -alias androiddebugkey -storepass android -keypass android | findstr /i "SHA1 SHA256"
        echo.
        echo Copy the SHA1 fingerprint above and add it to your Google Cloud Console:
        echo https://console.cloud.google.com/google/maps-apis/credentials
        goto :done
    )
)

echo Could not find keytool. Please use Android Studio method instead.
echo.
echo In Android Studio:
echo 1. Open Gradle panel on the right
echo 2. Navigate to: app ^> Tasks ^> android ^> signingReport
echo 3. Double-click to run
echo 4. Copy the SHA1 fingerprint from the output

:done
pause
