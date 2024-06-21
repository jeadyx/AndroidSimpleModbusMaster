package com.jeady.simple_modbus_master

import android.serialport.SerialPort
import android.util.Log
import java.io.InputStream
import java.io.OutputStream
import kotlin.concurrent.thread

private const val TAG = "SerialPortController"
object SerialPortController {
    private var sp: SerialPort?=null
    private lateinit var inputStream: InputStream
    private lateinit var outputStream: OutputStream
    fun open(path: String, baudRate: Int=9600): SerialPortController{
        sp ?: run {
            Log.d(TAG, "open: $path $baudRate")
            sp = SerialPort.newBuilder(path, baudRate).build()
            inputStream = sp!!.inputStream
            outputStream = sp!!.outputStream
        }
        return this
    }
    fun open(path: String, baudRate: Int=9600, dataBits: Int=8, stopBits: Int=1, parity: Int=0, flags: Int=0): SerialPortController{
        sp ?: run {
            sp = SerialPort
                .newBuilder(path, baudRate)
                .dataBits(dataBits)
                .stopBits(stopBits)
                .parity(parity)
                .flags(flags)
                .build()
            inputStream = sp!!.inputStream
            outputStream = sp!!.outputStream
        }
        return this
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
}