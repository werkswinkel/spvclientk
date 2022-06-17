package twostack.org.message.getheaders

import org.twostack.bitcoin4j.Sha256Hash
import org.twostack.bitcoin4j.Utils
import twostack.org.message.MessageHeader
import twostack.org.net.RegTestParams
import java.io.ByteArrayOutputStream

class GetHeadersMessage(val payload: GetHeadersPayload) {
    private val header = MessageHeader(RegTestParams.MAGIC_BYTES, MessageHeader.GET_HEADERS)


    companion object {
        fun fromByteArray(buffer: ByteArray): GetHeadersMessage{
            val nonce = Utils.readUint32(buffer, 0)
            return GetHeadersMessage(GetHeadersPayload());
        }
    }

    fun serialize(): ByteArray {
        val headerBuffer = payload.serialize()

        header.setPayloadParams(Sha256Hash.hashTwice(headerBuffer), headerBuffer.size.toUInt())

        //construct the final on-the-wire message
        val messageBytes = ByteArrayOutputStream()
        messageBytes.writeBytes(header.serialize())
        messageBytes.writeBytes(headerBuffer)

        return messageBytes.toByteArray()
    }
}