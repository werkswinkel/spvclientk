package twostack.org.net

class RegTestParams : P2PNetworkParameters() {


    override val port: Int
        get() = 18444

    companion object {
//        val MAGIC_BYTES: UInt = 0xdab5bffaU //this packetMagic does not work on REGTEST
        val MAGIC_BYTES: UInt = 0xfabfb5dau; //docs say this is old packetMagic, but it's the one that works with REGTEST
    }

}