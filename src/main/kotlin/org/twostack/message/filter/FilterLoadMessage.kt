package org.twostack.message.filter

import org.twostack.bitcoin4j.Sha256Hash
import org.twostack.message.MessageHeader
import org.twostack.message.P2PMessage
import org.twostack.net.RegTestParams
import java.io.ByteArrayOutputStream

class FilterLoadMessage(private val payload : FilterLoadPayload) : P2PMessage{
    private val header = MessageHeader(RegTestParams.MAGIC_BYTES, MessageHeader.FILTER_LOAD)

    override fun serialize(): ByteArray {
        val payloadBuffer = payload.serialize()
        header.setPayloadParams(Sha256Hash.hashTwice(payloadBuffer), payloadBuffer.size.toUInt())

        //write the header & payload
        val messageBytes = ByteArrayOutputStream()
        messageBytes.writeBytes(header.serialize())
        messageBytes.writeBytes(payloadBuffer)

        return messageBytes.toByteArray()

    }
}