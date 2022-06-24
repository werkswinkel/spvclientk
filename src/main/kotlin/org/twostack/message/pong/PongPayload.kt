package org.twostack.message.pong

import org.twostack.bitcoin4j.Utils

class PongPayload(val nonce: Long) {
    fun serialize() : ByteArray{
        val nonceBuffer = ByteArray(64)
        Utils.int64ToByteArrayLE(nonce, nonceBuffer, 0)
        return nonceBuffer
    }
}