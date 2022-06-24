package org.twostack.plugins

import io.ktor.server.routing.*
import io.ktor.server.application.*
import org.twostack.routes.socketRouting

fun Application.configureRouting() {

    routing {
        socketRouting()
    }
}
