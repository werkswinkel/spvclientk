package twostack.org

import io.ktor.network.selector.*
import io.ktor.network.sockets.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import org.twostack.bitcoin4j.Utils.HEX
import twostack.org.message.GetHeaderRequest
import twostack.org.message.MessageHeader
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
                        handleMessage(header, payloadBytes)

                    }
                }

            }
        }

        //start the handshake
        val versionPayload = VersionPayload(RegTestParams());
        val versionBuffer = versionPayload.serialize()
        val versionMessage = MessageHeader(RegTestParams.MAGIC_BYTES, MessageHeader.VERSION).serialize(versionBuffer)
        writeChannel.writeFully(versionMessage, 0, versionMessage.size)

//        val messageRequest = GetHeaderRequest().serialize()
//        val headerMessage = MessageHeader(MessageHeader.GET_HEADERS).serialize(messageRequest, )

    }

}

fun handleMessage(header: MessageHeader, payload: ByteArray) {

    when (header.commandString) {
        MessageHeader.VERSION -> println("version message")
        MessageHeader.VERSION_ACK -> println("verack message")
        MessageHeader.SENDHEADERS -> println("sendheaders message")
        MessageHeader.GET_HEADERS -> println("getheaders message")
        MessageHeader.GET_ADDR -> println("getaddress message")
    }
}

