package com.kivopass.app.crypto

import org.bouncycastle.crypto.generators.Argon2BytesGenerator
import org.bouncycastle.crypto.params.Argon2Parameters
import java.security.SecureRandom
import javax.crypto.Cipher
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.SecretKeySpec

/**
 * Primitivas criptográficas de Kivo Pass.
 *  - KDF: Argon2id (salt 16 bytes aleatorios)
 *  - Cifrado: AES-256-GCM (nonce 12 bytes aleatorios únicos, tag 16 bytes)
 *  - Aleatoriedad: SecureRandom
 */
object Crypto {
    const val SALT_LEN = 16
    const val NONCE_LEN = 12
    const val TAG_BITS = 128 // 16 bytes
    const val KEY_LEN = 32   // 256 bits

    // Parámetros Argon2id
    private const val ARGON_MEMORY_KB = 32 * 1024
    private const val ARGON_ITERATIONS = 3
    private const val ARGON_PARALLELISM = 2

    private val rng = SecureRandom()

    fun randomBytes(n: Int): ByteArray = ByteArray(n).also { rng.nextBytes(it) }

    fun randomInt(bound: Int): Int = rng.nextInt(bound)

    fun argon2id(secret: CharArray, salt: ByteArray): ByteArray {
        val params = Argon2Parameters.Builder(Argon2Parameters.ARGON2_id)
            .withVersion(Argon2Parameters.ARGON2_VERSION_13)
            .withIterations(ARGON_ITERATIONS)
            .withMemoryAsKB(ARGON_MEMORY_KB)
            .withParallelism(ARGON_PARALLELISM)
            .withSalt(salt)
            .build()
        val generator = Argon2BytesGenerator()
        generator.init(params)
        val pwd = String(secret).toByteArray(Charsets.UTF_8)
        val out = ByteArray(KEY_LEN)
        try {
            generator.generateBytes(pwd, out, 0, out.size)
        } finally {
            pwd.fill(0)
        }
        return out
    }

    /** Devuelve nonce(12) || ciphertext || tag(16). */
    fun encrypt(key: ByteArray, plain: ByteArray, aad: ByteArray): ByteArray {
        val nonce = randomBytes(NONCE_LEN)
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(Cipher.ENCRYPT_MODE, SecretKeySpec(key, "AES"), GCMParameterSpec(TAG_BITS, nonce))
        cipher.updateAAD(aad)
        return nonce + cipher.doFinal(plain)
    }

    /** Lanza AEADBadTagException si la clave es incorrecta o los datos fueron alterados. */
    fun decrypt(key: ByteArray, blob: ByteArray, aad: ByteArray): ByteArray {
        require(blob.size > NONCE_LEN + TAG_BITS / 8) { "blob inválido" }
        val nonce = blob.copyOfRange(0, NONCE_LEN)
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(Cipher.DECRYPT_MODE, SecretKeySpec(key, "AES"), GCMParameterSpec(TAG_BITS, nonce))
        cipher.updateAAD(aad)
        return cipher.doFinal(blob, NONCE_LEN, blob.size - NONCE_LEN)
    }
}
