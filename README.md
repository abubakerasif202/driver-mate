# DriverMate Assistant 🚖✨

DriverMate Assistant is a premium rideshare and delivery driver companion built with Jetpack Compose. It features a bespoke, high-contrast **Obsidian Black & Gold theme** designed for optimal visibility during day and night shifts.

Drivers can dynamically track their active shifts, evaluate trip tickets on the fly (calculating hourly and per-kilometer metrics against personal targets), log their driving history locally via Room, view advanced Canvas-rendered weekly income charts, and export everything to CSV format for hassle-free tax and income reporting.

---

## 🛠️ Prerequisites

Before building the application, ensure you have the following installed on your development machine:

1. **Java Development Kit (JDK):** JDK 17 or higher is required.
2. **Android SDK:** Available via Android Studio.
3. **Android Studio (Recommended):** Ladybug (2024.1.3) or newer.

---

## 💻 Build Instructions: Android Studio

Building DriverMate Assistant through Android Studio's graphical user interface is the most seamless method.

1. **Open the Project:**
   - Launch Android Studio.
   - Click on **File > Open...** (or **Open an Existing Project** from the Welcome screen).
   - Navigate to and select the root directory of this project.
   - Wait for Gradle to finish syncing and indexing files.

2. **Run on a Device or Emulator:**
   - Connect your physical Android device via USB (with **USB Debugging** enabled in Developer Options) or start a Virtual Device (AVD).
   - Select your target device in the device dropdown menu on the toolbar.
   - Click the green **Run (Play)** button, or press `Shift + F10` (`Control + R` on macOS).

3. **Build the Debug APK:**
   - On the top menu bar, navigate to **Build > Build Bundle(s) / APK(s) > Build APK(s)**.
   - Android Studio will compile the app. Once finished, a notification popup will appear at the bottom-right corner.
   - Click **locate** inside the notification to open your system's file explorer directly to the directory containing the fresh `app-debug.apk` file.

4. **Build the Signed/Release APK/AAB (for Store Deployment):**
   - Navigate to **Build > Generate Signed Bundle / APK...**
   - Select **APK** or **Android App Bundle** and click **Next**.
   - Point to your secure Keystore path, input your keystore/key aliases and passwords, and select **Release** build variant.
   - Click **Finish** to build.

---

## ⚡ Build Instructions: Windows PowerShell

If you prefer using the command line or are automating your build workflows, you can compile the APK directly in Windows PowerShell using the Gradle Wrapper.

### Step 1: Open PowerShell in the Project Root
1. Open Windows Explorer, go to the folder where the project is located.
2. Hold `Shift` and **Right-Click** in an empty space.
3. Select **Open PowerShell window here** (or open PowerShell manually and use `cd "C:\path\to\your\project"`).

### Step 2: Ensure Execution Permissions (One-Time Setup)
By default, Windows PowerShell restricts the execution of local scripts. If you encounter permission errors when running `./gradlew`, lift the restriction for your current session:
```powershell
Set-ExecutionPolicy -ExecutionPolicy Bypass -Scope Process
```

### Step 3: Run the Build Command
To compile the debug version of the APK, execute the local Gradle Wrapper:
```powershell
.\gradlew.bat assembleDebug
```
*(Alternatively, you can use `./gradlew assembleDebug` if using a UNIX shell environment).*

### Step 4: Locate Your APK
Once the task finishes with `BUILD SUCCESSFUL`, your generated debug APK will be located in the following relative directory:
```
app\build\outputs\apk\debug\app-debug.apk
```

### Useful PowerShell Build Tasks:
- **Clean the build cache:**
  ```powershell
  .\gradlew.bat clean
  ```
- **Compile and Run all Unit Tests:**
  ```powershell
  .\gradlew.bat testDebugUnitTest
  ```
- **Build a Release (Unsigned) APK:**
  ```powershell
  .\gradlew.bat assembleRelease
  ```

---

## 🎨 Design Theme & Core Architecture
* **Theme Scheme**: Charcoal and deep obsidian backgrounds (`#08080A`) contrasted with lustrous, golden brand accents (`#E5A93B`, `#FFD700`).
* **Visual Polish**: Modern Material Design 3 spacing and density guidelines, adaptive dynamic Canvas graphs, and smooth ripple click animations.
* **Local Persistence**: Full integration of Jetpack Room Database for secure offline trip records storage.
* **Service Overlay**: Includes an interactive background service (`OverlayService`) allowing drivers to open a floating, draggable calculator bubble above other rideshare apps.
