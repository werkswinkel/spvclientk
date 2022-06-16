package twostack.org.message

import org.twostack.bitcoin4j.Sha256Hash
import org.twostack.bitcoin4j.Utils
import org.twostack.bitcoin4j.Utils.*
import java.io.ByteArrayOutputStream

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
class MessageHeader {

    private var magicBytes : UInt = 0xfabfb5dau; //RegTest Magic Bytes
//        0x0b110907u; //testnet3
    private var command : ByteArray = ByteArray(12)
    private var commandString = "version"
//    private var payload : UInt = 0u
    private var checksum : ByteArray = ByteArray(4)

    fun serialize(payload: ByteArray) : ByteArray{
        val headerByteStream = ByteArrayOutputStream()

//        headerByteStream.writeBytes(magicBytes)
        uint32ToByteStreamLE(magicBytes.toLong(), headerByteStream)

        //FIXME: also restrict to absolute length of (command.size) which is 12
        for (i in 0..commandString.length - 1) {
           command[i] = (commandString.codePointAt(i) and 0xFF).toByte()
        }
        headerByteStream.writeBytes(command)

        checksum = Sha256Hash.hashTwice(payload)
        Utils.uint32ToByteStreamLE(payload.size.toLong(), headerByteStream)
//        headerByteStream.write(checksum, 0, 4)
        Utils.uint32ToByteStreamLE(Utils.readUint32(checksum, 0), headerByteStream)

        headerByteStream.writeBytes(payload)

        val finalBuf = headerByteStream.toByteArray()
        println(HEX.encode(finalBuf))
        return finalBuf
    }



}