package twostack.org.message.pong

import org.twostack.bitcoin4j.Sha256Hash
import org.twostack.bitcoin4j.Utils
import twostack.org.message.MessageHeader
import twostack.org.net.RegTestParams
import java.io.ByteArrayOutputStream

class PongMessage(val payload: PongPayload) {

    private val header = MessageHeader(RegTestParams.MAGIC_BYTES, MessageHeader.PONG)

    companion object {
        fun fromByteArray(buffer: ByteArray): PongMessage {
            val nonce = Utils.readUint32(buffer, 0)
            return PongMessage(PongPayload(nonce));
        }
    }

    fun serialize(): ByteArray {
        val pongBuffer = payload.serialize()

        header.setPayloadParams(Sha256Hash.hashTwice(pongBuffer), pongBuffer.size.toUInt())

        //construct the final on-the-wire message
        val messageBytes = ByteArrayOutputStream()
        messageBytes.writeBytes(header.serialize())
        messageBytes.writeBytes(pongBuffer)

        return messageBytes.toByteArray()
    }
}