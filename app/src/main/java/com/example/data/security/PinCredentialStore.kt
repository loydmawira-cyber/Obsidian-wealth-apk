package com.example.data.security

import android.content.SharedPreferences
import android.util.Base64
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.security.MessageDigest
import java.security.SecureRandom
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.PBEKeySpec

/**
 * Stores and verifies the quick-unlock PIN.
 *
 * What this replaces: the PIN was previously written to SharedPreferences in plaintext
 * (`pref_quick_pin`), read back in plaintext, and compared with `==`. Three defects in one:
 * the PIN was recoverable verbatim from the prefs XML, the comparison was not constant-time,
 * and there was no attempt limit at all — so a 10,000-value keyspace could be walked through
 * the on-screen keypad without ever being slowed down.
 *
 * What this provides:
 *  - PBKDF2 salted hash at rest. The PIN itself is never persisted and never held in memory
 *    beyond the verification call.
 *  - Constant-time comparison via [MessageDigest.isEqual].
 *  - On-device attempt limiting with an escalating lockout that survives process death,
 *    plus a guard against rolling the device clock backwards to skip a lockout.
 *
 * HONEST LIMIT — read before relying on this:
 * a 4-digit PIN is 10,000 candidates. An attacker who extracts this hash record from the
 * device (rooted, or via a backup — see `allowBackup`) can brute-force it offline; PBKDF2
 * makes that cost hours instead of milliseconds, but it does not make it infeasible. This PIN
 * is a convenience re-entry lock in front of Firebase Auth. It is NOT the data-at-rest
 * boundary for the vault and must not be treated as one.
 *
 * The real hardening, in order of value:
 *  1. Biometric unlock via androidx.biometric, with the PIN as fallback only.
 *  2. Bind this record to the Android Keystore (hardware-backed, non-exportable key), which
 *     defeats offline brute force outright. EncryptedSharedPreferences from
 *     androidx.security-crypto is the usual route.
 *  3. Allow a 6+ digit PIN or an alphanumeric passphrase.
 * None of those are done here because each adds a dependency this change set cannot compile
 * and test. They are the recommended follow-up, not optional polish.
 */
class PinCredentialStore(private val prefs: SharedPreferences) {

    sealed class Verification {
        object Success : Verification()
        /** Wrong PIN. [attemptsRemaining] reaches 0 on the attempt that triggers a lockout. */
        data class Incorrect(val attemptsRemaining: Int) : Verification()
        /** Too many failures. No verification was attempted. */
        data class LockedOut(val retryAfterMillis: Long) : Verification()
        object NotSet : Verification()
    }

    fun isPinSet(): Boolean =
        !prefs.getString(KEY_PIN_HASH, null).isNullOrBlank() &&
            !prefs.getString(KEY_PIN_SALT, null).isNullOrBlank()

    /**
     * Derives and stores a hash for [pin]. Returns false if [pin] is not 4 digits.
     * Clears any active lockout, since setting a PIN requires an already-unlocked session.
     */
    suspend fun setPin(pin: String): Boolean {
        if (pin.length != PIN_LENGTH || !pin.all { it.isDigit() }) return false

        val salt = ByteArray(SALT_BYTES).also { SecureRandom().nextBytes(it) }
        val algorithm = preferredAlgorithm()
        val hash = withContext(Dispatchers.Default) {
            derive(pin, salt, ITERATIONS, algorithm)
        }

        prefs.edit()
            .putString(KEY_PIN_HASH, Base64.encodeToString(hash, Base64.NO_WRAP))
            .putString(KEY_PIN_SALT, Base64.encodeToString(salt, Base64.NO_WRAP))
            .putString(KEY_PIN_ALGORITHM, algorithm)
            .putInt(KEY_PIN_ITERATIONS, ITERATIONS)
            .putInt(KEY_FAILED_ATTEMPTS, 0)
            .remove(KEY_LOCKOUT_UNTIL)
            .remove(KEY_LOCKOUT_SET_AT)
            .apply()
        return true
    }

    /**
     * Verifies [pin] against the stored record, enforcing the lockout.
     *
     * Runs off the main thread: PBKDF2 at [ITERATIONS] is deliberately slow, which is the point,
     * and blocking the UI thread for it would stutter the keypad.
     */
    suspend fun verify(pin: String): Verification {
        val remainingLockout = lockoutRemainingMillis()
        if (remainingLockout > 0L) return Verification.LockedOut(remainingLockout)

        val storedHash = prefs.getString(KEY_PIN_HASH, null)
        val storedSalt = prefs.getString(KEY_PIN_SALT, null)
        if (storedHash.isNullOrBlank() || storedSalt.isNullOrBlank()) return Verification.NotSet

        val algorithm = prefs.getString(KEY_PIN_ALGORITHM, ALGORITHM_SHA1) ?: ALGORITHM_SHA1
        val iterations = prefs.getInt(KEY_PIN_ITERATIONS, ITERATIONS)
        val salt = Base64.decode(storedSalt, Base64.NO_WRAP)
        val expected = Base64.decode(storedHash, Base64.NO_WRAP)

        val actual = withContext(Dispatchers.Default) {
            derive(pin, salt, iterations, algorithm)
        }

        // Constant-time. A byte-by-byte or String equality check leaks how much of the
        // candidate matched through timing.
        return if (MessageDigest.isEqual(expected, actual)) {
            prefs.edit()
                .putInt(KEY_FAILED_ATTEMPTS, 0)
                .remove(KEY_LOCKOUT_UNTIL)
                .remove(KEY_LOCKOUT_SET_AT)
                .apply()
            Verification.Success
        } else {
            registerFailure()
        }
    }

