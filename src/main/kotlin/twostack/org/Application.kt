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
import twostack.org.message.MessageHeader
import twostack.org.message.getheaders.GetHeadersMessage
import twostack.org.message.getheaders.GetHeadersPayload
import twostack.org.message.headers.BlockHeaderPayload
import twostack.org.message.inventory.InventoryPayload
import twostack.org.message.pong.PongMessage
import twostack.org.message.pong.PongPayload
import twostack.org.message.version.VersionMessage
import twostack.org.message.version.VersionPayload
import twostack.org.net.RegTestParams
import java.io.ByteArrayOutputStream

suspend fun main() {

    val client = P2PClient("127.0.0.1", 18444)
    client.start()
}


