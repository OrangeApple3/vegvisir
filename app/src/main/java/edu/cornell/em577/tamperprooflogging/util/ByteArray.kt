package edu.cornell.em577.tamperprooflogging.util

private val HEX_CHARS = "0123456789ABCDEF".toCharArray()

/**
 * Converts the ByteArray to a hex String representation. Paired with String.hexStringToByteArray.
 */
fun ByteArray.toHex() : String{
    val result = StringBuffer()

    forEach {
        val octet = it.toInt()
        val firstIndex = (octet and 0xF0).ushr(4)
        val secondIndex = octet and 0x0F
        result.append(HEX_CHARS[firstIndex])
        result.append(HEX_CHARS[secondIndex])
    }

    return result.toString()
}