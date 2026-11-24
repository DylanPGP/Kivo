package com.kivopass.app.data

import org.json.JSONObject
import java.util.UUID

enum class EntryType { LOGIN, CARD, SEED }

enum class AuthType { PIN, PASSWORD }

/** Claves de campos. */
object K {
    const val URL = "url"
    const val USER = "user"
    const val TOTP = "totp"
    const val PASS = "password"
    const val HOLDER = "holder"
    const val NUMBER = "number"
    const val EXPIRY = "expiry"
    const val CVV = "cvv"
    const val PIN = "pin"
    const val PHRASE = "phrase"
    const val PASSPHRASE = "passphrase"
    const val NOTES = "notes"
}

data class Entry(
    val id: String = UUID.randomUUID().toString(),
    val type: EntryType,
    val title: String,
    val fields: Map<String, String>,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = createdAt
) {
    fun f(key: String): String = fields[key].orEmpty()

    fun toJson(): JSONObject = JSONObject().apply {
        put("id", id)
        put("type", type.name)
        put("title", title)
        put("created", createdAt)
        put("updated", updatedAt)
        put("fields", JSONObject().also { o -> fields.forEach { (k, v) -> o.put(k, v) } })
    }

    companion object {
        fun fromJson(o: JSONObject): Entry {
            val fo = o.getJSONObject("fields")
            val map = HashMap<String, String>()
            fo.keys().forEach { map[it] = fo.getString(it) }
            return Entry(
                id = o.getString("id"),
                type = EntryType.valueOf(o.getString("type")),
                title = o.getString("title"),
                fields = map,
                createdAt = o.optLong("created", 0L),
                updatedAt = o.optLong("updated", 0L)
            )
        }
    }
}
