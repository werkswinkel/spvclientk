package org.twostack.message.merkleblock

import com.google.common.base.Objects
import org.twostack.bitcoin4j.Sha256Hash
import org.twostack.bitcoin4j.Utils
import org.twostack.bitcoin4j.VarInt
import org.twostack.bitcoin4j.block.Block
import org.twostack.bitcoin4j.exception.ProtocolException
import org.twostack.bitcoin4j.exception.VerificationException
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.IOException
import java.io.OutputStream
import java.util.*

/**
 *
 * A data structure that contains proofs of block inclusion for one or more transactions, in an efficient manner.
 *
 *
 * The encoding works as follows: we traverse the tree in depth-first order, storing a bit for each traversed node,
 * signifying whether the node is the parent of at least one matched leaf txid (or a matched txid itself). In case we
 * are at the leaf level, or this bit is 0, its merkle node hash is stored, and its children are not explored further.
 * Otherwise, no hash is stored, but we recurse into both (or the only) child branch. During decoding, the same
 * depth-first traversal is performed, consuming bits and hashes as they were written during encoding.
 *
 *
 * The serialization is fixed and provides a hard guarantee about the encoded size,
 * <tt>SIZE &lt;= 10 + ceil(32.25*N)</tt> where N represents the number of leaf nodes of the partial tree. N itself
 * is bounded by:
 *
 *
 *
 * N &lt;= total_transactions<br></br>
 * N &lt;= 1 + matched_transactions*tree_height
 *
 *
 *
 * <pre>The serialization format:
 * - uint32     total_transactions (4 bytes)
 * - varint     number of hashes   (1-3 bytes)
 * - uint256[]  hashes in depth-first order (&lt;= 32*N bytes)
 * - varint     number of bytes of flag bits (1-3 bytes)
 * - byte[]     flag bits, packed per 8 in a byte, least significant bit first (&lt;= 2*N-1 bits)
 * The size constraints follow from this.</pre>
 *
 *
 * Instances of this class are not safe for use by multiple threads.
 */
class PartialMerkleTree {
    // the total number of transactions in the block
    var transactionCount = 0
        private set

    // node-is-parent-of-matched-txid bits
    private var matchedChildBits: ByteArray? = null

    // txids and internal hashes
    private var hashes: MutableList<Sha256Hash>? = null

    constructor(payloadBytes: ByteArray) {
        parse(payloadBytes)
    }

    /**
     * Constructs a new PMT with the given bit set (little endian) and the raw list of hashes including internal hashes,
     * taking ownership of the list.
     */
    constructor(bits: ByteArray, hashes: MutableList<Sha256Hash>?, origTxCount: Int) {
        matchedChildBits = bits
        this.hashes = hashes
        transactionCount = origTxCount
    }

    fun serialize() : ByteArray {
        val stream = ByteArrayOutputStream()
        Utils.uint32ToByteStreamLE(transactionCount.toLong(), stream)
        stream.write(VarInt(hashes!!.size.toLong()).encode())
        for (hash in hashes!!) stream.write(hash.reversedBytes)
        stream.write(VarInt(matchedChildBits!!.size.toLong()).encode())
        stream.write(matchedChildBits!!)

        return stream.toByteArray()
    }

    private fun parse( payload : ByteArray) {
        val bis = ByteArrayInputStream(payload)
        transactionCount = Utils.readUint32FromStream(bis).toInt()
        val nHashes = VarInt.fromStream(bis).intValue()
        hashes = ArrayList(nHashes)
        for (i in 0 until nHashes) hashes?.add(Sha256Hash.wrapReversed(bis.readNBytes(32)))
        val nFlagBytes = VarInt.fromStream(bis).intValue()
        matchedChildBits = bis.readNBytes(nFlagBytes)
    }

    protected fun parseLite() {}
    private class ValuesUsed {
        var bitsUsed = 0
        var hashesUsed = 0
    }

