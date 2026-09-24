package com.example

import android.content.Context
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onRoot
import androidx.test.core.app.ActivityScenario
import androidx.test.core.app.ApplicationProvider
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class ExampleRobolectricTest {

  @get:Rule
  val composeTestRule = createAndroidComposeRule<MainActivity>()

  @Test
  fun `read string from context`() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val appName = context.getString(R.string.app_name)
    assertEquals("SQLora", appName)
  }

  @Test
  fun `launch MainActivity and compose`() {
    composeTestRule.waitForIdle()
    val context = ApplicationProvider.getApplicationContext<Context>()
    val font = androidx.core.content.res.ResourcesCompat.getFont(context, R.font.jetbrains_mono)
    org.junit.Assert.assertNotNull(font)
  }
}
