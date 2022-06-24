package org.twostack.message.getdata

import org.twostack.bitcoin4j.Sha256Hash
import java.io.ByteArrayOutputStream
import org.twostack.message.MessageHeader
import org.twostack.message.inventory.InventoryPayload
import org.twostack.net.RegTestParams

class GetDataMessage(private val payload : InventoryPayload) {
    private val header = MessageHeader(RegTestParams.MAGIC_BYTES, MessageHeader.GET_DATA)



    fun serialize(): ByteArray {
        val payloadBuffer = payload.serialize()
        header.setPayloadParams(Sha256Hash.hashTwice(payloadBuffer), payloadBuffer.size.toUInt())

        //write the header & payload
        val messageBytes = ByteArrayOutputStream()
        messageBytes.writeBytes(header.serialize())
        messageBytes.writeBytes(payloadBuffer)

        return messageBytes.toByteArray()

    }

}