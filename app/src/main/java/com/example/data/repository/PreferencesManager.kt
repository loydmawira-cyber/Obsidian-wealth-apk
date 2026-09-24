package com.example.data.repository

import android.content.Context
import android.content.SharedPreferences
import com.example.data.models.AiRiskProfile
import com.example.data.models.FiscalCalendar
import com.example.data.models.GeographicRegion
import com.example.data.models.NumberFormatStyle
import com.example.data.models.SupportedCurrency
import com.example.data.models.UserSettings
import com.example.data.security.PinCredentialStore
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

class PreferencesManager(context: Context) {
    private val prefs: SharedPreferences =
        context.getSharedPreferences("obsidian_wealth_prefs_v3", Context.MODE_PRIVATE)

    /** Hashed PIN storage with attempt limiting. See [PinCredentialStore]. */
    val pinStore: PinCredentialStore = PinCredentialStore(prefs)

    private val _settings = MutableStateFlow(loadSettings())
    val settings: StateFlow<UserSettings> = _settings

    init {
        purgeLegacyPlaintextSecrets()
    }

    /**
     * Deletes plaintext secrets written by earlier versions.
     *
     * Removing the accessor code is not enough: every device that already ran a previous build
     * still has `pref_user_password` (the account password in cleartext) and `pref_quick_pin`
     * (the unlock PIN in cleartext) sitting in the prefs XML. They would stay there forever,
     * readable on a rooted device or through any backup, long after the code that wrote them
     * was gone. This runs on every construction so the values are gone on first launch after
     * upgrade, and is cheap enough to leave in place permanently.
     *
     * A legacy plaintext PIN is NOT migrated into the hashed store. Re-deriving it would mean
     * reading the cleartext value to re-encode it, and the user is prompted to set a new PIN
     * instead — a one-time inconvenience that avoids handling the plaintext at all.
     */
    private fun purgeLegacyPlaintextSecrets() {
        val hadLegacyPin = !prefs.getString(LEGACY_KEY_QUICK_PIN, null).isNullOrBlank()
        val editor = prefs.edit()
            .remove(LEGACY_KEY_USER_PASSWORD)
            .remove(LEGACY_KEY_QUICK_PIN)
            // Notification-reading feature was removed; drop its stored app list.
            .remove("pref_trusted_notification_packages")

        // A legacy PIN was enabled but its plaintext is now gone and nothing was migrated, so
        // the enabled flag would point at an unverifiable PIN. Turn it off rather than leave a
        // lock the user cannot satisfy.
        if (hadLegacyPin && !pinStore.isPinSet()) {
            editor.putBoolean(KEY_PIN_ENABLED, false)
        }
        editor.apply()
    }

