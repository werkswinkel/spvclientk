package org.twostack.net

abstract class P2PNetworkParameters {

    abstract val port : Int;

    companion object {
        const val PROTOCOL_VERSION_MINIMUM = 70000L
        const val PROTOCOL_VERSION_PONG = 60001L
        const val PROTOCOL_VERSION_BLOOM_FILTER = 70000L
        const val PROTOCOL_VERSION_CURRENT = 70013L
    }


}