    private fun registerFailure(): Verification {
        val failed = prefs.getInt(KEY_FAILED_ATTEMPTS, 0) + 1
        val editor = prefs.edit().putInt(KEY_FAILED_ATTEMPTS, failed)

        val lockoutDuration = lockoutDurationFor(failed)
        return if (lockoutDuration > 0L) {
            val now = System.currentTimeMillis()
            editor
                .putLong(KEY_LOCKOUT_SET_AT, now)
                .putLong(KEY_LOCKOUT_UNTIL, now + lockoutDuration)
                .apply()
            Verification.LockedOut(lockoutDuration)
        } else {
            editor.apply()
            Verification.Incorrect(attemptsRemaining = (MAX_ATTEMPTS - failed).coerceAtLeast(0))
        }
    }

    /**
     * Milliseconds left on the current lockout, or 0 when not locked out.
     *
     * Wall-clock based, so it survives process death and reboot. Moving the device clock
     * backwards does not shorten a lockout: that is detected and the lockout is re-anchored to
     * the new "now" for its full remaining duration. Moving the clock forward can still skip a
     * lockout — an unavoidable limitation of client-side timing, and one more reason the PIN is
     * not the data-at-rest boundary.
     */
    fun lockoutRemainingMillis(): Long {
        val until = prefs.getLong(KEY_LOCKOUT_UNTIL, 0L)
        if (until <= 0L) return 0L

        val setAt = prefs.getLong(KEY_LOCKOUT_SET_AT, 0L)
        val now = System.currentTimeMillis()

        if (now < setAt) {
            val duration = lockoutDurationFor(prefs.getInt(KEY_FAILED_ATTEMPTS, MAX_ATTEMPTS))
            prefs.edit()
                .putLong(KEY_LOCKOUT_SET_AT, now)
                .putLong(KEY_LOCKOUT_UNTIL, now + duration)
                .apply()
            return duration
        }

        val remaining = until - now
        if (remaining <= 0L) {
            prefs.edit().remove(KEY_LOCKOUT_UNTIL).remove(KEY_LOCKOUT_SET_AT).apply()
            return 0L
        }
        return remaining
    }

    /** Removes the PIN record and all attempt state. */
    fun clear() {
        prefs.edit()
            .remove(KEY_PIN_HASH)
            .remove(KEY_PIN_SALT)
            .remove(KEY_PIN_ALGORITHM)
            .remove(KEY_PIN_ITERATIONS)
            .remove(KEY_FAILED_ATTEMPTS)
            .remove(KEY_LOCKOUT_UNTIL)
            .remove(KEY_LOCKOUT_SET_AT)
            .apply()
    }

    private fun derive(pin: String, salt: ByteArray, iterations: Int, algorithm: String): ByteArray {
        val spec = PBEKeySpec(pin.toCharArray(), salt, iterations, KEY_LENGTH_BITS)
        return try {
            SecretKeyFactory.getInstance(algorithm).generateSecret(spec).encoded
        } catch (e: java.security.NoSuchAlgorithmException) {
            // A record written on API 26+ with SHA256 can be read on a device that lacks it only
            // in a downgrade scenario; fail closed rather than silently accepting.
            throw IllegalStateException("PIN hash algorithm $algorithm unavailable on this device", e)
        } finally {
            spec.clearPassword()
        }
    }

    /**
     * PBKDF2WithHmacSHA256 exists from API 26. minSdk is 24, so API 24-25 fall back to
     * PBKDF2WithHmacSHA1 — still sound as a KDF. The choice is recorded per-record so an
     * existing PIN keeps verifying after an OS upgrade.
     */
    private fun preferredAlgorithm(): String = try {
        SecretKeyFactory.getInstance(ALGORITHM_SHA256)
        ALGORITHM_SHA256
    } catch (e: java.security.NoSuchAlgorithmException) {
        ALGORITHM_SHA1
    }

    companion object {
        const val PIN_LENGTH = 4
        const val MAX_ATTEMPTS = 5

        private const val ALGORITHM_SHA256 = "PBKDF2WithHmacSHA256"
        private const val ALGORITHM_SHA1 = "PBKDF2WithHmacSHA1"
        private const val ITERATIONS = 120_000
        private const val KEY_LENGTH_BITS = 256
        private const val SALT_BYTES = 16

        private const val KEY_PIN_HASH = "pref_pin_hash_v1"
        private const val KEY_PIN_SALT = "pref_pin_salt_v1"
        private const val KEY_PIN_ALGORITHM = "pref_pin_algorithm_v1"
        private const val KEY_PIN_ITERATIONS = "pref_pin_iterations_v1"
        private const val KEY_FAILED_ATTEMPTS = "pref_pin_failed_attempts"
        private const val KEY_LOCKOUT_UNTIL = "pref_pin_lockout_until"
        private const val KEY_LOCKOUT_SET_AT = "pref_pin_lockout_set_at"

        /** Escalating lockout. 0 means "no lockout yet". */
        fun lockoutDurationFor(failedAttempts: Int): Long = when {
            failedAttempts < MAX_ATTEMPTS -> 0L
            failedAttempts == 5 -> 30_000L
            failedAttempts == 6 -> 60_000L
            failedAttempts == 7 -> 5 * 60_000L
            failedAttempts == 8 -> 15 * 60_000L
            else -> 60 * 60_000L
        }
    }
}
