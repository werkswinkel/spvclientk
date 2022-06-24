package org.twostack


suspend fun main() {

    val client = P2PClient("127.0.0.1", 18444)
    client.start()

    println("The app is up and running !");

}


