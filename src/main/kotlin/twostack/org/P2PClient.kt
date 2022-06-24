package twostack.org

import io.ktor.network.selector.*
import io.ktor.network.sockets.*
import io.ktor.utils.io.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import org.twostack.bitcoin4j.Sha256Hash
import org.twostack.bitcoin4j.Utils
import twostack.org.message.MessageHeader
import twostack.org.message.getheaders.GetHeadersMessage
import twostack.org.message.getheaders.GetHeadersPayload
import twostack.org.message.headers.BlockHeaderPayload
import twostack.org.message.pong.PongMessage
import twostack.org.message.pong.PongPayload
import twostack.org.message.version.VersionMessage
import twostack.org.message.version.VersionPayload
import twostack.org.net.RegTestParams
import java.io.ByteArrayOutputStream

class P2PClient(val remoteHost: String, val remotePort: Int) {

    suspend fun start(){
        runBlocking {

            val selectorManager = SelectorManager(Dispatchers.IO)
            val socket = aSocket(selectorManager).tcp().connect(remoteHost, remotePort)

            val writeChannel = socket.openWriteChannel(true)
            val receiveChannel = socket.openReadChannel()

            var headerCursor = 0
            var payloadCursor = 0
            launch(Dispatchers.IO) {
                val headerStream = ByteArrayOutputStream()
                val payloadStream = ByteArrayOutputStream()
                var header: MessageHeader
                while (true) {
                    if (receiveChannel.availableForRead > 0) {
                        // read the 24-byte header
                        val headerBytes = ByteArray(24)
                        receiveChannel.readFully(headerBytes, 0, 24)

                        //deserialize the header
                        header = MessageHeader.fromByteArray(headerBytes)

                        if (header.hasPayload()) {
                            //read the payload bytes as specified by the header
                            val payloadBytes = ByteArray(header.payloadSize.toInt())
                            receiveChannel.readFully(payloadBytes, 0, header.payloadSize.toInt())

                            //look at checksum
                            val checksum = Sha256Hash.hashTwice(payloadBytes)
                            if(header.checksum.toLong() != Utils.readUint32(checksum, 0)) {
                                println("WARNING! Header checksums don't match!")
                            }
                            //dispatch the message-specific payload read
                            handleMessage(writeChannel, header, payloadBytes)

                        }
                    }

                }
            }

            //start the handshake
            val versionPayload = VersionPayload(RegTestParams());
            val versionMessage = VersionMessage(versionPayload)
            val versionBuffer = versionMessage.serialize()
            writeChannel.writeFully(versionBuffer, 0, versionBuffer.size)

        }

    }

    suspend fun handleMessage(writeChannel: ByteWriteChannel, header: MessageHeader, payload: ByteArray) {

        runBlocking {
            when (header.commandString) {
                MessageHeader.VERSION -> {
                    println("version message")
                    //The verack response has no payload, and only sets the command in header
                    val response = MessageHeader(RegTestParams.MAGIC_BYTES, MessageHeader.VERSION_ACK)
                    response.setPayloadParams(Sha256Hash.hashTwice(ByteArray(0)), 0u)
                    writeChannel.writeFully(response.serialize())
                }
                MessageHeader.VERSION_ACK -> println("verack message")
                MessageHeader.SENDHEADERS -> println("sendheaders message")
                MessageHeader.MERKLE_BLOCK -> {
                    println("merkleblock message")
                }
                MessageHeader.NOT_FOUND -> {
                    println("notfound. I don't have what you're looking for. ")
                }
                MessageHeader.GET_HEADERS -> println("getheaders message")
                MessageHeader.HEADERS -> {

                    println("receiving message hashes ")
                    val blockHeaders = BlockHeaderPayload.fromByteArray(payload)

//                println(HEX.encode(payload))

                }
                MessageHeader.GET_ADDR -> println("getaddress message")
                MessageHeader.PING -> {
                    println("responding to ping with a pong")
                    val nonce = Utils.readInt64(payload, 0)
                    val pongMessage = PongMessage(PongPayload(nonce))
                    writeChannel.writeFully(pongMessage.serialize())

                    //also send a getHeaders message for now
                    val headerMessage = GetHeadersMessage(GetHeadersPayload())
                    writeChannel.writeFully(headerMessage.serialize())

                }
                MessageHeader.PONG -> println("pong message")
                MessageHeader.FEE_FILTER -> println("feefilter message")
            }
        }
    }


    //handshakeComplete()

    //headersReceived()

}