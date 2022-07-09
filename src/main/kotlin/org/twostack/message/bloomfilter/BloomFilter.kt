package org.twostack.message.bloomfilter

import com.google.common.base.MoreObjects
import org.twostack.bitcoin4j.Utils
import org.twostack.bitcoin4j.VarInt
import java.io.ByteArrayOutputStream
import java.io.IOException
import java.util.*

class BloomFilter {

    /** The BLOOM_UPDATE_* constants control when the bloom filter is auto-updated by the peer using
     * it as a filter, either never, for all outputs or only for P2PK outputs (default)  */
    enum class BloomUpdate {
        UPDATE_NONE,  // 0
        UPDATE_ALL,  // 1

        /** Only adds outpoints to the filter if the output is a P2PK/pay-to-multisig script  */
        UPDATE_P2PUBKEY_ONLY //2
    }

    private var data: ByteArray
    private var hashFuncs: Long = 0
    private var nTweak: Long = 0
    private var nFlags: Byte = 0

    // Same value as Bitcoin Core
    // A filter of 20,000 items and a false positive rate of 0.1% or one of 10,000 items and 0.0001% is just under 36,000 bytes
    private val MAX_FILTER_SIZE: Long = 36000

    // There is little reason to ever have more hash functions than 50 given a limit of 36,000 bytes
    private val MAX_HASH_FUNCS = 50

    /**
     * Construct a BloomFilter by deserializing payloadBytes
     */
//    @Throws(ProtocolException::class)
//    fun BloomFilter(params: NetworkParameters?, payloadBytes: ByteArray?) {
//        super(params, payloadBytes, 0)
//    }

    /**
     * Constructs a filter with the given parameters which is updated on P2PK outputs only.
     */
    constructor(elements: Int, falsePositiveRate: Double, randomNonce: Long) :
            this(elements, falsePositiveRate, randomNonce, BloomUpdate.UPDATE_NONE)

    /**
     *
     * constructs a new bloom filter which will provide approximately the given false positive rate when the given
     * number of elements have been inserted. if the filter would otherwise be larger than the maximum allowed size,
     * it will be automatically downsized to the maximum size.
     *
     *
     * to check the theoretical false positive rate of a given filter, use
     * [BloomFilter.getFalsePositiveRate].
     *
     *
     * The anonymity of which coins are yours to any peer which you send a BloomFilter to is controlled by the
     * false positive rate. For reference, as of block 187,000, the total number of addresses used in the chain was
     * roughly 4.5 million. Thus, if you use a false positive rate of 0.001 (0.1%), there will be, on average, 4,500
     * distinct public keys/addresses which will be thought to be yours by nodes which have your bloom filter, but
     * which are not actually yours. Keep in mind that a remote node can do a pretty good job estimating the order of
     * magnitude of the false positive rate of a given filter you provide it when considering the anonymity of a given
     * filter.
     *
     *
     * In order for filtered block download to function efficiently, the number of matched transactions in any given
     * block should be less than (with some headroom) the maximum size of the MemoryPool used by the Peer
     * doing the downloading (default is [TxConfidenceTable.MAX_SIZE]). See the comment in processBlock(FilteredBlock)
     * for more information on this restriction.
     *
     *
     * randomNonce is a tweak for the hash function used to prevent some theoretical DoS attacks.
     * It should be a random value, however secureness of the random value is of no great consequence.
     *
     *
     * updateFlag is used to control filter behaviour on the server (remote node) side when it encounters a hit.
     * See [BloomFilter.BloomUpdate] for a brief description of each mode. The purpose
     * of this flag is to reduce network round-tripping and avoid over-dirtying the filter for the most common
     * wallet configurations.
     */
    constructor(elements: Int, falsePositiveRate: Double, randomNonce: Long, updateFlag: BloomUpdate) {
        // The following formulas were stolen from Wikipedia's page on Bloom Filters (with the addition of min(..., MAX_...))
        //                        Size required for a given number of elements and false-positive rate
        var size = (-1 / Math.pow(Math.log(2.0), 2.0) * elements * Math.log(falsePositiveRate)).toInt()
        size = Math.max(1, Math.min(size, MAX_FILTER_SIZE.toInt() * 8) / 8)
        data = ByteArray(size)
        // Optimal number of hash functions for a given filter size and element count.
        hashFuncs = (data.size * 8 / elements.toDouble() * Math.log(2.0)).toInt().toLong()
        hashFuncs = Math.max(1, Math.min(hashFuncs, MAX_HASH_FUNCS.toLong()))
        nTweak = randomNonce
        nFlags = (0xff and updateFlag.ordinal).toByte()
    }

