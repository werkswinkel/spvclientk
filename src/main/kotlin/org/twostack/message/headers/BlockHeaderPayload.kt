package org.twostack.message.headers

import org.twostack.bitcoin4j.VarInt
import java.io.ByteArrayInputStream

class BlockHeaderPayload(val hashes: ArrayList<ByteArray>) {

    companion object {

        fun fromByteArray(buffer: ByteArray): BlockHeaderPayload{
            val stream = ByteArrayInputStream(buffer)

            val headerSize = VarInt(buffer, 0)
            val skipBytes = headerSize.originalSizeInBytes
            stream.skip(skipBytes.toLong())

            val items : ArrayList<ByteArray> = ArrayList()
            for (ndx in 0..headerSize.intValue() - 1){
                val blockHeaderBytes = stream.readNBytes(81) //there is a trailing null byte to signify no txns
                val headerStream = ByteArrayInputStream(blockHeaderBytes)

                val version = headerStream.readNBytes(4)
                val prevHash = headerStream.readNBytes(32)
                val merkleHash= headerStream.readNBytes(32)
                val time = headerStream.readNBytes(4)
                val nBits = headerStream.readNBytes(4)
                val nonce = headerStream.readNBytes(4)
                val txcount = headerStream.readNBytes(1) //null byte. 0 txns
                headerStream.close()
                items.add(blockHeaderBytes)
            }

            return BlockHeaderPayload(items)
        }

    }
}