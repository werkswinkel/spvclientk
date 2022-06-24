package org.twostack.message.ping

import org.twostack.bitcoin4j.Utils

class PingPayload(val nonce : Long ) {

    fun serialize() : ByteArray{

        val nonceBuffer = ByteArray(64)
        Utils.int64ToByteArrayLE(nonce, nonceBuffer, 0)

        return nonceBuffer
    }
}