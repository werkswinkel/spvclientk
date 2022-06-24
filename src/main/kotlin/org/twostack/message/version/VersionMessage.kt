package org.twostack.message.version

import org.twostack.bitcoin4j.Sha256Hash
import org.twostack.message.MessageHeader
import org.twostack.net.RegTestParams
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