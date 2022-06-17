package twostack.org.net

class RegTestParams : P2PNetworkParameters() {


    override val port: Int
        get() = 18444

    companion object {
//        val MAGIC_BYTES: UInt = 0xdab5bffaU
        val MAGIC_BYTES: UInt = 0xfabfb5dau;
    }

}