    private fun loadSettings(): UserSettings {
        val currencyCode = prefs.getString(KEY_CURRENCY, SupportedCurrency.KES.name) ?: SupportedCurrency.KES.name
        val currency = try { SupportedCurrency.valueOf(currencyCode) } catch (e: Exception) { SupportedCurrency.KES }

        val regionId = prefs.getString(KEY_REGION, GeographicRegion.EAST_AFRICA.id) ?: GeographicRegion.EAST_AFRICA.id
        val region = GeographicRegion.values().find { it.id == regionId } ?: GeographicRegion.EAST_AFRICA

        val fiscalName = prefs.getString(KEY_FISCAL, FiscalCalendar.MID_YEAR.name) ?: FiscalCalendar.MID_YEAR.name
        val fiscal = try { FiscalCalendar.valueOf(fiscalName) } catch (e: Exception) { FiscalCalendar.MID_YEAR }

        val numberStyleName = prefs.getString(KEY_NUMBER_STYLE, NumberFormatStyle.WESTERN_STANDARD.name) ?: NumberFormatStyle.WESTERN_STANDARD.name
        val numberStyle = try { NumberFormatStyle.valueOf(numberStyleName) } catch (e: Exception) { NumberFormatStyle.WESTERN_STANDARD }

        val enableCashFlow = prefs.getBoolean(KEY_ENABLE_CASH_FLOW, true)
        val enableInvestments = prefs.getBoolean(KEY_ENABLE_INVESTMENTS, true)
        val enableDebtCenter = prefs.getBoolean(KEY_ENABLE_DEBT_CENTER, true)
        val enableGoals = prefs.getBoolean(KEY_ENABLE_GOALS, true)

        val showAiAdvisor = prefs.getBoolean(KEY_SHOW_AI_ADVISOR, true)
        val aiRiskName = prefs.getString(KEY_AI_RISK, AiRiskProfile.BALANCED_INSTITUTIONAL.name) ?: AiRiskProfile.BALANCED_INSTITUTIONAL.name
        val aiRisk = try { AiRiskProfile.valueOf(aiRiskName) } catch (e: Exception) { AiRiskProfile.BALANCED_INSTITUTIONAL }

        val hideBalances = prefs.getBoolean(KEY_HIDE_BALANCES, false)
        val biometric = prefs.getBoolean(KEY_BIOMETRIC, true)

        val enableNotifications = prefs.getBoolean(KEY_ENABLE_NOTIFICATIONS, true)
        val enableBillDue = prefs.getBoolean(KEY_ENABLE_BILL_DUE, true)
        val enableSip = prefs.getBoolean(KEY_ENABLE_SIP, true)
        val enableDailyBriefing = prefs.getBoolean(KEY_ENABLE_DAILY_BRIEFING, true)

        return UserSettings(
            currency = currency,
            region = region,
            fiscalCalendar = fiscal,
            numberFormatStyle = numberStyle,
            enableCashFlow = enableCashFlow,
            enableInvestments = enableInvestments,
            enableDebtCenter = enableDebtCenter,
            enableGoals = enableGoals,
            showFloatingAiAdvisor = showAiAdvisor,
            aiRiskProfile = aiRisk,
            hideBalances = hideBalances,
            biometricProtection = biometric,
            enableNotifications = enableNotifications,
            enableBillDueReminders = enableBillDue,
            enableSipReminders = enableSip,
            enableDailyBriefingReminders = enableDailyBriefing
        )
    }

    fun updateSettings(newSettings: UserSettings) {
        prefs.edit()
            .putString(KEY_CURRENCY, newSettings.currency.name)
            .putString(KEY_REGION, newSettings.region.id)
            .putString(KEY_FISCAL, newSettings.fiscalCalendar.name)
            .putString(KEY_NUMBER_STYLE, newSettings.numberFormatStyle.name)
            .putBoolean(KEY_ENABLE_CASH_FLOW, newSettings.enableCashFlow)
            .putBoolean(KEY_ENABLE_INVESTMENTS, newSettings.enableInvestments)
            .putBoolean(KEY_ENABLE_DEBT_CENTER, newSettings.enableDebtCenter)
            .putBoolean(KEY_ENABLE_GOALS, newSettings.enableGoals)
            .putBoolean(KEY_SHOW_AI_ADVISOR, newSettings.showFloatingAiAdvisor)
            .putString(KEY_AI_RISK, newSettings.aiRiskProfile.name)
            .putBoolean(KEY_HIDE_BALANCES, newSettings.hideBalances)
            .putBoolean(KEY_BIOMETRIC, newSettings.biometricProtection)
            .putBoolean(KEY_ENABLE_NOTIFICATIONS, newSettings.enableNotifications)
            .putBoolean(KEY_ENABLE_BILL_DUE, newSettings.enableBillDueReminders)
            .putBoolean(KEY_ENABLE_SIP, newSettings.enableSipReminders)
            .putBoolean(KEY_ENABLE_DAILY_BRIEFING, newSettings.enableDailyBriefingReminders)
            .apply()

        _settings.value = newSettings
    }

    fun applyRegionPreset(region: GeographicRegion) {
        val current = _settings.value
        val updated = current.copy(
            region = region,
            currency = region.defaultCurrency,
            fiscalCalendar = region.defaultFiscal,
            numberFormatStyle = region.defaultNumberStyle
        )
        updateSettings(updated)
    }

    fun updateCurrency(currency: SupportedCurrency) {
        updateSettings(_settings.value.copy(currency = currency))
    }

