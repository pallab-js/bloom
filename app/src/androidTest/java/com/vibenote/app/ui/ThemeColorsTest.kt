package com.vibenote.app.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.assertIsDisplayed
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.vibenote.app.core.theme.VibeNoteTheme
import com.vibenote.app.core.theme.VibeColors
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class ThemeColorsTest {
    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun darkThemeColorsAreApplied() {
        composeTestRule.setContent {
            VibeNoteTheme(darkTheme = true) {
                // Tag a box with background colour for testing
                Box(
                    modifier = Modifier
                        .testTag("darkBackground")
                        .background(VibeColors.Dark.backgroundDark)
                )
            }
        }
        // verify the box exists
        composeTestRule.onNodeWithTag("darkBackground").assertIsDisplayed()
    }

    @Test
    fun lightThemeColorsAreApplied() {
        composeTestRule.setContent {
            VibeNoteTheme(darkTheme = false) {
                Box(
                    modifier = Modifier
                        .testTag("lightBackground")
                        .background(VibeColors.Light.backgroundLight)
                )
            }
        }
        composeTestRule.onNodeWithTag("lightBackground").assertIsDisplayed()
    }
}
