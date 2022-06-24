package org.twostack.message

import org.twostack.bitcoin4j.Utils
import java.io.ByteArrayOutputStream
import java.math.BigInteger
import java.net.InetAddress

class PeerAddress(val addr: InetAddress, val port: Int) {

    private val services: BigInteger = BigInteger.ZERO
    private val timestamp: Long = Utils.currentTimeMillis()


    private var protocolVersion: Int = 0

    fun serialize() : ByteArray {

        val stream = ByteArrayOutputStream()

        if (protocolVersion >= 31402) {
            //TODO this appears to be dynamic because the client only ever sends out it's own address
            //so assumes itself to be up.  For a fuller implementation this needs to be dynamic only if
            //the address refers to this client.
            Utils.uint32ToByteStreamLE(timestamp, stream)
        }
        Utils.uint64ToByteStreamLE(services, stream) // nServices.

        // Java does not provide any utility to map an IPv4 address into IPv6 space, so we have to do it by hand.
        var ipBytes = addr.address
        if (ipBytes.size == 4) {
            val v6addr = ByteArray(16)
            System.arraycopy(ipBytes, 0, v6addr, 12, 4)
            v6addr[10] = 0xFF.toByte()
            v6addr[11] = 0xFF.toByte()
            ipBytes = v6addr
        }
        stream.write(ipBytes)

        // And write out the port. Unlike the rest of the protocol, address and port is in big endian byte order.

        stream.write(byteArrayOf((0xFF and port shr 8).toByte()))
        stream.write(byteArrayOf((0xFF and port).toByte()))

        return stream.toByteArray()
    }
}