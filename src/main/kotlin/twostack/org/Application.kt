package twostack.org

import io.ktor.network.selector.*
import io.ktor.network.sockets.*
import io.ktor.utils.io.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import org.twostack.bitcoin4j.Sha256Hash
import org.twostack.bitcoin4j.Utils
import org.twostack.bitcoin4j.Utils.*
import twostack.org.message.GetHeaderRequest
import twostack.org.message.MessageHeader
import twostack.org.message.pong.PongMessage
import twostack.org.message.pong.PongPayload
import twostack.org.message.version.VersionMessage
import twostack.org.message.version.VersionPayload
import twostack.org.net.RegTestParams
import java.io.ByteArrayOutputStream
import java.nio.ByteBuffer

suspend fun main() {
//    embeddedServer(Netty, port = 8080, host = "0.0.0.0") {
//        configureSockets()
//        configureRouting()
//    }.start(wait = true)
    runBlocking {

        val selectorManager = SelectorManager(Dispatchers.IO)
        val socket = aSocket(selectorManager).tcp().connect("127.0.0.1", 18444)


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

//        val messageRequest = GetHeaderRequest().serialize()
//        val headerMessage = MessageHeader(MessageHeader.GET_HEADERS).serialize(messageRequest, )

    }

}

fun handleMessage(writeChannel: ByteWriteChannel, header: MessageHeader, payload: ByteArray) {

    runBlocking {
        when (header.commandString) {
            MessageHeader.VERSION -> {
                println("version message")
                val response = MessageHeader(RegTestParams.MAGIC_BYTES, MessageHeader.VERSION_ACK)
                response.setPayloadParams(Sha256Hash.hashTwice(ByteArray(0)), 0u)
                writeChannel.writeFully(response.serialize())
            }
            MessageHeader.VERSION_ACK -> println("verack message")
            MessageHeader.SENDHEADERS -> println("sendheaders message")
            MessageHeader.GET_HEADERS -> println("getheaders message")
            MessageHeader.GET_ADDR -> println("getaddress message")
            MessageHeader.PING -> {
                println("responding to ping with a pong")
                val nonce = readInt64(payload, 0)
                val pongMessage = PongMessage(PongPayload(nonce))
                writeChannel.writeFully(pongMessage.serialize())

            }
            MessageHeader.PONG -> println("pong message")
            MessageHeader.FEE_FILTER -> println("feefilter message")
        }
    }
}

