package com.example

import com.example.data.models.SupportedCurrency
import com.example.data.models.UserSettings
import org.junit.Assert.assertEquals
import org.junit.Test

class UserSettingsCurrencyLabelTest {
    @Test
    fun changingSelectedCurrencyChangesLabelButKeepsTheSameFormattedNumber() {
        val amount = 1234.56
        val kes = UserSettings(currency = SupportedCurrency.KES).formatAmount(amount)
        val usd = UserSettings(currency = SupportedCurrency.USD).formatAmount(amount)

        assertEquals("KSh 1,234.56", kes)
        assertEquals("$1,234.56", usd)
        assertEquals(1234.56, amount, 0.0)
    }
}
