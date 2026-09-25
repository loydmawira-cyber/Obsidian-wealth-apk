# Obsidian Wealth: Finance and Ledger Review

## Result

The app now uses account-linked cash balances and currency-aware summaries rather than treating transaction amounts as one interchangeable pool. Unknown-currency records are kept for review and are not silently converted or presented as the selected display currency. The work does not add bank connectivity, execute payments, or retrieve live market prices.

## Accounting behavior

An account balance is its explicitly confirmed opening balance plus confirmed, linked ledger activity after the opening timestamp. Unconfirmed legacy opening amounts are excluded until a user reviews and confirms them. Reconciliation records the statement-to-ledger difference as an adjustment rather than operating income or spending.

The old global starting-cash amount is preserved as an unresolved legacy account rather than assigned the current display currency. Its amount is excluded from balances until the user confirms both the account currency and opening amount. Region sample data now receives explicit regional currency and account-link metadata when the designated demo/reset flow seeds it.

A transfer is recorded as an outgoing transaction and a matching incoming transaction in one Room transaction. The write checks both accounts, account activity, same-currency matching, equal positive finite amounts, transfer classification, and available funds before inserting either row. Transfers therefore change the two account balances without counting as income or spending. FX transfers are not supported.

Cash-flow rows use the transaction's stored currency or, when that is absent, the linked account's currency. Daily summaries are grouped by date and currency. If currency is unknown, no combined daily total is shown. Overview totals disclose unresolved currency data, while record-level review remains available in the account, investment, and debt screens.

Changing a regional preset changes presentation preferences only. It no longer reseeds or clears financial records. Amounts retain their record currency and are not converted when display preferences change.

## Regression coverage added

`AccountLedgerTest.kt` now covers the opening-balance timestamp boundary, exclusion of an unconfirmed legacy opening amount, transfer effects on both account balances while preserving the same-currency aggregate, separation of currencies and inactive accounts, and reconciliation corrections without mutation of prior entries. Existing tests also cover confirmed transaction filtering and unknown-currency completeness.

## Validation status

The final whitespace scan of the reviewed source files passed, the Gradle wrapper passed `bash -n`, and the source archive passed `zip -T`. The Gradle unit-test task was attempted with `bash ./gradlew testDebugUnitTest --no-daemon`. It stopped during Android task configuration because this environment has no Android SDK (`ANDROID_HOME` and `sdk.dir` are unset). Therefore the tests are present but are **not claimed as passing**, and Kotlin/Room compilation remains unverified. Build and runtime validation should be run in Android Studio or CI with the project's configured SDK platforms.

## Key implementation files

- [Account ledger calculations][1]
- [Room transfer and reconciliation methods][2]
- [Room schema and migrations][3]
- [Finance summary and account operations][4]
- [Cash-flow currency presentation][5]
- [Account review UI][6]
- [Ledger regression tests][7]

## References

[1]: app/src/main/java/com/example/data/models/AccountLedger.kt "Account ledger calculations"
[2]: app/src/main/java/com/example/data/dao/FinanceDao.kt "Room transfer and reconciliation methods"
[3]: app/src/main/java/com/example/data/database/AppDatabase.kt "Room schema and migrations"
[4]: app/src/main/java/com/example/ui/viewmodel/FinanceViewModel.kt "Finance summaries and account operations"
[5]: app/src/main/java/com/example/ui/screens/CashFlowScreen.kt "Cash-flow currency presentation"
[6]: app/src/main/java/com/example/ui/screens/AccountManagementComponents.kt "Account review and balance confirmation UI"
[7]: app/src/test/java/com/example/AccountLedgerTest.kt "Account ledger regression tests"
