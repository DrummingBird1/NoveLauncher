package com.ailauncher.app.ui.screens

import android.app.Application
import android.content.Context
import androidx.compose.ui.test.hasAnyAncestor
import androidx.compose.ui.test.hasScrollAction
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.isDialog
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollToNode
import androidx.compose.ui.test.performTextReplacement
import androidx.test.core.app.ApplicationProvider
import com.ailauncher.app.R
import com.ailauncher.app.data.SettingsRepository
import com.ailauncher.app.data.backup.BackupManager
import com.ailauncher.app.data.backup.DriveClient
import com.ailauncher.app.domain.models.BackupSettings
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/**
 * Settings → Backup → Restore from Google Drive, on the JVM: list the Drive
 * backups, pick an encrypted one, get asked for its password (wrong, then
 * right). BackupManager is mocked — DriveClientTest covers the HTTP side.
 */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [33], application = Application::class)
class BackupSectionDriveRestoreTest {

    @get:Rule val composeRule = createComposeRule()

    private val ctx: Context get() = ApplicationProvider.getApplicationContext()
    private val envelope = "NVLBK1:c2FsdGl2Y2lwaGVydGV4dA=="
    private val driveFile = DriveClient.DriveFile("b1", "novelauncher_backup_2026-10-17_09-00-00.json", 0L)
    private val backupManager = mockk<BackupManager>(relaxed = true)

    @Before
    fun setUp() {
        // DriveAuth.connectedEmail() reads this; non-null = "connected".
        ctx.getSharedPreferences("cloud_auth", Context.MODE_PRIVATE)
            .edit().putString("account_email", "someone@example.com").commit()
        every { backupManager.listLocalBackups() } returns emptyList()
        coEvery { backupManager.listDriveBackups() } returns BackupManager.DriveResult.Success(listOf(driveFile))
        coEvery { backupManager.downloadDriveBackup(driveFile) } returns BackupManager.DriveResult.Success(envelope)
        coEvery { backupManager.restoreFromJson(envelope, any()) } returns BackupManager.BackupResult.Error("Invalid format")
        coEvery { backupManager.restoreFromJson(envelope, "right-password") } returns BackupManager.BackupResult.Success("OK")
    }

    @Test
    fun `encrypted Drive backup asks for its password and restores with it`() {
        composeRule.setContent {
            BackupSection(BackupSettings(), backupManager, mockk<SettingsRepository>(relaxed = true), onUpdate = {})
        }

        val show = ctx.getString(R.string.drive_restore_show)
        composeRule.onNode(hasScrollAction()).performScrollToNode(hasText(show))
        composeRule.onNodeWithText(show).performClick()
        composeRule.waitForIdle()

        composeRule.onNode(hasScrollAction()).performScrollToNode(hasText(driveFile.name))
        composeRule.onNodeWithText(driveFile.name).performClick()
        composeRule.waitForIdle()

        // Blank Encryption password → the dialog asks, and nothing is restored yet.
        composeRule.onNodeWithText(ctx.getString(R.string.restore_password_title)).assertExists()
        coVerify(exactly = 0) { backupManager.restoreFromJson(any(), any()) }

        val passwordField = hasSetTextAction() and hasAnyAncestor(isDialog())
        val ok = hasText(ctx.getString(R.string.action_ok)) and hasAnyAncestor(isDialog())

        composeRule.onNode(passwordField).performTextReplacement("wrong-password")
        composeRule.onNode(ok).performClick()
        composeRule.waitForIdle()
        composeRule.onNodeWithText(ctx.getString(R.string.restore_password_wrong)).assertExists()

        composeRule.onNode(passwordField).performTextReplacement("right-password")
        composeRule.onNode(ok).performClick()
        composeRule.waitForIdle()

        coVerify { backupManager.restoreFromJson(envelope, "right-password") }
        assertTrue(
            "dialog should close after a successful restore",
            composeRule.onAllNodesWithText(ctx.getString(R.string.restore_password_title)).fetchSemanticsNodes().isEmpty()
        )
        composeRule.onNode(hasScrollAction()).performScrollToNode(hasText(ctx.getString(R.string.restore_success)))
        composeRule.onNodeWithText(ctx.getString(R.string.restore_success)).assertExists()
    }

    @Test
    fun `plain Drive backup restores without a password prompt`() {
        coEvery { backupManager.downloadDriveBackup(driveFile) } returns BackupManager.DriveResult.Success("""{"appearance":{}}""")
        coEvery { backupManager.restoreFromJson("""{"appearance":{}}""", "") } returns BackupManager.BackupResult.Success("OK")

        composeRule.setContent {
            BackupSection(BackupSettings(), backupManager, mockk<SettingsRepository>(relaxed = true), onUpdate = {})
        }

        val show = ctx.getString(R.string.drive_restore_show)
        composeRule.onNode(hasScrollAction()).performScrollToNode(hasText(show))
        composeRule.onNodeWithText(show).performClick()
        composeRule.waitForIdle()
        composeRule.onNode(hasScrollAction()).performScrollToNode(hasText(driveFile.name))
        composeRule.onNodeWithText(driveFile.name).performClick()
        composeRule.waitForIdle()

        coVerify { backupManager.restoreFromJson("""{"appearance":{}}""", "") }
        assertTrue(composeRule.onAllNodesWithText(ctx.getString(R.string.restore_password_title)).fetchSemanticsNodes().isEmpty())
    }
}
