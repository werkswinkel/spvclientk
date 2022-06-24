package twostack.org.message.getdata

import org.twostack.bitcoin4j.Sha256Hash
import twostack.org.message.MessageHeader
import twostack.org.message.inventory.InventoryPayload
import twostack.org.net.RegTestParams
import java.io.ByteArrayOutputStream

/*
    UNDEFINED = 0,
    MSG_TX = 1,
    MSG_BLOCK = 2,
    // The following can only occur in getdata. Invs always use TX or BLOCK.
    //!< Defined in BIP37
    MSG_FILTERED_BLOCK = 3,
    //!< Defined in BIP152
    MSG_CMPCT_BLOCK = 4,
 */
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