    // recursive function that traverses tree nodes, consuming the bits and hashes produced by TraverseAndBuild.
    // it returns the hash of the respective node.
    @Throws(VerificationException::class)
    private fun recursiveExtractHashes(
        height: Int,
        pos: Int,
        used: ValuesUsed,
        matchedHashes: MutableList<Sha256Hash>
    ): Sha256Hash {
        if (used.bitsUsed >= matchedChildBits!!.size * 8) {
            // overflowed the bits array - failure
            throw VerificationException("PartialMerkleTree overflowed its bits array")
        }
        val parentOfMatch: Boolean = Utils.checkBitLE(matchedChildBits, used.bitsUsed++)
        return if (height == 0 || !parentOfMatch) {
            // if at height 0, or nothing interesting below, use stored hash and do not descend
            if (used.hashesUsed >= hashes!!.size) {
                // overflowed the hash array - failure
                throw VerificationException("PartialMerkleTree overflowed its hash array")
            }
            val hash = hashes!![used.hashesUsed++]
            if (height == 0 && parentOfMatch) // in case of height 0, we have a matched txid
                matchedHashes.add(hash)
            hash
        } else {
            // otherwise, descend into the subtrees to extract matched txids and hashes
            val left = recursiveExtractHashes(height - 1, pos * 2, used, matchedHashes).bytes
            val right: ByteArray
            if (pos * 2 + 1 < getTreeWidth(transactionCount, height - 1)) {
                right = recursiveExtractHashes(height - 1, pos * 2 + 1, used, matchedHashes).bytes
                if (Arrays.equals(
                        right,
                        left
                    )
                ) throw VerificationException("Invalid merkle tree with duplicated left/right branches")
            } else {
                right = left
            }
            // and combine them before returning
            combineLeftRight(left, right)
        }
    }

    /**
     * Extracts tx hashes that are in this merkle tree
     * and returns the merkle root of this tree.
     *
     * The returned root should be checked against the
     * merkle root contained in the block header for security.
     *
     * @param matchedHashesOut A list which will contain the matched txn (will be cleared).
     * @return the merkle root of this merkle tree
     * @throws ProtocolException if this partial merkle tree is invalid
     */
    @Throws(VerificationException::class)
    fun getTxnHashAndMerkleRoot(matchedHashesOut: MutableList<Sha256Hash>): Sha256Hash {
        matchedHashesOut.clear()

        // An empty set will not work
        if (transactionCount == 0) throw VerificationException("Got a CPartialMerkleTree with 0 transactions")

        // there can never be more hashes provided than one for every txid
        if (hashes!!.size > transactionCount) throw VerificationException("Got a CPartialMerkleTree with more hashes than transactions")
        // there must be at least one bit per node in the partial tree, and at least one node per hash
        if (matchedChildBits!!.size * 8 < hashes!!.size) throw VerificationException("Got a CPartialMerkleTree with fewer matched bits than hashes")
        // calculate height of tree
        var height = 0
        while (getTreeWidth(transactionCount, height) > 1) height++
        // traverse the partial tree
        val used = ValuesUsed()
        val merkleRoot = recursiveExtractHashes(height, 0, used, matchedHashesOut)
        // verify that all bits were consumed (except for the padding caused by serializing it as a byte sequence)
        if ((used.bitsUsed + 7) / 8 != matchedChildBits!!.size ||  // verify that all hashes were consumed
            used.hashesUsed != hashes!!.size
        ) throw VerificationException("Got a CPartialMerkleTree that didn't need all the data it provided")
        return merkleRoot
    }

    override fun equals(o: Any?): Boolean {
        if (this === o) return true
        if (o == null || javaClass != o.javaClass) return false
        val other = o as PartialMerkleTree
        return transactionCount == other.transactionCount && hashes == other.hashes && Arrays.equals(
            matchedChildBits,
            other.matchedChildBits
        )
    }

