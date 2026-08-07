package com.ailauncher.app.security

import android.content.Context
import android.content.SharedPreferences
import android.util.Base64
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import com.ailauncher.app.data.SettingsRepository
import com.ailauncher.app.domain.models.LockMethod
import kotlinx.coroutines.flow.first
import java.security.MessageDigest
import java.security.SecureRandom
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicInteger
import java.util.concurrent.atomic.AtomicLong
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.PBEKeySpec

/**
 * v5 FIX: Biometric lock now works for both app lock AND launcher lock.
 * v8 FIX: Credentials hashed with PBKDF2-HMAC-SHA256 + per-device salt stored in
 *        EncryptedSharedPreferences. Legacy unsalted SHA-256 values are still
 *        accepted and silently upgraded on next successful verify.
 */
class AppLockManager(
    private val context: Context,
    private val settingsRepo: SettingsRepository
) {
    // v9: ConcurrentHashMap + atomics. unlockCache, launcherUnlockedUntil, and the
    // failed-attempt counters are all touched from both the Main thread (UI dialog
    // confirm callbacks, screen-off receiver) and Dispatchers.IO (verify* suspends).
    // Without synchronisation a concurrent verify+clearUnlockCache could publish a
    // stale "unlocked" state — the LockManager is on the critical path for app
    // launch, so subtle races here are scary.
    private val unlockCache = ConcurrentHashMap<String, Long>()
    private val UNLOCK_DURATION_MS = 5 * 60 * 1000L
    private val launcherUnlockedUntil = AtomicLong(0L)

    // v8: Failed-attempt lockout. Resets on success.
    // v9: persisted to securePrefs (EncryptedSharedPreferences) so the exponential
    // backoff survives a process restart. Previously the counter lived only in memory,
    // which let an attacker with physical access reset the backoff by force-stopping
    // the app between guesses — defeating brute-force protection on a tiny PIN keyspace.
    private val failedAttempts = AtomicInteger(0)
    private val lockoutUntil = AtomicLong(0L)
    private val lockoutHydrated = AtomicBoolean(false)
    private val MAX_ATTEMPTS = 5
    private val LOCKOUT_DURATION_MS = 30_000L  // 30 sec — grows after repeated trips

    /** Lazily load persisted lockout state on first access (off the constructor path). */
    private fun hydrateLockoutIfNeeded() {
        if (lockoutHydrated.compareAndSet(false, true)) {
            runCatching {
                failedAttempts.set(securePrefs.getInt(ATTEMPTS_KEY, 0))
                lockoutUntil.set(securePrefs.getLong(LOCKOUT_UNTIL_KEY, 0L))
            }
        }
    }

    private fun persistLockout() {
        runCatching {
            securePrefs.edit()
                .putInt(ATTEMPTS_KEY, failedAttempts.get())
                .putLong(LOCKOUT_UNTIL_KEY, lockoutUntil.get())
                .apply()
        }
    }

    /** Returns ms remaining if locked out, else 0. */
    fun lockoutRemainingMs(): Long {
        hydrateLockoutIfNeeded()
        return (lockoutUntil.get() - System.currentTimeMillis()).coerceAtLeast(0L)
    }

    private fun recordFailure() {
        hydrateLockoutIfNeeded()
        val attempts = failedAttempts.incrementAndGet()
        if (attempts >= MAX_ATTEMPTS) {
            val backoff = LOCKOUT_DURATION_MS * (1 shl (attempts - MAX_ATTEMPTS).coerceAtMost(5))
            lockoutUntil.set(System.currentTimeMillis() + backoff)
        }
        persistLockout()
    }

    private fun recordSuccess() {
        lockoutHydrated.set(true)
        failedAttempts.set(0)
        lockoutUntil.set(0L)
        persistLockout()
    }

    /** Clear all in-memory unlock state. Called on screen-off so locked apps re-prompt. */
    fun clearUnlockCache() {
        unlockCache.clear()
        launcherUnlockedUntil.set(0L)
    }

    // ── Hashing ──────────────────────────────────────────────────────

    companion object {
        private const val PBKDF2_PREFIX = "pbkdf2:"
        private const val ITERATIONS = 100_000
        private const val KEY_LENGTH_BITS = 256
        private const val SECURE_PREFS = "secure_lock_prefs"
        private const val SALT_KEY = "salt"
        private const val ATTEMPTS_KEY = "failed_attempts"
        private const val LOCKOUT_UNTIL_KEY = "lockout_until"
    }

    private val secureRandom = SecureRandom()

    private val securePrefs: SharedPreferences by lazy {
        try {
            val masterKey = MasterKey.Builder(context)
                .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
                .build()
            EncryptedSharedPreferences.create(
                context, SECURE_PREFS, masterKey,
                EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
                EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
            )
        } catch (_: Exception) {
            // Hardware-backed keystore unavailable — fall back to plain SharedPreferences.
            // Salt is still random per-install; weaker than EncryptedSharedPreferences but
            // still infinitely better than no salt.
            context.getSharedPreferences("${SECURE_PREFS}_fallback", Context.MODE_PRIVATE)
        }
    }

    private fun getOrCreateSalt(): ByteArray {
        securePrefs.getString(SALT_KEY, null)?.let { return Base64.decode(it, Base64.NO_WRAP) }
        val salt = ByteArray(16).also { secureRandom.nextBytes(it) }
        securePrefs.edit().putString(SALT_KEY, Base64.encodeToString(salt, Base64.NO_WRAP)).apply()
        return salt
    }

    private fun hash(input: String): String {
        val spec = PBEKeySpec(input.toCharArray(), getOrCreateSalt(), ITERATIONS, KEY_LENGTH_BITS)
        val bytes = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256").generateSecret(spec).encoded
        return PBKDF2_PREFIX + bytes.joinToString("") { "%02x".format(it) }
    }

    private fun legacySha256(input: String): String {
        val bytes = MessageDigest.getInstance("SHA-256").digest(input.toByteArray())
        return bytes.joinToString("") { "%02x".format(it) }
    }

    /**
     * Verifies [input] against [stored], transparently supporting three formats:
     * 1. PBKDF2-prefixed (current)
     * 2. 64-hex-char unsalted SHA-256 (legacy v1-v7)
     * 3. plain text (legacy personalZonePin)
     *
     * Returns the upgraded hash if the stored value used a legacy format and matched,
     * so callers can persist the new format. Returns null if input did not verify.
     */
    private fun verifyAndUpgrade(input: String, stored: String): String? {
        if (stored.isEmpty()) return null
        return when {
            stored.startsWith(PBKDF2_PREFIX) -> if (hash(input) == stored) stored else null
            stored.length == 64 && stored.all { it.isDigit() || it in 'a'..'f' } ->
                if (legacySha256(input) == stored) hash(input) else null
            else -> if (input == stored) hash(input) else null
        }
    }

    // ── Query ────────────────────────────────────────────────────────

    suspend fun isAppLocked(packageName: String): Boolean {
        val sec = settingsRepo.securityFlow.first()
        if (sec.appLockMethod == LockMethod.NONE) return false
        if (packageName !in sec.lockedAppPackages) return false
        val cached = unlockCache[packageName] ?: 0L
        return System.currentTimeMillis() >= cached
    }

    suspend fun getAppLockMethod(): LockMethod = settingsRepo.securityFlow.first().appLockMethod
    suspend fun getLauncherLockMethod(): LockMethod = settingsRepo.securityFlow.first().launcherLockMethod

    /**
     * FIX: Check if current lock method requires BiometricPrompt.
     * Returns true for FINGERPRINT or FACE — UI must call BiometricHelper.authenticate()
     */
    suspend fun requiresBiometric(): Boolean {
        val m = getAppLockMethod()
        return m == LockMethod.FINGERPRINT || m == LockMethod.FACE
    }

    suspend fun launcherRequiresBiometric(): Boolean {
        val m = getLauncherLockMethod()
        return m == LockMethod.FINGERPRINT || m == LockMethod.FACE
    }

    // ── Verify ───────────────────────────────────────────────────────

    suspend fun verifyPin(pin: String): Boolean {
        if (lockoutRemainingMs() > 0) return false
        val sec = settingsRepo.securityFlow.first()
        val upgraded = verifyAndUpgrade(pin, sec.appLockPin) ?: run { recordFailure(); return false }
        if (upgraded != sec.appLockPin) settingsRepo.saveSecurity(sec.copy(appLockPin = upgraded))
        recordSuccess()
        return true
    }

    suspend fun verifyPassword(pw: String): Boolean {
        if (lockoutRemainingMs() > 0) return false
        val sec = settingsRepo.securityFlow.first()
        val upgraded = verifyAndUpgrade(pw, sec.appLockPassword) ?: run { recordFailure(); return false }
        if (upgraded != sec.appLockPassword) settingsRepo.saveSecurity(sec.copy(appLockPassword = upgraded))
        recordSuccess()
        return true
    }

    suspend fun verifyPattern(p: List<Int>): Boolean {
        if (lockoutRemainingMs() > 0) return false
        val sec = settingsRepo.securityFlow.first()
        val candidate = p.joinToString("-")
        val ok = when {
            sec.appLockPatternHash.isNotEmpty() -> hash(candidate) == sec.appLockPatternHash
            sec.appLockPattern.isNotEmpty() -> p == sec.appLockPattern  // legacy plaintext
            else -> false
        }
        if (ok) {
            // Migrate a legacy plaintext pattern to a hash on first successful verify.
            if (sec.appLockPatternHash.isEmpty() && sec.appLockPattern.isNotEmpty()) {
                settingsRepo.saveSecurity(sec.copy(appLockPatternHash = hash(candidate), appLockPattern = emptyList()))
            }
            recordSuccess()
        } else recordFailure()
        return ok
    }

    suspend fun verifyLauncherPin(pin: String): Boolean {
        if (lockoutRemainingMs() > 0) return false
        val sec = settingsRepo.securityFlow.first()
        val upgraded = verifyAndUpgrade(pin, sec.launcherLockPin) ?: run { recordFailure(); return false }
        if (upgraded != sec.launcherLockPin) settingsRepo.saveSecurity(sec.copy(launcherLockPin = upgraded))
        recordSuccess()
        return true
    }

    suspend fun verifyLauncherPassword(pw: String): Boolean {
        if (lockoutRemainingMs() > 0) return false
        val sec = settingsRepo.securityFlow.first()
        val upgraded = verifyAndUpgrade(pw, sec.launcherLockPassword) ?: run { recordFailure(); return false }
        if (upgraded != sec.launcherLockPassword) settingsRepo.saveSecurity(sec.copy(launcherLockPassword = upgraded))
        recordSuccess()
        return true
    }

    suspend fun verifyPersonalZonePin(pin: String): Boolean {
        if (lockoutRemainingMs() > 0) return false
        val sec = settingsRepo.securityFlow.first()
        val upgraded = verifyAndUpgrade(pin, sec.personalZonePin) ?: run { recordFailure(); return false }
        if (upgraded != sec.personalZonePin) settingsRepo.saveSecurity(sec.copy(personalZonePin = upgraded))
        recordSuccess()
        return true
    }

    // ── Grant unlock ─────────────────────────────────────────────────

    fun grantAppUnlock(packageName: String) { unlockCache[packageName] = System.currentTimeMillis() + UNLOCK_DURATION_MS }
    fun grantLauncherUnlock() { launcherUnlockedUntil.set(System.currentTimeMillis() + UNLOCK_DURATION_MS) }
    fun isLauncherUnlocked(): Boolean = System.currentTimeMillis() < launcherUnlockedUntil.get()

    /** Quick Settings Tile "lock now" action — expires the unlock grace period
     *  immediately without touching the configured launcherLockMethod, so the
     *  next resume re-prompts even if it's well within the normal TTL. */
    fun revokeLauncherUnlock() { launcherUnlockedUntil.set(0L) }

    /**
     * FIX: Called after successful BiometricPrompt authentication.
     * Grants unlock just like PIN/password verification does.
     */
    fun grantBiometricUnlock(packageName: String) { grantAppUnlock(packageName) }
    fun grantLauncherBiometricUnlock() { grantLauncherUnlock() }

    // ── Set credentials ──────────────────────────────────────────────

    suspend fun setAppLockPin(pin: String) {
        val c = settingsRepo.securityFlow.first()
        settingsRepo.saveSecurity(c.copy(appLockMethod = LockMethod.PIN, appLockPin = hash(pin)))
    }

    suspend fun setAppLockPassword(pw: String) {
        val c = settingsRepo.securityFlow.first()
        settingsRepo.saveSecurity(c.copy(appLockMethod = LockMethod.PASSWORD, appLockPassword = hash(pw)))
    }

    suspend fun setAppLockPattern(pattern: List<Int>) {
        val c = settingsRepo.securityFlow.first()
        settingsRepo.saveSecurity(c.copy(
            appLockMethod = LockMethod.PATTERN,
            appLockPatternHash = hash(pattern.joinToString("-")),
            appLockPattern = emptyList()  // never persist the raw pattern
        ))
    }

    /**
     * FIX v5: Biometric setup now correctly saves the method.
     * When user selects fingerprint/face in settings:
     * 1. This saves the method
     * 2. On next app launch, isAppLocked() returns true
     * 3. LauncherActivity checks requiresBiometric() → true
     * 4. Shows BiometricPrompt → on success calls grantBiometricUnlock()
     */
    suspend fun setAppLockBiometric(method: LockMethod) {
        require(method == LockMethod.FINGERPRINT || method == LockMethod.FACE)
        val c = settingsRepo.securityFlow.first()
        settingsRepo.saveSecurity(c.copy(appLockMethod = method))
    }

    suspend fun setLauncherLockBiometric(method: LockMethod) {
        require(method == LockMethod.FINGERPRINT || method == LockMethod.FACE)
        val c = settingsRepo.securityFlow.first()
        settingsRepo.saveSecurity(c.copy(launcherLockMethod = method))
    }

    suspend fun setLauncherLockPin(pin: String) {
        val c = settingsRepo.securityFlow.first()
        settingsRepo.saveSecurity(c.copy(launcherLockMethod = LockMethod.PIN, launcherLockPin = hash(pin)))
    }

    suspend fun setPersonalZonePin(pin: String) {
        val c = settingsRepo.securityFlow.first()
        settingsRepo.saveSecurity(c.copy(personalZoneLockMethod = LockMethod.PIN, personalZonePin = hash(pin)))
    }

    suspend fun toggleAppLock(pkg: String) {
        val c = settingsRepo.securityFlow.first()
        val locked = c.lockedAppPackages.toMutableSet()
        if (pkg in locked) locked.remove(pkg) else locked.add(pkg)
        settingsRepo.saveSecurity(c.copy(lockedAppPackages = locked))
    }

    suspend fun setLayoutLocked(v: Boolean) {
        val c = settingsRepo.securityFlow.first()
        settingsRepo.saveSecurity(c.copy(isLayoutLocked = v))
    }

    suspend fun isLayoutLocked(): Boolean = settingsRepo.securityFlow.first().isLayoutLocked
}