    /**
     * Returns the theoretical false positive rate of this filter if were to contain the given number of elements.
     */
    fun getFalsePositiveRate(elements: Int): Double {
        return Math.pow(1 - Math.pow(Math.E, -1.0 * (hashFuncs * elements) / (data.size * 8)), hashFuncs.toDouble())
    }

    override fun toString(): String {
        val helper = MoreObjects.toStringHelper(this).omitNullValues()
        helper.add("data length", data.size)
        helper.add("hashFuncs", hashFuncs)
        helper.add("nFlags", getUpdateFlag())
        return helper.toString()
    }

//    @Throws(ProtocolException::class)
//    protected fun parse() {
//        data = readByteArray()
//        if (data.size > MAX_FILTER_SIZE) throw ProtocolException("Bloom filter out of size range.")
//        hashFuncs = readUint32()
//        if (hashFuncs > MAX_HASH_FUNCS) throw ProtocolException("Bloom filter hash function count out of range")
//        nTweak = readUint32()
//        nFlags = readBytes(1).get(0)
//        length = cursor - offset
//    }

    @Throws(IOException::class)
    fun serialize() : ByteArray {
        val stream = ByteArrayOutputStream()
        stream.write(VarInt(data.size.toLong()).encode())
        stream.write(data)
        Utils.uint32ToByteStreamLE(hashFuncs, stream)
        Utils.uint32ToByteStreamLE(nTweak, stream)
        stream.write(nFlags.toInt())

        return stream.toByteArray()
    }

    private fun rotateLeft32(x: Int, r: Int): Int {
        return x shl r or (x ushr 32 - r)
    }
    /**
     * Applies the MurmurHash3 (x86_32) algorithm to the given data.
     * See this [C++ code for the original.](https://github.com/aappleby/smhasher/blob/master/src/MurmurHash3.cpp)
     */


    private fun murmurHash3(data: ByteArray, nTweak: Long, hashNum: Int, buffer: ByteArray): Int {
        var h1 : Int = (hashNum * 0xFBA4C795 + nTweak).toInt()
        val c1 : Int = (0xcc9e2d51).toInt()
        val c2 : Int = (0x1b873593).toInt()

        val numBlocks = buffer.size / 4 * 4
        // body
        var i = 0
        while (i < numBlocks) {
            var k1 : Int = buffer[i].toInt() and 0xFF or
                    (buffer[i + 1].toInt() and 0xFF shl 8) or
                    (buffer[i + 2].toInt() and 0xFF shl 16) or
                    (buffer[i + 3].toInt() and 0xFF shl 24)
            k1 *= c1
            k1 = rotateLeft32(k1, 15)
            k1 *= c2
            h1 = h1 xor k1
            h1 = rotateLeft32(h1, 13)
            h1 = h1 * 5 + (0xe6546b64).toInt()
            i += 4
        }
        var k1 = 0
        when (buffer.size and 3) {
            3 -> {
                k1 = k1 xor (buffer[numBlocks + 2].toInt() and 0xff) shl 16
            }
            2 -> {
                k1 = k1 xor (buffer[numBlocks + 1].toInt() and 0xff) shl 8
            }
            1 -> {
                k1 = k1 xor (buffer[numBlocks].toInt() and 0xff)
                k1 *= c1
                k1 = rotateLeft32(k1, 15)
                k1 *= c2
                h1 = h1 xor k1
            }
            else -> {}
        }

        // finalization
        h1 = h1 xor buffer.size.toInt()
        h1 = h1 xor (h1 ushr 16)
        h1 *= (0x85ebca6b).toInt()
        h1 = h1 xor (h1 ushr 13)
        h1 *= (0xc2b2ae35).toInt()
        h1 = h1 xor (h1 ushr 16)

        return ((h1.toLong() and 0xFFFFFFFFL) % (data.size * 8)).toInt()
    }
    /**
     * Returns true if the given object matches the filter either because it was inserted, or because we have a
     * false-positive.
     */
    @Synchronized
    operator fun contains(buffer: ByteArray): Boolean {
        for (i in 0 until hashFuncs) {
            if (!Utils.checkBitLE(data, murmurHash3(data, nTweak, i.toInt(), buffer))) return false
        }
        return true
    }

    /** Insert the given arbitrary data into the filter  */
    @Synchronized
    fun insert(buffer: ByteArray) {
        for (i in 0 until hashFuncs) {
            Utils.setBitLE(data, murmurHash3(data, nTweak, i.toInt(), buffer))
        }
    }

    /**
     * Sets this filter to match all objects. A Bloom filter which matches everything may seem pointless, however,
     * it is useful in order to reduce steady state bandwidth usage when you want full blocks. Instead of receiving
     * all transaction data twice, you will receive the vast majority of all transactions just once, at broadcast time.
     * Solved blocks will then be send just as Merkle trees of tx hashes, meaning a constant 32 bytes of data for each
     * transaction instead of 100-300 bytes as per usual.
     */
    @Synchronized
    fun setMatchAll() {
        data = byteArrayOf(0xff.toByte())
    }

