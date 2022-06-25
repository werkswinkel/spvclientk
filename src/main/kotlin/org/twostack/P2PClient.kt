package org.twostack

import io.ktor.network.selector.*
import io.ktor.network.sockets.*
import io.ktor.utils.io.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import org.twostack.bitcoin4j.Sha256Hash
import org.twostack.bitcoin4j.Utils
import org.twostack.message.MessageHeader
import org.twostack.message.P2PMessage
import org.twostack.message.headers.BlockHeaderPayload
import org.twostack.message.pong.PongMessage
import org.twostack.message.pong.PongPayload
import org.twostack.message.version.VersionMessage
import org.twostack.message.version.VersionPayload
import org.twostack.net.RegTestParams
import java.io.ByteArrayOutputStream

class P2PClient(val remoteHost: String, val remotePort: Int) {

    private var writeChannel: ByteChannel = ByteChannel(autoFlush = true)
    private var receiveChannel: ByteChannel = ByteChannel(autoFlush = true)

    suspend fun start() {
        runBlocking {
            println("The P2PClient main thread : ${Thread.currentThread()}")

            val selectorManager = SelectorManager(Dispatchers.IO)
            val socket = async {
                aSocket(selectorManager).tcp().connect(remoteHost, remotePort)
            }.await()

            socket.attachForWriting(writeChannel) // = socket.openWriteChannel(true)
            socket.attachForReading(receiveChannel) // = socket.openReadChannel()

            launch(Dispatchers.IO) {
                println("The P2PClient eventloop thread : ${Thread.currentThread()}")
                var header: MessageHeader
                while (true) {

//                    if (receiveChannel.isClosedForRead){
//                        println("Receiving channel has closed. Aborting. Later !!");
//                       break;
//                    }

                    val availableBytes = receiveChannel.availableForRead ?: 0
                    if (availableBytes > 0) {
                        try {
                            // read the 24-byte header
                            val headerBytes = ByteArray(24)
                            async {
                                receiveChannel.readFully(headerBytes, 0, 24)
                            }.await()

                            //deserialize the header
                            header = MessageHeader.fromByteArray(headerBytes)

                            println("Bytes left to read:[ ${receiveChannel.availableForRead} ]")
                            println("Bytes in payload:[ ${header.payloadSize} ]")

                            if (header.hasPayload()) {
                                //read the payload bytes as specified by the header
                                val payloadBytes = ByteArray(header.payloadSize.toInt())
                                async {
                                    receiveChannel.readFully(payloadBytes, 0, header.payloadSize.toInt())
                                }.await()


                                //look at checksum
                                val checksum = Sha256Hash.hashTwice(payloadBytes)
                                if (header.checksum.toLong() != Utils.readUint32(checksum, 0)) {
                                    println("WARNING! Header checksums don't match!")
                                }
                                //dispatch the message-specific payload read
                                async {
                                    handleMessage(writeChannel, header, payloadBytes)
                                }.await()
                                println("Bytes left after payload read:[ ${receiveChannel.availableForRead} ]\n")

                            }

                        } catch (ex: Exception) {
                            ex.printStackTrace()
                        }
                    }

                }

            }

            doHandShake()
        }

    }

    suspend fun doHandShake() {
        //start the handshake
        val versionPayload = VersionPayload(RegTestParams());
        val versionMessage = VersionMessage(versionPayload)
        sendMessage(versionMessage)

    }

    suspend fun sendMessage(message: P2PMessage) {
        println("The sendMessage thread : ${Thread.currentThread()}")
        val messageBuffer = message.serialize()
        if (messageBuffer.isNotEmpty()) {
            writeChannel.writeFully(messageBuffer, 0, messageBuffer.size)
        }
    }

    suspend fun handleMessage(writeChannel: ByteWriteChannel, header: MessageHeader, payload: ByteArray) {

        println("The handleMessage thread : ${Thread.currentThread()}")
        when (header.commandString) {
            MessageHeader.VERSION -> {
                println("MSG: version message")
                //The verack response has no payload, and only sets the command in header
                val response = MessageHeader(RegTestParams.MAGIC_BYTES, MessageHeader.VERSION_ACK)
                response.setPayloadParams(Sha256Hash.hashTwice(ByteArray(0)), 0u)
                writeChannel.writeFully(response.serialize())
            }
            MessageHeader.VERSION_ACK -> println("MSG: verack message")
            MessageHeader.SENDHEADERS -> println("MSG: sendheaders message")
            MessageHeader.MERKLE_BLOCK -> {
                println("MSG: merkleblock message")
            }
            MessageHeader.NOT_FOUND -> {
                println("MSG: notfound. I don't have what you're looking for. ")
            }
            MessageHeader.GET_HEADERS -> println("getheaders message")
            MessageHeader.HEADERS -> {

                println("MSG: receiving headers ")
//                    val blockHeaders = BlockHeaderPayload.fromByteArray(payload)

//                println(HEX.encode(payload))

            }
            MessageHeader.GET_ADDR -> println("getaddress message")
            MessageHeader.PING -> {
                println("MSG: responding to ping with a pong")
                val nonce = Utils.readInt64(payload, 0)
                val pongMessage = PongMessage(PongPayload(nonce))
                sendMessage(pongMessage)
//                    writeChannel?.writeFully(pongMessage.serialize())


            }
            MessageHeader.PONG -> println("MSG: pong message")
            MessageHeader.FEE_FILTER -> println("MSG: feefilter message")
            MessageHeader.BLOCK -> println("MSG: new block found !")
            MessageHeader.INVENTORY -> println("MSG: here's an inventory !")
            MessageHeader.TRANSACTION -> {
                println("MSG: Here's the transaction you asked for")
            }
            MessageHeader.GET_UTXOS -> {
                println("MSG: Here's the UTXO you requested")
            }
            MessageHeader.FILTER_LOAD -> {
                println("MSG: receiving filter load message")
            }
            MessageHeader.FEE_FILTER -> {
                println("MSG: received a fee filter message")
            }
            else -> {
                println("I don't understand that message: ${header.commandString}")
            }
        }
    }


    //handshakeComplete()

    //headersReceived()

}