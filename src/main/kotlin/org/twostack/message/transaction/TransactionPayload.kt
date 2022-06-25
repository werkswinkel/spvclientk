package org.twostack.message.transaction

import org.twostack.bitcoin4j.transaction.Transaction
import java.nio.ByteBuffer

class TransactionPayload(val tx: Transaction) {

    companion object {
        fun fromByteArray(buffer : ByteArray) : TransactionPayload{
            return TransactionPayload(Transaction(ByteBuffer.wrap(buffer)))
        }

    }
}