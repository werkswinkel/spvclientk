package org.twostack.routes

import io.ktor.server.application.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import org.twostack.P2PClient
import org.twostack.bitcoin4j.Utils.HEX
import org.twostack.dto.Hello
import org.twostack.message.Inventory
import org.twostack.message.InventoryType
import org.twostack.message.getdata.GetDataMessage
import org.twostack.message.getheaders.GetHeadersMessage
import org.twostack.message.getheaders.GetHeadersPayload
import org.twostack.message.inventory.InventoryPayload

fun Route.socketRouting(p2pClient: P2PClient) {

    route("/tx") {
        get {
            //call.respond(SomeContainer)
            call.respond("Yo, wassup")
        }
        get("{id?}") {
            call.respond("Yo, wassup : ${call.parameters["id"]}")
        }
        get("/headers") {
            println("sending headers message")
            getHeaders(p2pClient)
            call.respond("getHeaders message sent")
        }
        get("/somedata") {
            println("sending getData message")
            getData(p2pClient)
            call.respond("getData message sent")
        }
    }
}

suspend fun getHeaders(p2pClient: P2PClient) {

    //also send a getheaders message for now
    val headermessage = GetHeadersMessage(GetHeadersPayload())
    p2pClient.sendMessage(headermessage)
}

suspend fun getData(p2pClient: P2PClient) {

    //followed by a getdata message
    val inventory = Inventory(
        InventoryType.MSG_FILTERED_BLOCK,
        HEX.decode("74abe123d6cccb7310c6ddde974d26dc0c08fe0358d1688deada2f9a68ff18fb").reversedArray()
    )
//                    val inventory = Inventory(InventoryType.MSG_TX, HEX.decode("678940c939f372e86028b17dfaddacccdb5b8dc3338ae15ea0130acd91fb6eff").reversedArray())
    val invItems = listOf<Inventory>(inventory)
    val invPayload = InventoryPayload(invItems)
    val getDataMessage = GetDataMessage(invPayload)
    p2pClient.sendMessage(getDataMessage)
}