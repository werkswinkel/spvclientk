package org.twostack.message.merkleblock

import org.twostack.bitcoin4j.Utils
import org.twostack.bitcoin4j.Utils.HEX
import org.twostack.bitcoin4j.VarInt
import org.twostack.message.headers.BlockHeaderPayload
import java.io.ByteArrayInputStream

class MerkleBlockPayload(
    val blockHeader: BlockHeaderPayload,
    val txCount: Long,
    val hashList: ArrayList<String>,
    val flagCount: Long,
    val flagBuffer: String
) {

    companion object {
        fun fromByteArray(buffer: ByteArray) : MerkleBlockPayload {
            val stream = ByteArrayInputStream(buffer)

            val blockHeader = BlockHeaderPayload.fromByteArray(stream.readNBytes(80))

            val txCount = Utils.readUint32FromStream(stream)

            val hashCount = VarInt.fromStream(stream)

            val hashList = ArrayList<String>()
            for (i in 1..hashCount.intValue()) {
                val hashBuffer = stream.readNBytes(32)
                hashList.add(HEX.encode(hashBuffer.reversedArray()))
            }

            val flagCount = VarInt.fromStream(stream)

            val flagBuffer = HEX.encode(stream.readAllBytes())

            return MerkleBlockPayload(blockHeader, txCount, hashList, flagCount.longValue(), flagBuffer)
        }

    }


}