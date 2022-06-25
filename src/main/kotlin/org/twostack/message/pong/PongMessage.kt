package org.twostack.message.pong

import org.twostack.bitcoin4j.Sha256Hash
import org.twostack.bitcoin4j.Utils
import org.twostack.message.MessageHeader
import org.twostack.message.P2PMessage
import org.twostack.net.RegTestParams
import java.io.ByteArrayOutputStream

class PongMessage(val payload: PongPayload) : P2PMessage {

    private val header = MessageHeader(RegTestParams.MAGIC_BYTES, MessageHeader.PONG)

    companion object {
        fun fromByteArray(buffer: ByteArray): PongMessage {
            val nonce = Utils.readUint32(buffer, 0)
            return PongMessage(PongPayload(nonce));
        }
    }

    override fun serialize(): ByteArray {
        val pongBuffer = payload.serialize()

        header.setPayloadParams(Sha256Hash.hashTwice(pongBuffer), pongBuffer.size.toUInt())

        //construct the final on-the-wire message
        val messageBytes = ByteArrayOutputStream()
        messageBytes.writeBytes(header.serialize())
        messageBytes.writeBytes(pongBuffer)

        return messageBytes.toByteArray()
    }
}