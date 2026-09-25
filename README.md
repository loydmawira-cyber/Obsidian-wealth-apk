# Obsidian Wealth

Obsidian Wealth is an Android app for manually recording personal cash flow, accounts, investments, debts, and savings goals. Its balances and summaries are based on information entered or imported by the user; the app does not connect to banks or move money.

## What the app does

- **Cash accounts and ledger:** Record account opening balances, income, expenses, and statement reconciliations. Balances are calculated from the opening balance and confirmed, account-linked entries.
- **Transfers:** Record a same-currency transfer as a linked debit and credit. The pair is written together and excluded from income and spending totals. Cross-currency conversion is not supported.
- **Multi-currency records:** Accounts and financial records retain currency metadata. Totals are calculated only within one currency; the app does not infer exchange rates or convert values. Legacy records with unknown currency are excluded from currency-specific totals until reviewed.
- **Investments:** Maintain user-entered holding quantities, cost, and prices. These values are not represented as live or independently verified market data. Recurring investment plans are reminders; a plan does not create a debit or investment holding.
- **Debt and goals:** Record user-maintained card, loan, and goal information. Payments and contributions must be explicitly recorded; these figures are not institution-verified.
- **Reports:** Reports are personal summaries, not audited financial statements or attestations.
- **Privacy controls:** The app includes local balance masking and Firebase-backed account/cloud functionality as configured by the project.

## Accounting notes

An account's current balance is its explicitly confirmed opening balance plus confirmed linked income and adjustments, less confirmed linked expenses. Unconfirmed legacy opening amounts remain excluded until reviewed. Transfer entries affect the balances of their two accounts but do not count as income or spending. Reconciliation records the difference between the ledger balance and the statement balance as an adjustment rather than operating income or expense.

Unresolved currency values remain visible for review but are not silently assigned the selected display currency. Confirming a currency labels the existing amount; it does not convert that amount. Summary figures cover the selected display currency and can be incomplete while unresolved records remain.

Changing a regional preset changes presentation preferences only. It does not reseed or clear financial records, and stored amounts are not converted when display preferences change.

## Build and test

### Prerequisites

- Android Studio and Android SDK platforms required by the project (API 34 and API 36)
- JDK 17 or newer

### Build a debug APK

```bash
./gradlew assembleDebug
```

On Windows, run `gradlew.bat assembleDebug`. The debug APK is written to `app/build/outputs/apk/debug/app-debug.apk`.

### Run unit tests

```bash
./gradlew testDebugUnitTest
```

A local Android SDK is required for Gradle to configure the Android test tasks. In this review environment, unit tests could not run because no Android SDK was installed.

## Firebase configuration

The project includes its Firebase Android client configuration. Before distributing a build, verify that the Firebase project, package name, enabled services, Firestore rules, and API-key restrictions match the intended deployment.

## Known limitations

The app does not provide bank connectivity, automatic payment execution, automatic investment execution, or foreign-exchange conversion. Prices, account balances, debt figures, and goal amounts are maintained by the user. Imported or legacy records may require review before they are included in totals.

## References

The accounting behavior and safeguards are implemented in the source files [1] [2] [3].

[1]: app/src/main/java/com/example/data/models/AccountLedger.kt "Account ledger calculations"
[2]: app/src/main/java/com/example/data/database/AppDatabase.kt "Room schema and migrations"
[3]: app/src/main/java/com/example/data/dao/FinanceDao.kt "Finance data access and atomic transfer writes"

## Debt-funded cash-flow activity

The Debt tab supports confirmed credit-card purchases and loan top-ups. A card purchase is accepted only when the selected card has enough available credit; it increases the card balance and appears as an expense in the cash-flow ledger and expense/budget views without reducing a cash account. A loan top-up records the borrowed proceeds in a same-currency deposit account, increases both the loan's principal and outstanding balance, and appears in monthly cash flow as an inflow while remaining excluded from operating-income summaries. Both actions require explicit confirmation, are committed atomically with their linked ledger entry, and cannot be edited or deleted independently from the ledger. Their debt links are included in Firestore sync and Room migration 9-to-10.

