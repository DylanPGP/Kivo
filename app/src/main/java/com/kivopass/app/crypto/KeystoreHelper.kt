package com.kivopass.app.crypto

import android.os.Build
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec

/**
 * Claves auxiliares protegidas por Android Keystore (respaldadas por hardware cuando es posible).
 *  - WRAP: envuelve la clave derivada con Argon2id (la clave nunca sale del Keystore).
 *  - BIO: exige huella dactilar en cada uso; guarda una copia de la clave de la bóveda.
 */
object KeystoreHelper {
    private const val PROVIDER = "AndroidKeyStore"
    private const val WRAP_ALIAS = "kivo_wrap_key_v1"
    private const val BIO_ALIAS = "kivo_bio_key_v1"
    private const val TRANSFORMATION = "AES/GCM/NoPadding"

    private fun keyStore(): KeyStore = KeyStore.getInstance(PROVIDER).apply { load(null) }

    private fun builder(alias: String) = KeyGenParameterSpec.Builder(
        alias, KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT
    )
        .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
        .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
        .setKeySize(256)
        .setRandomizedEncryptionRequired(true)

    private fun generate(spec: KeyGenParameterSpec): SecretKey {
        val kg = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, PROVIDER)
        kg.init(spec)
        return kg.generateKey()
    }

    // ---------- Clave de envoltura (sin autenticación de usuario) ----------

    private fun wrapKey(): SecretKey {
        (keyStore().getKey(WRAP_ALIAS, null) as? SecretKey)?.let { return it }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            try {
                return generate(builder(WRAP_ALIAS).setIsStrongBoxBacked(true).build())
            } catch (e: Exception) {
                // StrongBox no disponible: se usa el TEE normal
            }
        }
        return generate(builder(WRAP_ALIAS).build())
    }

    /** Devuelve iv(12) || ciphertext || tag. */
    fun encrypt(plain: ByteArray): ByteArray {
        val cipher = Cipher.getInstance(TRANSFORMATION)
        cipher.init(Cipher.ENCRYPT_MODE, wrapKey())
        return cipher.iv + cipher.doFinal(plain)
    }

    fun decrypt(blob: ByteArray): ByteArray {
        val iv = blob.copyOfRange(0, Crypto.NONCE_LEN)
        val cipher = Cipher.getInstance(TRANSFORMATION)
        cipher.init(Cipher.DECRYPT_MODE, wrapKey(), GCMParameterSpec(Crypto.TAG_BITS, iv))
        return cipher.doFinal(blob, Crypto.NONCE_LEN, blob.size - Crypto.NONCE_LEN)
    }

    // ---------- Clave biométrica (huella en cada uso) ----------

    /** Crea una clave nueva y devuelve un Cipher de cifrado listo para BiometricPrompt. */
    fun newBioEncryptCipher(): Cipher {
        deleteBioKey()
        val b = builder(BIO_ALIAS)
            .setUserAuthenticationRequired(true)
            .setInvalidatedByBiometricEnrollment(true)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            b.setUserAuthenticationParameters(0, KeyProperties.AUTH_BIOMETRIC_STRONG)
        } else {
            @Suppress("DEPRECATION")
            b.setUserAuthenticationValidityDurationSeconds(-1)
        }
        val key = generate(b.build())
        val cipher = Cipher.getInstance(TRANSFORMATION)
        cipher.init(Cipher.ENCRYPT_MODE, key)
        return cipher
    }

    /** Puede lanzar KeyPermanentlyInvalidatedException si cambiaron las huellas registradas. */
    fun bioDecryptCipher(iv: ByteArray): Cipher {
        val key = keyStore().getKey(BIO_ALIAS, null) as SecretKey
        val cipher = Cipher.getInstance(TRANSFORMATION)
        cipher.init(Cipher.DECRYPT_MODE, key, GCMParameterSpec(Crypto.TAG_BITS, iv))
        return cipher
    }

    fun deleteBioKey() {
        try {
            val ks = keyStore()
            if (ks.containsAlias(BIO_ALIAS)) ks.deleteEntry(BIO_ALIAS)
        } catch (_: Exception) {
        }
    }
}
