package com.example

import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onRoot
import com.example.ui.components.D3AllocationSlice
import com.example.ui.components.D3InteractiveDonutChart
import com.example.ui.theme.EmeraldGrowth
import com.example.ui.theme.ElectricIndigo
import com.example.ui.theme.MyApplicationTheme
import androidx.compose.ui.graphics.Color
import com.github.takahirom.roborazzi.RobolectricDeviceQualifiers
import com.github.takahirom.roborazzi.captureRoboImage
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(qualifiers = RobolectricDeviceQualifiers.Pixel8, sdk = [35])
class GreetingScreenshotTest {

  @get:Rule val composeTestRule = createComposeRule()

  @Test
  fun greeting_screenshot() {
    composeTestRule.setContent {
      MyApplicationTheme {
        D3InteractiveDonutChart(
          slices = listOf(
            D3AllocationSlice(
              key = "EQUITIES",
              name = "Equities",
              value = 138000.0,
              primaryColor = EmeraldGrowth,
              gradientColors = listOf(EmeraldGrowth),
              holdingsCount = 4,
              xirrReturnPercent = 18.4,
              description = "Core Equities"
            ),
            D3AllocationSlice(
              key = "MUTUAL_FUNDS",
              name = "Mutual Funds",
              value = 25000.0,
              primaryColor = ElectricIndigo,
              gradientColors = listOf(ElectricIndigo),
              holdingsCount = 3,
              xirrReturnPercent = 15.2,
              description = "Mutual Funds & SIPs"
            ),
            D3AllocationSlice(
              key = "GOLD",
              name = "Gold & Bullion",
              value = 18000.0,
              primaryColor = Color(0xFFF59E0B),
              gradientColors = listOf(Color(0xFFF59E0B)),
              holdingsCount = 1,
              xirrReturnPercent = 12.8,
              description = "Gold Reserve"
            )
          ),
          totalPortfolioValue = 181000.0
        )
      }
    }

    composeTestRule.onRoot().captureRoboImage(filePath = "src/test/screenshots/greeting.png")
  }
}
