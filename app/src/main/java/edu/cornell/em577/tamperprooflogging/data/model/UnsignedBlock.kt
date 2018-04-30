package edu.cornell.em577.tamperprooflogging.data.model

import com.vegvisir.data.ProtocolMessageProto
import edu.cornell.em577.tamperprooflogging.data.source.UserDataRepository
import edu.cornell.em577.tamperprooflogging.util.toHex
import java.security.PrivateKey
import java.security.Signature
import java.util.*

/**
 * Unsigned bundled set of transactions together with the requester's metadata and the parent
 * blocks' cryptographic hashes.
 */
data class UnsignedBlock(
    val userId: String,
    val timestamp: Long,
    val location: String,
    val parentHashes: List<String>,
    val transactions: List<Transaction>
) {
    companion object {
        private const val USER_ID = "userId"
        private const val TIMESTAMP = "timestamp"
        private const val LOCATION = "location"
        private const val PARENT_HASHES = "parentHashes"
        private const val TRANSACTIONS = "transactions"

        fun fromProto(protoUnsignedBlock: ProtocolMessageProto.UnsignedBlock): UnsignedBlock {
            return UnsignedBlock(
                protoUnsignedBlock.userId,
                protoUnsignedBlock.timestamp,
                protoUnsignedBlock.location,
                protoUnsignedBlock.parentHashesList,
                protoUnsignedBlock.transactionsList.map { Transaction.fromProto(it) })
        }

        fun fromJson(properties: Map<String, Any>): UnsignedBlock {
            return UnsignedBlock(
                properties[USER_ID] as String,
                (properties[TIMESTAMP] as String).toLong(),
                properties[LOCATION] as String,
                properties[PARENT_HASHES] as List<String>,
                (properties[TRANSACTIONS] as List<Map<String, Any>>).map { Transaction.fromJson(it) }
            )
        }

        fun generateAdminCertificate(userRepo: UserDataRepository): UnsignedBlock {
            val (adminId, adminLocation) = userRepo.loadAdminMetaData()
            val hexPublicKey = userRepo.loadAdminHexPublicKey()
            return UnsignedBlock(
                adminId,
                Calendar.getInstance().timeInMillis,
                adminLocation,
                listOf(),
                listOf(Transaction.generateCertificate(adminId, hexPublicKey))
            )
        }

        fun generateUserCertificate(
            userRepo: UserDataRepository,
            parentHashes: List<String>
        ): UnsignedBlock {
            val (adminId, adminLocation) = userRepo.loadAdminMetaData()
            val (userId, _) = userRepo.loadUserMetaData()
            val hexPublicKey = userRepo.loadUserHexPublicKey()
            return UnsignedBlock(
                adminId,
                Calendar.getInstance().timeInMillis,
                adminLocation,
                parentHashes,
                listOf(Transaction.generateCertificate(userId, hexPublicKey))
            )
        }

        fun generateProofOfWitness(
            userRepo: UserDataRepository,
            parentHashes: List<String>,
            localTimestamp: Long
        ): UnsignedBlock {
            val (userId, userLocation) = userRepo.loadUserMetaData()
            return UnsignedBlock(
                userId,
                localTimestamp,
                userLocation,
                parentHashes,
                listOf(Transaction.generateProofOfWitness(userId))
            )
        }
    }

    fun toProto(): ProtocolMessageProto.UnsignedBlock {
        return ProtocolMessageProto.UnsignedBlock.newBuilder()
            .setUserId(userId)
            .setTimestamp(timestamp)
            .setLocation(location)
            .addAllParentHashes(parentHashes)
            .addAllTransactions(transactions.map { it.toProto() })
            .build()
    }

    fun toJson(): HashMap<String, Any> {
        val properties = HashMap<String, Any>()
        properties[USER_ID] = userId
        properties[TIMESTAMP] = timestamp.toString()
        properties[LOCATION] = location
        properties[PARENT_HASHES] = parentHashes
        properties[TRANSACTIONS] = transactions.map { it.toJson() }
        return properties
    }

    fun sign(privateKey: PrivateKey): String {
        val sig = Signature.getInstance("SHA256withRSA")
        sig.initSign(privateKey)
        sig.update(userId.toByteArray())
        sig.update(timestamp.toString().toByteArray())
        sig.update(location.toByteArray())
        sig.update(parentHashes.sorted().toString().toByteArray())
        sig.update(transactions.map { it.toString() }.sorted().toString().toByteArray())
        return sig.sign().toHex()
    }
}