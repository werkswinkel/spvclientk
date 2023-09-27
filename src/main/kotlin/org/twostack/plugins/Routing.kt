package org.twostack.plugins

import io.ktor.server.routing.*
import io.ktor.server.application.*
import org.twostack.P2PClient
import org.twostack.routes.socketRouting

fun Application.configureRouting(p2pClient: P2PClient) {

    routing {
        socketRouting(p2pClient)
    }
}
