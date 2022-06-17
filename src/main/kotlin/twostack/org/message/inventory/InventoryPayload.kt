package twostack.org.message.inventory

import org.twostack.bitcoin4j.Utils.HEX
import org.twostack.bitcoin4j.VarInt
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream

class InventoryPayload {

//    val items : List<Inventory> = ArrayList()
    val items : List<String> = ArrayList()

    companion object {

//        fun fromByteArray(buffer: ByteArray): InventoryPayload {
//            val inputStream = ByteArrayInputStream(buffer)
//
//            val version = inputStream.readNBytes(4)
//            val recordCount = VarInt(buffer, 5)
//
//            val skipRead = recordCount.originalSizeInBytes
//            inputStream.skip(skipRead.toLong())
//
//            for (ndx in 0..recordCount.intValue()){
//                items.add(HEX.encode(inputStream.readNBytes(32)))
//            }
//
//            inputStream.readNBytes(32)
//        }
    }



    fun serialize() : ByteArray{
        val out = ByteArrayOutputStream()

        return out.toByteArray()
    }
}