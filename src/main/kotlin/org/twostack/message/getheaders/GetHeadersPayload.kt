package org.twostack.message.getheaders

import org.twostack.bitcoin4j.Utils
import org.twostack.bitcoin4j.VarInt
import java.io.ByteArrayOutputStream

/*
    Varies  | count  | compactSize uint | Number of block headers up to a maximum of 2,000. Note: headers-first
                                          sync assumes the sending node will send the maximum number of headers whenever possible.

                                        -------------------------------------
    Varies  | headers | block_header     | Block headers: each 80-byte block header is in the format described in the
                                            block headers section with an additional 0x00 suffixed. This 0x00 is
                                            called the transaction count, but because the headers message doesn’t
                                            include any transactions, the transaction count is always zero.
 */
class GetHeadersPayload {

//    val version : UInt = 70001u
    val hashCount : UInt = 1u //request one
    val hashes : ByteArray = ByteArray(32) // just one dummy hash value to force from headers from genesis
    val stopHash : ByteArray = ByteArray(32)

    fun serialize() : ByteArray{
        val out = ByteArrayOutputStream()

        //write the protocol version
        Utils.uint32ToByteStreamLE(0, out)

//        out.write(version.toInt())
        out.write(VarInt(hashCount.toLong()).encode()) //number of hashes
        out.writeBytes(hashes) //need to reverse actual bytes for little endian write
        out.writeBytes(stopHash)

        return out.toByteArray()
    }
}