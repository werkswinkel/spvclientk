package org.twostack

import io.ktor.server.application.*
import kotlinx.coroutines.async
import org.twostack.plugins.configureRouting
import org.twostack.plugins.configureSerialization
import org.twostack.plugins.configureSockets


//suspend fun main() {
//
//    val client = P2PClient("127.0.0.1", 18444)
//    client.start()
//
//    println("The app is up and running !");
//
//}

fun main(args: Array<String>): Unit = io.ktor.server.netty.EngineMain.main(args)

fun Application.module(){

    val client = P2PClient("127.0.0.1", 18444)
    async {
        client.start()
    }

    configureRouting(client)
    configureSerialization()
    configureSockets()
}
