# Obsidian Wealth 💎

Obsidian Wealth is an elite, privacy-first personal finance and wealth management Android application designed with modern Material Design 3 and Jetpack Compose. It empowers users with local-first data tracking, deep financial analytics, dynamic regional localization, and autonomous AI insights.

## Features

- **Cash Flow Intelligence**: Real-time tracking of incomes, categorized expenses, and automated balance calculation with interactive visual breakdowns.
- **Investments & Portfolio Management**: Track equities (stocks, ETFs), Money Market Funds (MMFs), Treasury bonds, mutual funds, gold, and crypto alongside automated monthly SIP investments.
- **Debt & Credit Optimization**: Credit card utilization monitoring, loan amortizations, and automated debt payoff velocity algorithms (Debt Avalanche and Snowball strategies).
- **Goals & Wealth Audits**: Milestone tracking with progress rings and one-tap generation of comprehensive audited financial statements.
- **Dynamic Localization**: Built-in support for multiple global currencies and regional profiles (defaulting to Kenyan Shillings `KES / KSh` and East Africa, with seamless toggling for USD, GBP, EUR, INR, ZAR, and more).
- **Customizable Modules**: Toggle any feature module on or off according to personal preferences via the top 3-dash menu.
- **Autonomous AI Advisor**: Embedded financial advisory engine with natural language queries and offline rule-based fallback mode.
- **Discreet Vault Mode**: Instant balance masking to protect sensitive figures in public settings.

---

## Local Development & Debug Build

### Prerequisites
- **Android Studio** Ladybug (or newer)
- **JDK 17** (or JDK 21)
- Android SDK with API 36 / API 34 installed

### Building the Debug APK Locally
The repository includes the pre-configured Gradle wrapper, requiring no manual Gradle installation:

```bash
# Clone the repository
git clone <your-repo-url>
cd <repo-name>

# Ensure executable permissions on Unix/macOS/Linux
chmod +x gradlew

# Build the debug APK
./gradlew assembleDebug
```

On Windows (Command Prompt / PowerShell):
```cmd
gradlew.bat assembleDebug
```

The compiled APK will be output to:
```
app/build/outputs/apk/debug/app-debug.apk
```

---

## Continuous Integration (CI) on GitHub Actions

This repository includes a pre-configured GitHub Actions workflow (`.github/workflows/build.yml`) that automatically builds and verifies the debug APK on every push and pull request to the `main` branch.

### Downloading the Debug APK from GitHub Actions:
1. Navigate to your repository on GitHub.
2. Click on the **Actions** tab at the top.
3. Select the latest workflow run under **Android CI (Debug Build)**.
4. Scroll down to the **Artifacts** section at the bottom of the summary page.
5. Click **obsidian-wealth-debug-apk** to download the zip file containing the ready-to-install debug APK.

---

## Release & Play Store Signing

> **Note on Signing**: The repository is currently configured exclusively for debug builds and continuous integration. Production and Google Play Store release signing configurations (upload keystore, key passwords, and Google Play App Bundle publishing) will be configured when preparing for production deployment.
