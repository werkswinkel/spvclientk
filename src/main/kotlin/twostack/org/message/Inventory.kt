package twostack.org.message

/***
 *    Inventory Entry
 *    ----------------
 *    4 bytes  | type
 *    32 bytes | transaction ID (sha256 hash)
 */

enum class InventoryType(val type: Int){

    UNDEFINED(0),
    MSG_TX(1),
    MSG_BLOCK(2),
    MSG_FILTERED_BLOCK (3),
    MSG_CMPCT_BLOCK (4);

    companion object {
       fun fromValue(type: Int) : InventoryType {
           return when (type) {
               UNDEFINED.type -> UNDEFINED
               MSG_TX.type -> MSG_TX
               MSG_BLOCK.type -> MSG_BLOCK
               MSG_FILTERED_BLOCK.type -> MSG_FILTERED_BLOCK
               MSG_CMPCT_BLOCK.type -> MSG_CMPCT_BLOCK
               else -> throw Exception("Undefined inventory type. That type does not exist")
           }
       }
    }
}

data class Inventory(val invType : InventoryType, val txnId: ByteArray) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as Inventory

        if (invType != other.invType) return false
        if (!txnId.contentEquals(other.txnId)) return false

        return true
    }

    override fun hashCode(): Int {
        var result = invType.type
        result = 31 * result + txnId.contentHashCode()
        return result
    }
}