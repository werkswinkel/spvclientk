package twostack.org.message

import org.twostack.bitcoin4j.Sha256Hash
import org.twostack.bitcoin4j.Utils
import org.twostack.bitcoin4j.Utils.*
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.util.*

/**
 * 4 bytes  | start string  |  char[4]  |   Magic bytes indicating the originating network;
 *                                          used to seek to next message when stream state is unknown.
 *                                          -----------------------------------
 *
 * 12 bytes | command name  | char[12] |    ASCII string which identifies what message type is
 *                                          contained in the payload. Followed by nulls (0x00) to pad out byte count;
 *                                          for example: version\0\0\0\0\0.
 *                                          -----------------------------------
 *
 * 4 bytes  |  payload size | uint32_t |    Number of bytes in payload. The current maximum number of bytes (“MAX_SIZE”)
 *                                          allowed in the payload by Bitcoin Core is 32 MiB—messages with a payload size
 *                                          larger than this will be dropped or rejected.
 *                                          -----------------------------------
 *
 * 4 bytes  | checksum      | char[4]  |    Added inprotocol version 209. First 4 bytes of SHA256(SHA256(payload)) in
 *                                          internal byte order. If payload is empty, as in verack and “getaddr”
 *                                          messages, the checksum is always 0x5df6e0e2 (SHA256(SHA256(<empty string>))).
 *                                          -----------------------------------
 */
class MessageHeader(val magicBytes: UInt, val commandString: String) {

    var payloadSize: UInt = 0u
    var checksum: UInt = 0u

    companion object {

        const val VERSION = "version"
        const val INVENTORY = "inv"
        const val BLOCK = "block"
        const val MERKLE_BLOCK = "merkleblock"
        const val GET_DATA = "getdata"
        const val GET_BLOCKS = "getblocks"
        const val GET_HEADERS = "getheaders"
        const val TRANSACTION = "tx"
        const val ADDRRESS = "addr"
        const val PING = "ping"
        const val PONG = "pong"
        const val VERSION_ACK = "verack"
        const val HEADERS = "headers"
        const val ALERT = "alert"
        const val FILTER_LOAD = "filterload"
        const val NOT_FOUND = "notfound"
        const val MEMPOOL = "mempool"
        const val REJECT = "reject"
        const val UTXOS = "utxos"
        const val GET_UTXOS = "getutxos"
        const val SENDHEADERS = "sendheaders"
        const val FEE_FILTER = "feefilter"
        const val GET_ADDR = "getaddr"

        fun fromByteArray(headerBytes : ByteArray ): MessageHeader {
            val headerStream = ByteArrayInputStream(headerBytes)

            //magic
            val magicBytes = readUint32(headerStream.readNBytes(4), 0)

            //command
            val commandBytes = headerStream.readNBytes(12)
            val scanner = Scanner(ByteArrayInputStream(commandBytes), Charsets.UTF_8)
            scanner.useDelimiter("\u0000")
            val commandString = scanner.next()

            //payload
            val payloadSize = readUint32(headerStream.readNBytes(4), 0)

            //checksum
            val checksum = readUint32(headerStream.readNBytes(4), 0)

            val header = MessageHeader(magicBytes.toUInt(), commandString)
            header.setPayloadParams(checksum.toUInt(), payloadSize.toUInt())

            return header;
        }

    }

    fun hasPayload(): Boolean {
        return payloadSize > 0u
    }



    fun serialize(): ByteArray {
        val headerByteStream = ByteArrayOutputStream()

        //magic
        uint32ToByteStreamLE(magicBytes.toLong(), headerByteStream)

        //command
        var command: ByteArray = ByteArray(12)
        for (i in 0..commandString.length - 1) {
            command[i] = (commandString.codePointAt(i) and 0xFF).toByte()
        }
        headerByteStream.writeBytes(command)

        //payload size
        Utils.uint32ToByteStreamLE(payloadSize.toLong(), headerByteStream)

        //first 4 bytes of payload's checksum
        Utils.uint32ToByteStreamLE(checksum.toLong(), headerByteStream)

        //FIXME: Payload append should move out
//        headerByteStream.writeBytes(payload)

        val finalBuf = headerByteStream.toByteArray()
        println(HEX.encode(finalBuf))
        return finalBuf
    }

    fun setPayloadParams(checksum: UInt, payloadSize: UInt) {

        //payload size
        this.payloadSize = payloadSize

        //first 4 bytes of payload hash is our checksum
        this.checksum = checksum
    }

    fun setPayloadParams(payloadHash: ByteArray, payloadSize: UInt) {
       setPayloadParams(readUint32(payloadHash, 0).toUInt(), payloadSize)
    }


}