package com.jeady.simple_modbus_master

import android.content.Context
import android.widget.Toast
import java.text.SimpleDateFormat
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle
import java.util.Locale
import kotlin.UInt
import kotlin.collections.dropLast
import kotlin.collections.takeLast
import kotlin.collections.toUByteArray
import kotlin.text.toHexString

private const val TAG = "Util"
object Util {
    fun showToast(context: Context, msg: String){
        Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
    }
    fun localTimeString(format: String="yyyy-MM-dd HH:mm:ss"): String{
        val dateFormat = SimpleDateFormat(format, Locale.CHINA)
        return dateFormat.format(System.currentTimeMillis())
    }
    fun localDateString(format: String="yyyy-MM-dd"): String{
        val dateFormat = SimpleDateFormat(format, Locale.CHINA)
        return dateFormat.format(System.currentTimeMillis())
    }
    fun weekBefore(before: Long): String{
        return LocalDate.now().minusDays(before).dayOfWeek.getDisplayName(TextStyle.FULL, Locale.CHINA)
    }
    fun dateBefore(before: Long): String{
        return LocalDate.now().minusDays(before).format(DateTimeFormatter.ofPattern("MM-dd"))
    }
    @OptIn(ExperimentalUnsignedTypes::class)
    fun crc16(byteArray: ByteArray): ByteArray{
        var crc16 = 0xFFFF
        val ubBytes = byteArray.toUByteArray()
        for (i in ubBytes.indices) {
            crc16 = crc16 xor ubBytes[i].toInt()
            for (j in 0..7) {
                crc16 = if (crc16 and 1 == 1) {
                    crc16 shr 1 xor 0xA001
                } else {
                    crc16 shr 1
                }
            }
        }
        return crc16.toTwoByte(false)
    }
    fun checkCrc16(uByteArray: ByteArray): Boolean{
        val crc16 = crc16(uByteArray.dropLast(2).toByteArray())
        return crc16[0] == uByteArray[uByteArray.size-2] && crc16[1] == uByteArray[uByteArray.size-1]
    }

    /**
     * convert int to two Byte
     * @param bigEndian true: ex(0x1234) big endian(12,34), false: little endian(34,12)
     */
    fun Int.toTwoByte(bigEndian: Boolean=true): ByteArray{
        val lower = this and 0xFF
        val higher = this shr 8 and 0xFF
        return if(bigEndian){
            byteArrayOf(higher.toByte(), lower.toByte())
        }else{
            byteArrayOf(lower.toByte(), higher.toByte())
        }
    }

    /**
     * convert short to two Byte
     * @param bigEndian true: ex(0x1234) big endian(12,34), false: little endian(34,12)
     */
    fun Short.toTwoByte(bigEndian: Boolean=true): ByteArray{
        return this.toInt().toTwoByte(bigEndian)
    }
    @OptIn(ExperimentalStdlibApi::class)
    fun ByteArray.lastTwoByteToUInt(): Int {
        if (this.size < 2) return -1
        val lastTwoByte = this.takeLast(2).toByteArray()
        return lastTwoByte.toHexString().toInt(16)
    }
    @OptIn(ExperimentalStdlibApi::class)
    fun ByteArray.lastTwoByteToInt(): Int {
        if (this.size < 2) return -1
        val lastTwoByte = this.takeLast(2).toByteArray()
        val high = lastTwoByte[0]
        val lower = lastTwoByte[1]
        var res = byteArrayOf(high, lower).toHexString().toInt(16)
        if(high.toInt() < 0){
            res = -(byteArrayOf(high, lower).toHexString().toInt(16) - 1).xor(0xffff)
        }
        return res
    }
    fun UInt.div(divisor: Float): Float {
        return this.toFloat() / divisor
    }
}