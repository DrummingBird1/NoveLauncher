package com.ailauncher.app.data.backup

import android.accounts.Account
import android.content.Context
import com.google.android.gms.auth.api.identity.AuthorizationRequest
import com.google.android.gms.auth.api.identity.AuthorizationResult
import com.google.android.gms.auth.api.identity.Identity
import com.google.android.gms.auth.api.identity.RevokeAccessRequest
import com.google.android.gms.common.api.Scope
import com.google.android.gms.tasks.Tasks
import timber.log.Timber
import java.util.concurrent.TimeUnit

/**
 * v10: Google Drive authorization via Google Identity Services'
 * [com.google.android.gms.auth.api.identity.AuthorizationClient].
 *
 * Replaces the legacy `GoogleSignIn` flow, which play-services-auth 22 removed.
 * It also fixes a long-standing gap: the old `initiateGoogleSignIn()` lived on
 * LauncherActivity and nothing ever called it, so "Google Drive" was offered as
 * a backup destination with no way to actually connect an account.
 *
 * Flow:
 * - UI calls [request] → `authorize()`. First time, the result carries a
 *   PendingIntent (consent screen); the UI launches it and hands the returned
 *   Intent to [resultFromIntent].
 * - [accessTokenBlocking] is called from the backup worker thread. Once the user
 *   has consented, `authorize()` returns a fresh access token with no UI, so
 *   scheduled backups work in the background. If consent was revoked it returns
 *   null and the backup reports "reconnect in Settings".
 *
 * Only the `drive.file` scope is requested — access to files this app created,
 * never the rest of the user's Drive — plus `email`, used solely to show which
 * account is connected.
 *
 * Needs an Android OAuth client registered in Google Cloud Console for
 * `com.ailauncher.app` + the signing certificate's SHA-1, otherwise authorize()
 * fails with DEVELOPER_ERROR (10). See assets/distribution/GOOGLE_DRIVE_SETUP.md.
 */
object DriveAuth {
    private const val PREFS = "cloud_auth"
    private const val KEY_EMAIL = "account_email"
    private val SCOPES = listOf(
        Scope("https://www.googleapis.com/auth/drive.file"),
        Scope("email")
    )

    fun request(context: Context): AuthorizationRequest =
        AuthorizationRequest.Builder()
            .setRequestedScopes(SCOPES)
            .apply { connectedEmail(context)?.let { setAccount(Account(it, "com.google")) } }
            .build()

    fun client(context: Context) = Identity.getAuthorizationClient(context)

    /** Unwraps the consent screen's result Intent. Throws ApiException on failure. */
    fun resultFromIntent(context: Context, data: android.content.Intent?): AuthorizationResult =
        client(context).getAuthorizationResultFromIntent(data)

    /** Persists the connected account after a successful authorization. */
    fun onAuthorized(context: Context, result: AuthorizationResult): String {
        val email = result.toGoogleSignInAccount()?.email ?: "Google Drive"
        prefs(context).edit().putString(KEY_EMAIL, email).remove("oauth_token").apply()
        return email
    }

    fun connectedEmail(context: Context): String? = prefs(context).getString(KEY_EMAIL, null)

    /**
     * Fresh access token without UI, or null if the user must (re)consent.
     * Blocking — call only off the main thread.
     */
    fun accessTokenBlocking(context: Context): String? {
        if (connectedEmail(context) == null) return null
        return try {
            val result = Tasks.await(client(context).authorize(request(context)), 30, TimeUnit.SECONDS)
            if (result.hasResolution()) null else result.accessToken
        } catch (e: Exception) {
            Timber.w(e, "Drive authorization failed")
            null
        }
    }

    /** Revokes the grant with Google (best effort) and forgets the account locally. */
    fun disconnect(context: Context) {
        val email = connectedEmail(context)
        prefs(context).edit().remove(KEY_EMAIL).remove("oauth_token").apply()
        if (email == null) return
        try {
            client(context).revokeAccess(
                RevokeAccessRequest.builder()
                    .setAccount(Account(email, "com.google"))
                    .setScopes(SCOPES)
                    .build()
            )
        } catch (e: Exception) {
            Timber.w(e, "Drive revokeAccess failed")
        }
    }

    private fun prefs(context: Context) = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
}
