package com.vibenote.app.ui

import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertHasClickAction
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.vibenote.app.core.theme.VibePillButton
import com.vibenote.app.core.theme.VibeNoteTheme
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.Assert.assertTrue

@RunWith(AndroidJUnit4::class)
class VibePillButtonTest {
    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun buttonRendersAndRespondsToClicks() {
        var clicked = false

        composeTestRule.setContent {
            VibeNoteTheme {
                VibePillButton(
                    text = "Click Me",
                    onClick = { clicked = true },
                    isPrimary = true
                )
            }
        }

        val buttonNode = composeTestRule.onNodeWithText("Click Me")
        buttonNode.assertIsDisplayed()
        buttonNode.assertHasClickAction()
        
        buttonNode.performClick()
        
        assertTrue("Button should have registered the click", clicked)
    }
}
