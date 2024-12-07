package com.jeady.simple_modbus_master

import android.serialport.SerialPort
import android.util.Log
import java.io.IOException
import java.io.InputStream
import java.io.OutputStream
import kotlin.collections.copyOfRange
import kotlin.collections.slice
import kotlin.concurrent.thread
import kotlin.let
import kotlin.ranges.until
import kotlin.run

private const val TAG = "SerialPortController"
class SerialPortController(path: String, baudRate: Int=9600) {
    private var sp: SerialPort?=null
    private lateinit var inputStream: InputStream
    private lateinit var outputStream: OutputStream
    init{
        sp = SerialPort.newBuilder(path, baudRate).build()
        sp?.let {
            inputStream = it.inputStream
            outputStream = it.outputStream
        } ?: run{
            throw IOException("error serial")
        }
    }
    constructor(path: String, baudRate: Int=9600, dataBits: Int=8, stopBits: Int=1, parity: Int=0, flags: Int=0) : this(path, baudRate) {
        sp = SerialPort.newBuilder(path, baudRate)
            .dataBits(dataBits)
            .stopBits(stopBits)
            .parity(parity)
            .flags(flags)
            .build()
        sp?.let {
            inputStream = it.inputStream
            outputStream = it.outputStream
        } ?: run{
            throw IOException("error serial")
        }
    }
    fun test(){
        Log.d(TAG, "test: ${sp?.baudrate}")
    }

    fun close(){
        inputStream.close()
        outputStream.close()
        sp!!.close()
        sp = null
    }

    fun write(data: ByteArray): Boolean{
        try {
            outputStream.write(data)
            return true
        }catch (e: Exception){
            Log.e(TAG, "write: ${e.message}")
            return false
        }
    }

    fun readWhile(onReadData: (data: ByteArray, size: Int)->Unit){
        thread {
            while (true) {
                val buffer = ByteArray(1024)
                val len = inputStream.read(buffer)
                if (len > 0) {
                    onReadData(buffer, len)
                }
            }
        }
    }

    fun read(): ByteArray{
        try {
            val buffer = ByteArray(1024)
            val len = inputStream.read(buffer)
            return buffer.copyOfRange(0, len)
        }catch (e: Exception){
            Log.e(TAG, "read: ${e.message}")
            return ByteArray(0)
        }
    }

    fun readUntilTimeout(timeout: Long, onReadData: (data: ByteArray, size: Int)->Unit){
        thread {
            val buffer = ByteArray(1024)
            var len: Int
            val startTime = System.currentTimeMillis()
            while (true) {
                if (System.currentTimeMillis() - startTime > timeout) {
                    onReadData(byteArrayOf(), -1)
                    break
                }
                len = inputStream.read(buffer)
                if (len > 0) {
                    onReadData(buffer, len)
                    break
                }
            }
        }
    }

//    @Synchronized
    fun readUntilTimeout(timeout: Long, request: ByteArray, onReadData: (data: ByteArray, size: Int)->Unit){
        var buffer = byteArrayOf()
        var len: Int
        val startTime = System.currentTimeMillis()
        while (true) {
            if (System.currentTimeMillis() - startTime > timeout) {
                onReadData(byteArrayOf(), -1)
                break
            }
            if(inputStream.available()>0) {
                val bufferTmp = ByteArray(512)
                len = inputStream.read(bufferTmp, 0, 2)
                buffer += bufferTmp.slice(0 until len)
                when (bufferTmp[1]) {
                    0x03.toByte() -> {
                        len = inputStream.read(bufferTmp, 0, 1)
                        buffer += bufferTmp.slice(0 until len)
                        val frameSize = bufferTmp[0].let {
                            if (it < 0) it + 256 else it.toInt()
                        }
                        len = inputStream.read(bufferTmp, 0, frameSize + 2)
                        buffer += bufferTmp.slice(0 until len)
                    }

                    0x10.toByte() -> {
                        len = inputStream.read(bufferTmp, 0, 6)
                        buffer += bufferTmp.slice(0 until len)
                    }

                    (request[1] + 0x80).toByte() -> {
                        len = inputStream.read(bufferTmp, 0, 3)
                        buffer += bufferTmp.slice(0 until len)
                    }
                }
                if (len >= 3 && request.slice(0..1) == buffer.slice(0..1)) {
                    onReadData(buffer, buffer.size)
                    break
                }
            }
        }
    }

    fun clearCache(): Int{
        val buffer = ByteArray(4096)
        Log.d(TAG, "clearCache: ${inputStream.available()}")
        if(inputStream.available()>0){
            return inputStream.read(buffer)
        }else{
            return -1
        }
    }
}