    fun toggleHideBalances() {
        updateSettings(_settings.value.copy(hideBalances = !_settings.value.hideBalances))
    }

    fun toggleModule(module: String, enabled: Boolean) {
        val current = _settings.value
        val updated = when (module) {
            "cash_flow" -> current.copy(enableCashFlow = enabled)
            "investments" -> current.copy(enableInvestments = enabled)
            "debt" -> current.copy(enableDebtCenter = enabled)
            "goals" -> current.copy(enableGoals = enabled)
            "ai_advisor" -> current.copy(showFloatingAiAdvisor = enabled)
            else -> current
        }
        updateSettings(updated)
    }

    // getCloudVaultId()/setCloudVaultId() were removed deliberately.
    //
    // The old getter minted an id as "obsidian_vault_" + (1000..9999).random() — a 9,000-value
    // guessable namespace — and persisted it. Combined with a restore path that accepted any
    // vault id, that was a direct route to reading another user's financial records.
    // The vault id is now always the Firebase Auth uid, derived at read time by
    // FirestoreSyncManager from the signed-in user. It is not persisted and not settable.

    fun getLastCloudSyncTime(): Long {
        return prefs.getLong(KEY_LAST_CLOUD_SYNC, 0L)
    }

    fun setLastCloudSyncTime(timestamp: Long) {
        prefs.edit().putLong(KEY_LAST_CLOUD_SYNC, timestamp).apply()
    }

    // AUTH & QUICK 4-DIGIT PIN METHODS
    fun isLoggedIn(): Boolean {
        return prefs.getBoolean(KEY_IS_LOGGED_IN, false)
    }

    fun setLoggedIn(loggedIn: Boolean) {
        prefs.edit().putBoolean(KEY_IS_LOGGED_IN, loggedIn).apply()
    }

    fun getUserEmail(): String {
        return prefs.getString(KEY_USER_EMAIL, "") ?: ""
    }

    fun setUserEmail(email: String) {
        prefs.edit().putString(KEY_USER_EMAIL, email).apply()
    }

    // Account password is NEVER persisted locally.
    //
    // getUserPassword()/setUserPassword() and the `pref_user_password` key were removed. They
    // wrote the account password to SharedPreferences in cleartext. Firebase Auth already holds
    // the session — it issues and refreshes its own tokens, so the app has no reason to keep the
    // password at all. Anything that appears to need it wants a re-authentication prompt
    // (`FirebaseUser.reauthenticate`) instead.
    // Existing cleartext values are deleted by purgeLegacyPlaintextSecrets().

    // PIN is stored hashed via `pinStore`, never in plaintext.
    // getQuickPin()/setQuickPin() and the `pref_quick_pin` key were removed; use
    // pinStore.setPin() / pinStore.verify() / pinStore.clear().

    fun isPinEnabled(): Boolean {
        return prefs.getBoolean(KEY_PIN_ENABLED, false) && pinStore.isPinSet()
    }

