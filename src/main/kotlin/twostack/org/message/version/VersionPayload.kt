package twostack.org.message.version

import org.twostack.bitcoin4j.Utils
import org.twostack.bitcoin4j.VarInt
import twostack.org.message.MessagePayload
import twostack.org.message.PeerAddress
import twostack.org.net.P2PNetworkParameters
import java.io.ByteArrayOutputStream
import java.io.IOException
import java.net.InetAddress
import java.net.UnknownHostException

class VersionPayload(netParams : P2PNetworkParameters) : MessagePayload(){

    /**
     * The version number of the protocol spoken.
     */
    val clientVersion = P2PNetworkParameters.PROTOCOL_VERSION_CURRENT


    /** We hard-code the IPv4 localhost address here rather than use InetAddress.getLocalHost() because some
     *  mobile phones have broken localhost DNS entries, also, this is faster.
     */
    val localhost = byteArrayOf(127, 0, 0, 1)

    /**
     * Flags defining what optional services are supported.
     */
    var localServices: Long = 4

    /**
     * What the other side believes the current time to be, in seconds.
     */
    var time: Long = System.currentTimeMillis() / 1000

    /**
     * The network address of the node receiving this message.
     */
    var receivingAddr: PeerAddress = PeerAddress(InetAddress.getByAddress(localhost), netParams.port)

    /**
     * The network address of the node emitting this message. Not used.
     */
    var fromAddr: PeerAddress = PeerAddress(InetAddress.getByAddress(localhost), netParams.port)

    /**
     * User-Agent as defined in [BIP 14](https://github.com/bitcoin/bips/blob/master/bip-0014.mediawiki).
     * Bitcoin Core sets it to something like "/Satoshi:0.9.1/".
     */
    var subVer: String = "/spvclient:0.1.0/"

    /**
     * How many blocks are in the chain, according to the other side.
     */
    var bestHeight: Long = 0 //FIXME: This should be set to our current height

    /**
     * Whether or not to relay tx invs before a filter is received.
     * See [BIP 37](https://github.com/bitcoin/bips/blob/master/bip-0037.mediawiki#extensions-to-existing-messages).
     */
    var relayTxesBeforeFilter = false

    fun serialize() : ByteArray{

        val buf = ByteArrayOutputStream()

        Utils.uint32ToByteStreamLE(clientVersion, buf)
        Utils.uint64ToByteStreamLE(localServices, buf)
        Utils.uint64ToByteStreamLE(time, buf)
        try {
            // My address.
            buf.writeBytes(fromAddr.serialize())
            // Their address.
            buf.writeBytes(receivingAddr.serialize())
        } catch (e: UnknownHostException) {
            throw RuntimeException(e) // Can't happen.
        } catch (e: IOException) {
            throw RuntimeException(e) // Can't happen.
        }
        // Next up is the "local host nonce", this is to detect the case of connecting
        // back to yourself. We don't care about this as we won't be accepting inbound
        // connections.
        Utils.uint64ToByteStreamLE(0, buf)


        // Now comes subVer/user-agent
        val subVerBytes = subVer.toByteArray(charset("UTF-8"))
        buf.write(VarInt(subVerBytes.size.toLong()).encode())
        buf.write(subVerBytes)

        // Size of known block chain.
        Utils.uint32ToByteStreamLE(bestHeight, buf)
        buf.write(if (relayTxesBeforeFilter) 1 else 0)

        return buf.toByteArray()
    }
}