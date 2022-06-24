package twostack.org.message.inventory

import org.twostack.bitcoin4j.Utils
import org.twostack.bitcoin4j.VarInt
import twostack.org.message.Inventory
import twostack.org.message.InventoryType
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream

/*

    Inventory Entry
    ----------------
    4 bytes  | type
    32 bytes | transaction ID (sha256 hash)
 */
class InventoryPayload(val items: List<Inventory>) {

    companion object {

        fun fromByteArray(buffer: ByteArray): InventoryPayload {
            val inputStream = ByteArrayInputStream(buffer)

            //how many entries did we get ?
            val recordCount = VarInt(buffer, 0)
            val skipBytes = recordCount.originalSizeInBytes
            inputStream.skip(skipBytes.toLong())

            val items = ArrayList<Inventory>()
            for (ndx in 0..recordCount.intValue()){

                val invType = Utils.readUint32(inputStream.readNBytes(4), 0)
                val txnId = inputStream.readNBytes(32)
                items.add(Inventory(InventoryType.fromValue(invType.toInt()), txnId))
            }

            return InventoryPayload(items)
        }
    }



    fun serialize() : ByteArray{
        val out = ByteArrayOutputStream()

        //write the number of items
        val recordCount = VarInt(items.size.toLong())
        out.writeBytes(recordCount.encode())

        //write the items
        for (item : Inventory in items){
            Utils.uint32ToByteStreamLE(item.invType.type.toLong(), out)
            out.write(item.txnId)
        }

        return out.toByteArray()
    }
}