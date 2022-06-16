package twostack.org

import io.ktor.network.selector.*
import io.ktor.network.sockets.*
import io.ktor.utils.io.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import org.twostack.bitcoin4j.Utils.HEX
import twostack.org.message.GetHeaderRequest
import twostack.org.message.MessageHeader
import twostack.org.message.VersionMessage
import twostack.org.net.RegTestParams
import java.nio.ByteBuffer

suspend fun main() {
//    embeddedServer(Netty, port = 8080, host = "0.0.0.0") {
//        configureSockets()
//        configureRouting()
//    }.start(wait = true)
    runBlocking {

        val selectorManager = SelectorManager(Dispatchers.IO)
        val socket = aSocket(selectorManager).tcp().connect("127.0.0.1", 18444)

        val versionMessage = VersionMessage(RegTestParams());

        val writeChannel = socket.openWriteChannel(true)
        val receiveChannel = socket.openReadChannel()

        launch(Dispatchers.IO) {
            while (true) {
                val count = receiveChannel.availableForRead
                if (count > 0) {
                    val buff = ByteBuffer.allocate(count)
                    receiveChannel.readAvailable(buff)

                    println(HEX.encode(buff.array()))
                }
            }
        }

        //write something

//    val messageRequest = GetHeaderRequest().serialize()

//    println(HEX.encode(finalMessage))

        val buf = versionMessage.serialize()
        val finalMessage = MessageHeader().serialize(buf)
        writeChannel.writeFully(finalMessage, 0, finalMessage.size )

    }

}
