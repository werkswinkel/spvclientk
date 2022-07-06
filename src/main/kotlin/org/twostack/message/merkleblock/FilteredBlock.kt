package org.twostack.message.merkleblock

import org.twostack.bitcoin4j.Sha256Hash
import org.twostack.bitcoin4j.block.Block
import org.twostack.bitcoin4j.exception.ProtocolException
import org.twostack.bitcoin4j.exception.VerificationException
import org.twostack.bitcoin4j.params.NetworkParameters
import org.twostack.bitcoin4j.transaction.Transaction
import java.io.ByteArrayOutputStream
import java.io.IOException
import java.io.OutputStream
import java.util.*

class FilteredBlock {
    /*
    private var header: Block? = null

    private var merkleTree: PartialMerkleTree? = null
    private var cachedTransactionHashes: List<Sha256Hash>? = null

    // A set of transactions whose hashes are a subset of getTransactionHashes()
    // These were relayed as a part of the filteredblock getdata, ie likely weren't previously received as loose transactions
    private val associatedTransactions: MutableMap<Sha256Hash, Transaction> = HashMap<Sha256Hash, Transaction>()

    @Throws(ProtocolException::class)
    fun FilteredBlock(payloadBytes: ByteArray) {
        super(params, payloadBytes, 0)
    }

//    fun FilteredBlock(params: NetworkParameters?, header: Block?, pmt: PartialMerkleTree?) {
//        super(params)
//        this.header = header
//        merkleTree = pmt
//    }

    @Throws(IOException::class)
    fun serialize() : ByteArray{
        val stream = ByteArrayOutputStream()
        if (header.getTransactions() == null) header.bitcoinSerializeToStream(stream) else header.cloneAsHeader()
            .bitcoinSerializeToStream(stream)
        merkleTree.bitcoinSerializeToStream(stream)
    }

    @Throws(ProtocolException::class)
    protected fun parse() {
        val headerBytes = ByteArray(Block.HEADER_SIZE)
        System.arraycopy(payload, 0, headerBytes, 0, Block.HEADER_SIZE)
        header = params.getDefaultSerializer().makeBlock(headerBytes)
        merkleTree = PartialMerkleTree(params, payload, Block.HEADER_SIZE)
        length = Block.HEADER_SIZE + merkleTree.getMessageSize()
    }

    /**
     * Gets a list of leaf hashes which are contained in the partial merkle tree in this filtered block
     *
     * @throws ProtocolException If the partial merkle block is invalid or the merkle root of the partial merkle block doesn't match the block header
     */
    @Throws(VerificationException::class)
    fun getTransactionHashes(): List<Sha256Hash?> {
        if (cachedTransactionHashes != null) return Collections.unmodifiableList(cachedTransactionHashes)
        val hashesMatched: List<Sha256Hash> = LinkedList<Sha256Hash>()
        return if (header.getMerkleRoot().equals(merkleTree.getTxnHashAndMerkleRoot(hashesMatched))) {
            cachedTransactionHashes = hashesMatched
            Collections.unmodifiableList(cachedTransactionHashes)
        } else throw VerificationException("Merkle root of block header does not match merkle root of partial merkle tree.")
    }

    /**
     * Gets a copy of the block header
     */
    fun getBlockHeader(): Block? {
        return header.cloneAsHeader()
    }

    /** Gets the hash of the block represented in this Filtered Block  */
    fun getHash(): Sha256Hash {
        return header.getHash()
    }

    /**
     * Provide this FilteredBlock with a transaction which is in its Merkle tree.
     * @return false if the tx is not relevant to this FilteredBlock
     */
    @Throws(VerificationException::class)
    fun provideTransaction(tx: Transaction): Boolean {
        val hash: Sha256Hash = Sha256Hash.wrap(tx.transactionIdBytes)
        if (getTransactionHashes().contains(hash)) {
            associatedTransactions[hash] = tx
            return true
        }
        return false
    }

    /** Returns the [PartialMerkleTree] object that provides the mathematical proof of transaction inclusion in the block.  */
    fun getPartialMerkleTree(): PartialMerkleTree? {
        return merkleTree
    }

    /** Gets the set of transactions which were provided using provideTransaction() which match in getTransactionHashes()  */
    fun getAssociatedTransactions(): Map<Sha256Hash?, Transaction?>? {
        return Collections.unmodifiableMap(associatedTransactions)
    }

    /** Number of transactions in this block, before it was filtered  */
    fun getTransactionCount(): Int {
        return merkleTree.getTransactionCount()
    }

    override fun equals(o: Any?): Boolean {
        if (this === o) return true
        if (o == null || javaClass != o.javaClass) return false
        val other = o as FilteredBlock
        return associatedTransactions == other.associatedTransactions && header.equals(other.header) && merkleTree.equals(
            other.merkleTree
        )
    }

    override fun hashCode(): Int {
        return Objects.hash(associatedTransactions, header, merkleTree)
    }

    override fun toString(): String? {
        return "FilteredBlock{merkleTree=$merkleTree, header=$header}"
    }
*/
}