    /**
     * Copies filter into this. Filter must have the same size, hash function count and nTweak or an
     * IllegalArgumentException will be thrown.
     */
//    fun merge(filter: BloomFilter) {
//        if (!matchesAll() && !filter.matchesAll()) {
//            Preconditions.checkArgument(
//                filter.data.size == data.size && filter.hashFuncs == hashFuncs && filter.nTweak == nTweak
//            )
//            for (i in data.indices) data[i] = data[i] or filter.data.get(i)
//        } else {
//            data = byteArrayOf(0xff.toByte())
//        }
//    }

    /**
     * Returns true if this filter will match anything. See [BloomFilter.setMatchAll]
     * for when this can be a useful thing to do.
     */
    @Synchronized
    fun matchesAll(): Boolean {
        for (b in data) if (b != 0xff.toByte()) return false
        return true
    }

    /**
     * The update flag controls how application of the filter to a block modifies the filter. See the enum javadocs
     * for information on what occurs and when.
     */
    @Synchronized
    fun getUpdateFlag(): BloomUpdate {
        return if (nFlags.toInt() == 0) BloomUpdate.UPDATE_NONE else if (nFlags.toInt() == 1) BloomUpdate.UPDATE_ALL else if (nFlags.toInt() == 2) BloomUpdate.UPDATE_P2PUBKEY_ONLY else throw IllegalStateException(
            "Unknown flag combination"
        )
    }

    /**
     * Creates a new FilteredBlock from the given Block, using this filter to select transactions. Matches can cause the
     * filter to be updated with the matched element, this ensures that when a filter is applied to a block, spends of
     * matched transactions are also matched. However it means this filter can be mutated by the operation. The returned
     * filtered block already has the matched transactions associated with it.
     */
//    @Synchronized
//    fun applyAndUpdate(block: Block): FilteredBlock? {
//        val txns: List<Transaction> = block.getTransactions()
//        val txHashes: MutableList<Sha256Hash> = ArrayList<Sha256Hash>(txns.size)
//        val matched: MutableList<Transaction> = ArrayList<Transaction>()
//        val bits = ByteArray(Math.ceil(txns.size / 8.0).toInt())
//        for (i in txns.indices) {
//            val tx: Transaction = txns[i]
//            txHashes.add(tx.getTxId())
//            if (applyAndUpdate(tx)) {
//                Utils.setBitLE(bits, i)
//                matched.add(tx)
//            }
//        }
//        val pmt: PartialMerkleTree = PartialMerkleTree.buildFromLeaves(block.getParams(), bits, txHashes)
//        val filteredBlock = FilteredBlock(block.getParams(), block.cloneAsHeader(), pmt)
//        for (transaction in matched) filteredBlock.provideTransaction(transaction)
//        return filteredBlock
//    }
//
//    @Synchronized
//    fun applyAndUpdate(tx: Transaction): Boolean {
//        if (contains(tx.getTxId().getBytes())) return true
//        var found = false
//        val flag = getUpdateFlag()
//        for (output in tx.getOutputs()) {
//            val script: Script = output.getScriptPubKey()
//            for (chunk in script.getChunks()) {
//                if (!chunk.isPushData()) continue
//                if (contains(chunk.data)) {
//                    val isSendingToPubKeys = ScriptPattern.isP2PK(script) || ScriptPattern.isSentToMultisig(script)
//                    if (flag == BloomUpdate.UPDATE_ALL || flag == BloomUpdate.UPDATE_P2PUBKEY_ONLY && isSendingToPubKeys) insert(
//                        output.getOutPointFor()
//                    )
//                    found = true
//                }
//            }
//        }
//        if (found) return true
//        for (input in tx.getInputs()) {
//            if (contains(input.getOutpoint().unsafeBitcoinSerialize())) {
//                return true
//            }
//            for (chunk in input.getScriptSig().getChunks()) {
//                if (chunk.isPushData() && contains(chunk.data)) return true
//            }
//        }
//        return false
//    }

    @Synchronized
    override fun equals(o: Any?): Boolean {
        if (this === o) return true
        if (o == null || javaClass != o.javaClass) return false
        val other: BloomFilter = o as BloomFilter
        return hashFuncs == other.hashFuncs && nTweak == other.nTweak && Arrays.equals(data, other.data)
    }

    @Synchronized
    override fun hashCode(): Int {
        return Objects.hash(hashFuncs, nTweak, Arrays.hashCode(data))
    }

}