    fun setPinEnabled(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_PIN_ENABLED, enabled).apply()
    }

    // CLOUD DELETIONS: items the user deleted on this device that still need removing from the cloud.
    // Entries look like "transactions/42". Only explicit local deletions are ever queued, so a new
    // or empty device can never wipe a cloud vault.
    fun getPendingCloudDeletions(): Set<String> =
        (prefs.getStringSet(KEY_PENDING_CLOUD_DELETIONS, emptySet()) ?: emptySet()).toSet()

    fun addPendingCloudDeletions(entries: Collection<String>) {
        if (entries.isEmpty()) return
        val updated = getPendingCloudDeletions().toMutableSet().apply { addAll(entries) }
        prefs.edit().putStringSet(KEY_PENDING_CLOUD_DELETIONS, updated).apply()
    }

    fun removePendingCloudDeletions(entries: Collection<String>) {
        val updated = getPendingCloudDeletions().toMutableSet().apply { removeAll(entries.toSet()) }
        prefs.edit().putStringSet(KEY_PENDING_CLOUD_DELETIONS, updated).apply()
    }

    fun clearPendingCloudDeletions() {
        prefs.edit().remove(KEY_PENDING_CLOUD_DELETIONS).apply()
    }

    // CASH STARTING BALANCE (set by each user; 0 until they choose to enter one)
    fun getStartingBalance(): Double =
        java.lang.Double.longBitsToDouble(prefs.getLong(KEY_STARTING_BALANCE, java.lang.Double.doubleToRawLongBits(0.0)))

    fun setStartingBalance(value: Double) {
        prefs.edit().putLong(KEY_STARTING_BALANCE, java.lang.Double.doubleToRawLongBits(value)).apply()
    }

    fun isStartingBalancePromptDone(): Boolean = prefs.getBoolean(KEY_STARTING_BALANCE_PROMPT_DONE, false)

    fun setStartingBalancePromptDone(done: Boolean) {
        prefs.edit().putBoolean(KEY_STARTING_BALANCE_PROMPT_DONE, done).apply()
    }

    // DEMO/SEED DATA (restricted to a single designated account)
    fun hasAutoSeededDemoData(): Boolean {
        return prefs.getBoolean(KEY_DEMO_SEEDED, false)
    }

    fun setAutoSeededDemoData(seeded: Boolean) {
        prefs.edit().putBoolean(KEY_DEMO_SEEDED, seeded).apply()
    }

    // Tracks which Firebase account last used this device's local database,
    // so we can detect an account switch and avoid leaking one account's
    // local data into another account's session.
    fun getLastSignedInUid(): String {
        return prefs.getString(KEY_LAST_SIGNED_IN_UID, "") ?: ""
    }

    fun setLastSignedInUid(uid: String) {
        prefs.edit().putString(KEY_LAST_SIGNED_IN_UID, uid).apply()
    }

    companion object {
        private const val KEY_CURRENCY = "pref_currency"
        private const val KEY_REGION = "pref_region"
        private const val KEY_FISCAL = "pref_fiscal"
        private const val KEY_NUMBER_STYLE = "pref_number_style"
        private const val KEY_ENABLE_CASH_FLOW = "pref_enable_cash_flow"
        private const val KEY_ENABLE_INVESTMENTS = "pref_enable_investments"
        private const val KEY_ENABLE_DEBT_CENTER = "pref_enable_debt_center"
        private const val KEY_ENABLE_GOALS = "pref_enable_goals"
        private const val KEY_SHOW_AI_ADVISOR = "pref_show_ai_advisor"
        private const val KEY_AI_RISK = "pref_ai_risk"
        private const val KEY_HIDE_BALANCES = "pref_hide_balances"
        private const val KEY_BIOMETRIC = "pref_biometric"
        private const val KEY_LAST_CLOUD_SYNC = "pref_last_cloud_sync"
        private const val KEY_IS_LOGGED_IN = "pref_is_logged_in"
        private const val KEY_USER_EMAIL = "pref_user_email"
        private const val KEY_PIN_ENABLED = "pref_pin_enabled"
        private const val KEY_DEMO_SEEDED = "pref_demo_seeded"
        private const val KEY_PENDING_CLOUD_DELETIONS = "pref_pending_cloud_deletions"
        private const val KEY_STARTING_BALANCE = "pref_cash_starting_balance"
        private const val KEY_STARTING_BALANCE_PROMPT_DONE = "pref_cash_starting_balance_prompt_done"
        private const val KEY_LAST_SIGNED_IN_UID = "pref_last_signed_in_uid"
        private const val KEY_ENABLE_NOTIFICATIONS = "pref_enable_notifications"
        private const val KEY_ENABLE_BILL_DUE = "pref_enable_bill_due"
        private const val KEY_ENABLE_SIP = "pref_enable_sip"
        private const val KEY_ENABLE_DAILY_BRIEFING = "pref_enable_daily_briefing"

        // Retained only so purgeLegacyPlaintextSecrets() can delete what older builds wrote.
        // Never read these values; never write them again.
        private const val LEGACY_KEY_USER_PASSWORD = "pref_user_password"
        private const val LEGACY_KEY_QUICK_PIN = "pref_quick_pin"
    }
}
