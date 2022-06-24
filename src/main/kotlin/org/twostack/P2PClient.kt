package org.twostack

import io.ktor.network.selector.*
import io.ktor.network.sockets.*
import io.ktor.utils.io.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import org.twostack.bitcoin4j.Sha256Hash
import org.twostack.bitcoin4j.Utils
import org.twostack.message.MessageHeader
import org.twostack.message.headers.BlockHeaderPayload
import org.twostack.message.pong.PongMessage
import org.twostack.message.pong.PongPayload
import org.twostack.message.version.VersionMessage
import org.twostack.message.version.VersionPayload
import org.twostack.net.RegTestParams
import java.io.ByteArrayOutputStream

class P2PClient(val remoteHost: String, val remotePort: Int) {

    suspend fun start(){
        runBlocking{

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
            println("received new message : ${header.commandString}")
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

                    println("receiving headers ")
                    val blockHeaders = BlockHeaderPayload.fromByteArray(payload)

//                println(HEX.encode(payload))

                }
                MessageHeader.GET_ADDR -> println("getaddress message")
                MessageHeader.PING -> {
                    println("responding to ping with a pong")
                    val nonce = Utils.readInt64(payload, 0)
                    val pongMessage = PongMessage(PongPayload(nonce))
                    writeChannel.writeFully(pongMessage.serialize())

//                    //also send a getHeaders message for now
//                    val headerMessage = GetHeadersMessage(GetHeadersPayload())
//                    writeChannel.writeFully(headerMessage.serialize())


                    //followed by a getdata message
//                    val inventory = Inventory(InventoryType.MSG_FILTERED_BLOCK, HEX.decode("74abe123d6cccb7310c6ddde974d26dc0c08fe0358d1688deada2f9a68ff18fb").reversedArray())
////                    val inventory = Inventory(InventoryType.MSG_TX, HEX.decode("678940c939f372e86028b17dfaddacccdb5b8dc3338ae15ea0130acd91fb6eff").reversedArray())
//                    val invItems = listOf<Inventory>(inventory)
//                    val invPayload = InventoryPayload(invItems)
//                    val getDataMessage = GetDataMessage(invPayload)
//                    writeChannel.writeFully(getDataMessage.serialize())

                }
                MessageHeader.PONG -> println("pong message")
                MessageHeader.FEE_FILTER -> println("feefilter message")
                MessageHeader.BLOCK -> println("new block found !")
                MessageHeader.INVENTORY -> println("here's an inventory !")
                MessageHeader.TRANSACTION -> {
                    println("Here's the transaction you asked for")
                }
                MessageHeader.GET_UTXOS -> {
                    println("Here's the UTXO you requested")
                }
                MessageHeader.FILTER_LOAD -> {
                    println("receiving filter load message")
                }
                MessageHeader.FEE_FILTER -> {
                    println("received a fee filter message")
                }
            }
        }
    }


    //handshakeComplete()

    //headersReceived()

}