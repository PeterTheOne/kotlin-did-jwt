package me.uport.sdk.ethr_status

import me.uport.credential_status.CredentialStatus
import me.uport.credential_status.StatusEntry
import me.uport.credential_status.StatusResolver
import me.uport.credential_status.getStatusEntry
import me.uport.sdk.core.Networks
import me.uport.sdk.jsonrpc.JsonRPC
import me.uport.sdk.jwt.JWTTools
import org.kethereum.extensions.hexToBigInteger
import org.kethereum.keccakshortcut.keccak
import pm.gnosis.model.Solidity
import java.math.BigInteger


/**
 *
 * Ethr Implementation of the [StatusResolver]
 * This class enables users check the revocation status of a credential
 */
class EthrStatusResolver : StatusResolver {

    override val method = "EthrStatusRegistry2019"

    override suspend fun checkStatus(credential: String): EthrStatus {
        val (_, payloadRaw) = JWTTools().decodeRaw(credential)
        val issuer = payloadRaw["iss"] as String

        val statusEntry = getStatusEntry(credential)

        if (statusEntry.type == method) {
            return runCredentialCheck(
                credential,
                statusEntry,
                issuer
            )
        } else {
            throw IllegalStateException("The method '$method' is not a supported credential status method.")
        }
    }

    /*
     * Checks the revocation status of a given credential by
     * making a call to the smart contract
     *
     */
    private suspend fun runCredentialCheck(
        credential: String,
        status: StatusEntry,
        issuer: String
    ): EthrStatus {
        val (registryAddress, network) = parseRegistryId(status.id)

        val ethNetwork = Networks.get(network)
        val rpc = JsonRPC(ethNetwork.rpcUrl)
        val credentialHash = credential.toByteArray().keccak()

        val encodedMethodCall = Revocation.Revoked.encode(
            Solidity.Address(extractAddress(issuer).hexToBigInteger()),
            Solidity.Bytes32(credentialHash)
        )

        val result = rpc.ethCall(registryAddress, encodedMethodCall)

        return EthrStatus(result.hexToBigInteger())
    }

    /*
     * Parses a given registry ID
     * @returns the network and the registry Address
     *
     */
    internal fun parseRegistryId(id: String): Pair<String, String> {

        //language=RegExp
        val didParsePattern = "^(\\w+)?(?::)?(0x[0-9a-fA-F]{40})".toRegex()

        if (!didParsePattern.matches(id)) {
            throw IllegalArgumentException("The id '$id' is not a valid status registry ID.")
        }

        val matchResult = didParsePattern.find(id)
            ?: throw IllegalStateException("The format for '$id' is not a supported")

        val (network, registryAddress) = matchResult.destructured

        val nameOrId = if (network.isBlank()) {
            "mainnet"
        } else {
            network
        }

        return Pair(registryAddress, nameOrId)
    }

    private fun extractAddress(normalizedDid: String): String {

        //language=RegExp
        val identityExtractPattern = "^did:ethr:((\\w+):)?(0x[0-9a-fA-F]{40})".toRegex()

        return identityExtractPattern
            .find(normalizedDid)
            ?.destructured?.component3() ?: ""
    }
}

/**
 * Represents the status of a credential that should be checked using `EthrStatusRegistry2019`
 **/
data class EthrStatus(
    /**
     * The block number when it was first revoked by the issuer, or [BigInteger.ZERO] if it was never revoked
     **/
    val blockNumber: BigInteger
) : CredentialStatus