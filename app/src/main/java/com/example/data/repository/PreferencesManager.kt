package com.example.data.repository

import android.content.Context
import android.content.SharedPreferences
import com.example.data.models.AiRiskProfile
import com.example.data.models.FiscalCalendar
import com.example.data.models.GeographicRegion
import com.example.data.models.NumberFormatStyle
import com.example.data.models.SupportedCurrency
import com.example.data.models.UserSettings
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

class PreferencesManager(context: Context) {
    private val prefs: SharedPreferences =
        context.getSharedPreferences("obsidian_wealth_prefs_v3", Context.MODE_PRIVATE)

    private val _settings = MutableStateFlow(loadSettings())
    val settings: StateFlow<UserSettings> = _settings

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
            biometricProtection = biometric
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

    fun getCloudVaultId(): String {
        val saved = prefs.getString(KEY_CLOUD_VAULT_ID, "") ?: ""
        if (saved.isNotBlank()) return saved
        val generated = "obsidian_vault_" + (1000..9999).random()
        setCloudVaultId(generated)
        return generated
    }

    fun setCloudVaultId(id: String) {
        prefs.edit().putString(KEY_CLOUD_VAULT_ID, id).apply()
    }

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

    fun getUserPassword(): String {
        return prefs.getString(KEY_USER_PASSWORD, "") ?: ""
    }

    fun setUserPassword(password: String) {
        prefs.edit().putString(KEY_USER_PASSWORD, password).apply()
    }

    fun getQuickPin(): String {
        return prefs.getString(KEY_QUICK_PIN, "") ?: ""
    }

    fun setQuickPin(pin: String) {
        prefs.edit().putString(KEY_QUICK_PIN, pin).apply()
    }

    fun isPinEnabled(): Boolean {
        return prefs.getBoolean(KEY_PIN_ENABLED, false) && getQuickPin().length == 4
    }

    fun setPinEnabled(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_PIN_ENABLED, enabled).apply()
    }

    // DEMO/SEED DATA (restricted to a single designated account)
    fun hasAutoSeededDemoData(): Boolean {
        return prefs.getBoolean(KEY_DEMO_SEEDED, false)
    }

    fun setAutoSeededDemoData(seeded: Boolean) {
        prefs.edit().putBoolean(KEY_DEMO_SEEDED, seeded).apply()
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
        private const val KEY_CLOUD_VAULT_ID = "pref_cloud_vault_id"
        private const val KEY_LAST_CLOUD_SYNC = "pref_last_cloud_sync"
        private const val KEY_IS_LOGGED_IN = "pref_is_logged_in"
        private const val KEY_USER_EMAIL = "pref_user_email"
        private const val KEY_USER_PASSWORD = "pref_user_password"
        private const val KEY_QUICK_PIN = "pref_quick_pin"
        private const val KEY_PIN_ENABLED = "pref_pin_enabled"
        private const val KEY_DEMO_SEEDED = "pref_demo_seeded"
    }
}
