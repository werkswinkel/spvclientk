package org.twostack.routes

import io.ktor.server.application.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import org.twostack.dto.Hello

fun Route.socketRouting (){

    route("/tx"){
        get {
           //call.respond(SomeContainer)
            call.respond("Yo, wassup")
        }
        get("{id?}"){
            call.respond("Yo, wassup : ${call.parameters["id"]}")
        }
    }
}