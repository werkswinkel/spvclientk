package org.twostack.message.ping

import org.twostack.bitcoin4j.Sha256Hash
import org.twostack.bitcoin4j.Utils
import org.twostack.message.MessageHeader
import org.twostack.message.P2PMessage
import org.twostack.net.RegTestParams
import java.io.ByteArrayOutputStream

class PingMessage(val payload: PingPayload) : P2PMessage{

    private val header = MessageHeader(RegTestParams.MAGIC_BYTES, MessageHeader.PING)

    companion object {
        fun fromByteArray(buffer: ByteArray): PingMessage {
            val nonce = Utils.readUint32(buffer, 0)
            return PingMessage(PingPayload(nonce));
        }
    }

    override fun serialize(): ByteArray {
        val pingBuffer = payload.serialize()

        header.setPayloadParams(Sha256Hash.hashTwice(pingBuffer), pingBuffer.size.toUInt())

        //construct the final on-the-wire message
        val messageBytes = ByteArrayOutputStream()
        messageBytes.writeBytes(header.serialize())
        messageBytes.writeBytes(pingBuffer)

        return messageBytes.toByteArray()
    }
}