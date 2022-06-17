package twostack.org.message.version

import org.twostack.bitcoin4j.Sha256Hash
import twostack.org.message.MessageHeader
import twostack.org.net.RegTestParams
import java.io.ByteArrayOutputStream

class VersionMessage(val versionPayload : VersionPayload) {

    private val header = MessageHeader(RegTestParams.MAGIC_BYTES, MessageHeader.VERSION)

    fun serialize() : ByteArray{
        val versionBuffer = versionPayload.serialize()

        //update the header checksum and payload size
        header.setPayloadParams(Sha256Hash.hashTwice(versionBuffer), versionBuffer.size.toUInt())

        //construct the final on-the-wire message
        val messageBytes = ByteArrayOutputStream()
        messageBytes.writeBytes(header.serialize())
        messageBytes.writeBytes(versionBuffer)

        return messageBytes.toByteArray()
    }

}