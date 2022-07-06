package org.twostack.message.filter

import org.twostack.bitcoin4j.Utils
import org.twostack.message.bloomfilter.BloomFilter

/**
Varies  | nFilterBytes   | compactSize uint  | Number of bytes in the following filter bit field.
----
Varies  | filter         |  uint8_t[]        | A bit field of arbitrary byte-aligned size.
                                                The maximum size is 36,000 bytes.
----
4       | nHashFuncs     | uint32_t          | The number of hash functions to use in this filter. The maximum value
                                                allowed in this field is 50.
----
4       | nTweak         | uint32_t          | An arbitrary value to add to the seed value in the hash function used
                                                by the bloom filter.
----
1       | nFlags         | uint8_t           | A set of flags that control how outpoints corresponding to a matched
                                                pubkey script are added to the filter. See the table in the Updating A
                                                Bloom Filter subsection below.
----


Bloom Filter Payload
 */
class FilterLoadPayload(val filter : BloomFilter) {
//    private var data: ByteArray = []
    private var hashFuncs: Long = 0
    private var nTweak: Long = 0
    private var nFlags: Byte = 0

    companion object {
//        fun fromByteArray(buffer: ByteArray) : FilterLoadPayload{
//            val filter = BloomFilter(buffer)
//            return FilterLoadPayload(filter)
//        }
    }

    fun serialize() : ByteArray{
        return filter.serialize()
    }

}