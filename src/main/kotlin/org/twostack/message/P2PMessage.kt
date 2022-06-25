package org.twostack.message

interface P2PMessage {
    fun serialize() : ByteArray
}