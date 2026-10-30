package dev.starhold.core.network

import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive

internal fun JsonElement.objectOrNull(): JsonObject? = this as? JsonObject

internal fun JsonElement.stringOrNull(): String? {
    val primitive = this as? JsonPrimitive ?: return null
    return if (primitive.isString) primitive.content else null
}

internal fun JsonElement.contentOrNull(): String? = (this as? JsonPrimitive)?.content
