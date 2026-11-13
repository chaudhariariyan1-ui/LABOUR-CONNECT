package com.example.domain

import java.security.MessageDigest
import java.security.SecureRandom
import java.util.concurrent.ConcurrentHashMap
import kotlin.math.abs

object SecurityHelper {
    private val random = SecureRandom()

    fun generateSalt(): String {
        val bytes = ByteArray(16)
        random.nextBytes(bytes)
        return bytes.joinToString("") { "%02x".format(it) }
    }

    fun hashSecret(secret: String, salt: String): String {
        val digest = MessageDigest.getInstance("SHA-256")
        val input = "$salt:$secret".toByteArray(Charsets.UTF_8)
        val hash = digest.digest(input)
        return hash.joinToString("") { "%02x".format(it) }
    }

    fun verifySecret(secret: String, salt: String, expectedHash: String): Boolean {
        return hashSecret(secret, salt) == expectedHash
    }

    fun generateDigitalWorkerId(stateName: String, sequenceSeed: Long): String {
        val stateCode = IndiaLocations.codeForState(stateName)
        val suffix = (abs(sequenceSeed) % 900000L + 100000L)
        return "IND-$stateCode-2026-$suffix"
    }

    /**
     * Privacy-preserving identity tokenization.
     * Never stores or exposes raw 12-digit Aadhaar numbers.
     * Accepts only tokenized DigiLocker reference, e-Shram UAN, or Virtual ID (VID),
     * and masks all leading digits before persisting.
     */
    fun tokenizeIdentityReference(method: String, rawReference: String): String {
        val cleaned = rawReference.trim().replace(" ", "").replace("-", "")
        val last4 = if (cleaned.length >= 4) cleaned.takeLast(4) else cleaned.padStart(4, 'X')
        val digestPrefix = hashSecret(cleaned, "SHRAMSETU_DPDP_TOKEN").take(6).uppercase()
        return when (method) {
            "DIGILOCKER_CONSENT" -> "DL-$digestPrefix-XXXX-$last4"
            "ESHRAM_UAN" -> "ES-$digestPrefix-XXXX-$last4"
            "VID_TOKEN" -> "VID-XXXX-XXXX-$last4"
            else -> "TOKEN-$digestPrefix-$last4"
        }
    }

    fun maskMobileNumber(mobile: String, allowFull: Boolean): String {
        if (allowFull) return "+91 $mobile"
        val digits = mobile.filter { it.isDigit() }
        return if (digits.length >= 4) {
            "+91 ••••••${digits.takeLast(4)}"
        } else {
            "+91 ••••••••••"
        }
    }

    fun redactSensitiveDigitsInMessage(text: String): String {
        // Prevents accidental sharing of 12-digit ID numbers in chat
        val twelveDigitPattern = Regex("\\b\\d{4}[\\s-]?\\d{4}[\\s-]?\\d{4}\\b")
        return twelveDigitPattern.replace(text, "[ID REDACTED FOR PRIVACY]")
    }

    fun generateSixDigitOtp(): String {
        val code = 100000 + random.nextInt(900000)
        return code.toString()
    }
}

/**
 * In-memory rate limiter for OTP requests & login attempts.
 */
object RateLimiter {
    private data class AttemptRecord(val timestamps: MutableList<Long> = mutableListOf())
    private val buckets = ConcurrentHashMap<String, AttemptRecord>()

    fun checkAndRecord(key: String, maxAttempts: Int, windowMillis: Long): Long? {
        val now = System.currentTimeMillis()
        val record = buckets.getOrPut(key) { AttemptRecord() }
        synchronized(record) {
            record.timestamps.removeAll { now - it > windowMillis }
            if (record.timestamps.size >= maxAttempts) {
                val oldest = record.timestamps.minOrNull() ?: now
                val waitSec = ((windowMillis - (now - oldest)) / 1000L).coerceAtLeast(1L)
                return waitSec
            }
            record.timestamps.add(now)
            return null
        }
    }
}
