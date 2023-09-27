package org.twostack.routes

import io.ktor.server.application.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import org.twostack.P2PClient
import org.twostack.bitcoin4j.Utils.HEX
import org.twostack.dto.Hello
import org.twostack.message.Inventory
import org.twostack.message.InventoryType
import org.twostack.message.bloomfilter.BloomFilter
import org.twostack.message.filter.FilterLoadMessage
import org.twostack.message.filter.FilterLoadPayload
import org.twostack.message.getdata.GetDataMessage
import org.twostack.message.getheaders.GetHeadersMessage
import org.twostack.message.getheaders.GetHeadersPayload
import org.twostack.message.inventory.InventoryPayload

fun Route.socketRouting(p2pClient: P2PClient) {

    route("/tx") {
        get {
            //call.respond(SomeContainer)
            getTx(p2pClient)
            call.respond("Yo, tx message sent")
        }
        get("{id?}") {
            call.respond("Yo, wassup : ${call.parameters["id"]}")
        }
        get("/headers") {
            println("sending headers message")
            getHeaders(p2pClient)
            call.respond("getHeaders message sent")
        }
        get("/setfilter/{txId}") {
            println("setting filter for Tx : ${call.parameters["txId"]}")
            setFilter(p2pClient, call.parameters["txId"].toString())
            call.respond("FilterLoadMessage sent")
        }
        get("/getdata/{blockId}/{txId}") {
            println("sending getData message")
            val blockId = call.parameters["blockId"].toString()
            val txId = call.parameters["txId"].toString()
            getData(p2pClient, blockId, txId)
            call.respond("getData message sent")
        }
    }
}

suspend fun setFilter(p2pClient: P2PClient, txId: String) {
    val filter = BloomFilter(1, 0.01, 2147483649L)
    filter.insert(HEX.decode(txId))
    val filterMessage = FilterLoadMessage(FilterLoadPayload(filter))
    p2pClient.sendMessage(filterMessage)
}

suspend fun getHeaders(p2pClient: P2PClient) {

    //also send a getheaders message for now
    val headermessage = GetHeadersMessage(GetHeadersPayload())
    p2pClient.sendMessage(headermessage)
}

suspend fun getData(p2pClient: P2PClient, blockId: String, txId: String) {

    //followed by a getdata message
    val blockInv = Inventory(
        InventoryType.MSG_FILTERED_BLOCK,
        HEX.decode(blockId).reversedArray()
    )

//    val txInv= Inventory(
//        InventoryType.MSG_TX,
//        HEX.decode(txId).reversedArray()
//    )

    val invItems = listOf<Inventory>(blockInv)
    val invPayload = InventoryPayload(invItems)
    val getDataMessage = GetDataMessage(invPayload)
    p2pClient.sendMessage(getDataMessage)
}

suspend fun getTx(p2pClient: P2PClient) {

    //followed by a getdata message
    val inventory = Inventory(
        InventoryType.MSG_TX,
        HEX.decode("a38b3b397e1fe6eba49180fb21d4207732424e3d0a50d9f16deb0d707a540d62").reversedArray()
    )
    val invItems = listOf<Inventory>(inventory)
    val invPayload = InventoryPayload(invItems)
    val getDataMessage = GetDataMessage(invPayload)
    p2pClient.sendMessage(getDataMessage)
}
