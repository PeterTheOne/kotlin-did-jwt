@file:Suppress("EXPERIMENTAL_API_USAGE")

package me.uport.sdk.httpsdid

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import me.uport.sdk.universaldid.AuthenticationEntry
import me.uport.sdk.universaldid.DIDDocument
import me.uport.sdk.universaldid.PublicKeyEntry
import me.uport.sdk.universaldid.ServiceEntry

/**
 * Encapsulates the fields of a Decentralized Identity Document
 */
@Serializable
data class HttpsDIDDocument(
        @SerialName("@context")
        override val context: String = "https://w3id.org/did/v1",

        @SerialName("id")
        override val id: String, //ex: "did:https:example.com#owner"

        @SerialName("publicKey")
        override val publicKey: List<PublicKeyEntry> = emptyList(),

        @SerialName("authentication")
        override val authentication: List<AuthenticationEntry> = emptyList(),

        @SerialName("service")
        override val service: List<ServiceEntry> = emptyList()

) : DIDDocument {

    /**
     * Serializes this [HttpsDIDDocument] into a JSON string
     */
    fun toJson(): String = Json.stringify(serializer(), this)

    companion object {

        /**
         * Attempts to deserialize a given [json] string into a [HttpsDIDDocument]
         */
        fun fromJson(json: String) = Json.nonstrict.parse(serializer(), json)
    }

}
