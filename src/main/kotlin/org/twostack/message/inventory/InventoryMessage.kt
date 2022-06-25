package org.twostack.message.inventory

import org.twostack.bitcoin4j.Sha256Hash
import org.twostack.message.MessageHeader
import org.twostack.message.P2PMessage
import org.twostack.net.RegTestParams
import java.io.ByteArrayOutputStream

class InventoryMessage(private val payload : InventoryPayload) : P2PMessage{
    private val header = MessageHeader(RegTestParams.MAGIC_BYTES, MessageHeader.INVENTORY)

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