    override fun hashCode(): Int {
        return Objects.hashCode(transactionCount, hashes, Arrays.hashCode(matchedChildBits))
    }

    override fun toString(): String {
        return "PartialMerkleTree{" +
                "transactionCount=" + transactionCount +
                ", matchedChildBits=" + Arrays.toString(matchedChildBits) +
                ", hashes=" + hashes +
                '}'
    }

    companion object {
        /**
         * Calculates a PMT given the list of leaf hashes and which leaves need to be included. The relevant interior hashes
         * are calculated and a new PMT returned.
         */
        fun buildFromLeaves(includeBits: ByteArray, allLeafHashes: List<Sha256Hash>): PartialMerkleTree {
            // Calculate height of the tree.
            var height = 0
            while (getTreeWidth(allLeafHashes.size, height) > 1) height++
            val bitList: MutableList<Boolean> = ArrayList()
            val hashes: MutableList<Sha256Hash> = ArrayList()
            traverseAndBuild(height, 0, allLeafHashes, includeBits, bitList, hashes)
            val bits = ByteArray(Math.ceil(bitList.size / 8.0).toInt())
            for (i in bitList.indices) if (bitList[i]) Utils.setBitLE(bits, i)
            return PartialMerkleTree(bits, hashes, allLeafHashes.size)
        }

        // Based on CPartialMerkleTree::TraverseAndBuild in Bitcoin Core.
        private fun traverseAndBuild(
            height: Int, pos: Int, allLeafHashes: List<Sha256Hash>, includeBits: ByteArray,
            matchedChildBits: MutableList<Boolean>, resultHashes: MutableList<Sha256Hash>
        ) {
            var parentOfMatch = false
            // Is this node a parent of at least one matched hash?
            var p = pos shl height
            while (p < pos + 1 shl height && p < allLeafHashes.size) {
                if (Utils.checkBitLE(includeBits, p)) {
                    parentOfMatch = true
                    break
                }
                p++
            }
            // Store as a flag bit.
            matchedChildBits.add(parentOfMatch)
            if (height == 0 || !parentOfMatch) {
                // If at height 0, or nothing interesting below, store hash and stop.
                resultHashes.add(calcHash(height, pos, allLeafHashes))
            } else {
                // Otherwise descend into the subtrees.
                val h = height - 1
                val p = pos * 2
                traverseAndBuild(h, p, allLeafHashes, includeBits, matchedChildBits, resultHashes)
                if (p + 1 < getTreeWidth(allLeafHashes.size, h)) traverseAndBuild(
                    h,
                    p + 1,
                    allLeafHashes,
                    includeBits,
                    matchedChildBits,
                    resultHashes
                )
            }
        }

        private fun calcHash(height: Int, pos: Int, hashes: List<Sha256Hash>): Sha256Hash {
            if (height == 0) {
                // Hash at height 0 is just the regular tx hash itself.
                return hashes[pos]
            }
            val h = height - 1
            val p = pos * 2
            val left = calcHash(h, p, hashes)
            // Calculate right hash if not beyond the end of the array - copy left hash otherwise.
            val right: Sha256Hash
            right = if (p + 1 < getTreeWidth(hashes.size, h)) {
                calcHash(h, p + 1, hashes)
            } else {
                left
            }
            return combineLeftRight(left.bytes, right.bytes)
        }

        // helper function to efficiently calculate the number of nodes at given height in the merkle tree
        private fun getTreeWidth(transactionCount: Int, height: Int): Int {
            return transactionCount + (1 shl height) - 1 shr height
        }

        private fun combineLeftRight(left: ByteArray, right: ByteArray): Sha256Hash {
            return Sha256Hash.wrapReversed(
                Sha256Hash.hashTwice(
                    Utils.reverseBytes(left), 0, 32,
                    Utils.reverseBytes(right), 0, 32
                )
            )
        }
    }
}

