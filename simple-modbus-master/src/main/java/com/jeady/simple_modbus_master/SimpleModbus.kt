package com.jeady.simple_modbus_master

import android.content.Context
import android.util.Log
import com.jeady.simple_modbus_master.Util.toTwoByte
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import kotlin.experimental.and

object SimpleModbus{
    private val TAG = "SimpleModbus"
    lateinit var serialPort: SerialPortController
    private var readTimeout: Long = 0

    /**
     * init modbus, must be called first
     * @param serialPath serial port path, example /dev/ttyS1
     * @param baudRate baud rate, default 9600
     * @param readTimeout get response by the timeout
     * @return SimpleModbus
     */
    fun init(serialPath: String, baudRate: Int=9600, readTimeout: Long=500): SimpleModbus {
        if(!SimpleModbus::serialPort.isInitialized){
            serialPort = SerialPortController.open(serialPath, baudRate)
            SimpleModbus.readTimeout = readTimeout
        }
        return this
    }

    /**
     * clear serialport buff
     * @return cleared buffer size
     */
    fun clearCache(): Int{
        return serialPort.clearCache()
    }

    /**
     * create a custom read request
     * @param slaveAddress slave address
     * @param functionCode function code
     * @param startAddress start address of the register
     * @param quantity quantity of the register
     * @return request
     * @see ModbusFunctionCode; it can be used by "ModbusFunctionCode.ReadHoldingRegisters.value"
     */
    fun createCustomReadRequest(slaveAddress: Int, functionCode: Int, startAddress: Int, quantity: Int): ByteArray {
        val request = ByteArray(6)
        request[0] = slaveAddress.toByte()
        request[1] = functionCode.toByte()
        request[2] = (startAddress shr 8).toByte()
        request[3] = (startAddress and 0xFF).toByte()
        request[4] = (quantity shr 8).toByte()
        request[5] = (quantity and 0xFF).toByte()
        val crcCode = Util.crc16(request)
        return request + crcCode
    }

    /**
     * create a custom write request
     * @param slaveAddress slave address
     * @param functionCode function code
     * @param startAddress start address of the register
     * @param data data to write
     * @return request
     * @see ModbusFunctionCode; it can be used by "ModbusFunctionCode.ReadHoldingRegisters.value"
     */
    fun createCustomWriteRequest(slaveAddress: Int, functionCode: Int, startAddress: Int, quantity: Int, data: ByteArray): ByteArray{
        val request = ByteArray(7 + quantity*2)
        request[0] = slaveAddress.toByte()
        request[1] = functionCode.toByte()
        request[2] = (startAddress shr 8).toByte()
        request[3] = (startAddress and 0xFF).toByte()
        if (quantity > 0xFF) {
            request[4] = (quantity shr 8).toByte()
            request[5] = (quantity and 0xFF).toByte()
        }else{
            request[4] = 0x00
            request[5] = quantity.toByte()
        }
        (quantity*2).let {
            if(it>0xff){
                request[6] = 0xff.toByte()
            }else{
                request[6] = it.toByte()
            }
        }
        for(i in 0..<quantity*2){
            request[7 + i] = data.getOrElse(i) { 0 }
        }
        val crcCode = Util.crc16(request)
        return request + crcCode
    }

    /**
     * create a custom write request
     * @param slaveAddress slave address
     * @param functionCode function code
     * @param startAddress start address of the register
     * @param data short data array to write
     * @return request
     * @see ModbusFunctionCode; it can be used by "ModbusFunctionCode.ReadHoldingRegisters.value"
     */
    fun createCustomWriteRequest(slaveAddress: Int, functionCode: Int, startAddress: Int, data: ShortArray): ByteArray{
        val quantity = data.size
        val request = ByteArray(7 + quantity*2)
        request[0] = slaveAddress.toByte()
        request[1] = functionCode.toByte()
        request[2] = (startAddress shr 8).toByte()
        request[3] = (startAddress and 0xFF).toByte()
        if (quantity > 0xFF) {
            request[4] = (quantity shr 8).toByte()
            request[5] = (quantity and 0xFF).toByte()
        }
        (quantity*2).let {
            if(it>0xff){
                request[6] = 0xff.toByte()
            }else{
                request[6] = it.toByte()
            }
        }
        for (i in data){
            if(i.toInt()>0xff){
                request[7 + i * 2] = (i.toTwoByte()[0] and 0xFF.toByte())
                request[8 + i * 2] = (i.toTwoByte()[1] and 0xFF.toByte())
            }else{
                request[7 + i * 2] = 0x00.toByte()
                request[8 + i * 2] = (i.toByte() and 0xFF.toByte())
            }
        }
        val crcCode = Util.crc16(request)
        return request + crcCode
    }

    /**
     * create request for read holding registers
     * @param slaveAddress slave address
     * @param startAddress start address of register
     * @param quantity quantity of registers will reading
     * @sample SimpleModbusSample.sampleRead3Register
     */
    fun createRequestReadHoldingRegisters(slaveAddress: Int, startAddress: Int, quantity: Int): ByteArray {
        return createCustomReadRequest(slaveAddress, ModbusFunctionCode.ReadHoldingRegisters.value, startAddress, quantity)
    }

    /**
     * create request for write multiple registers
     * @param slaveAddress slave address
     * @param startAddress start address of register
     * @param quantity quantity of registers
     * @data data byte data to write, its format must be ["high byte", "low byte", "high byte", "low byte",...]
     * @return request request byte array
     */
    fun createRequestWriteMultipleRegisters(slaveAddress: Int, startAddress: Int, quantity: Int, data: ByteArray): ByteArray {
        return createCustomWriteRequest(slaveAddress, ModbusFunctionCode.WriteMultipleRegisters.value, startAddress, quantity, data)
    }

    /**
     * create request for write multiple registers
     * @param slaveAddress slave address
     * @param startAddress register start address
     * @param quantity quantity of registers
     * @param data of short array, its size can smaller to quantity, will be filled with 0x00
     * @return request byte array
     * @sample SimpleModbusSample.sampleWrite3Registers
     */
    fun createRequestWriteMultipleRegisters(slaveAddress: Int, startAddress: Int, quantity: Int, data: ShortArray): ByteArray {
        val byteArray = ByteArray(data.size*2)
        data.forEachIndexed{idx, short->
            val twoByte = short.toTwoByte()
            byteArray[idx*2] = twoByte[0] and 0xFF.toByte()
            byteArray[idx*2+1] = twoByte[1] and 0xFF.toByte()
        }
        return createCustomWriteRequest(slaveAddress, ModbusFunctionCode.WriteMultipleRegisters.value, startAddress, quantity, byteArray)
    }

    /**
     * create request from pdu hex string
     * @param slaveAddress slave address
     * @param pdu pdu hex string
     * @return request data
     * @sample SimpleModbusSample.sampleWritePdu
     */
    @OptIn(ExperimentalStdlibApi::class)
    fun createRequestFromPduString(slaveAddress: Int, pdu: String): ByteArray{
        if(pdu.length<2) return byteArrayOf()
        Log.d(TAG, "createRequestFromPduString: $slaveAddress $pdu")
        val byteSendNoCrc = byteArrayOf(slaveAddress.toByte()) + pdu.hexToByteArray()
        val request = byteSendNoCrc + Util.crc16(byteSendNoCrc)
        return request
    }

    /**
     * send request to device and get response
     * @param request request data
     * @param timeout timeout of get response; init timeout is used if keep it default
     * @param onResponse callback when get response or 'invalid' or 'timeout'
     *
     * @sample SimpleModbusSample.sampleWrite3Registers
     * @sample SimpleModbusSample.sampleRead5Register
     */
    @Synchronized
    fun write(request: ByteArray, timeout: Long=-1, onResponse: (response: SimpleModbusResponse)->Unit){
        val requestTime = LocalDateTime.now()
        SerialPortController.write(request)
        SerialPortController.readUntilTimeout(timeout.takeIf { it > 0 } ?: readTimeout, request) { data, size ->
            if (size > 0) {
                val resData = data.copyOfRange(0, size)
                val validRes = validateResponse(request, resData)
                if (validRes != SimpleModbusExceptionCode.NoError) {
                    onResponse(
                        SimpleModbusResponse(
                            validRes,
                            request, resData, getPduFromAdu(request), getPduFromAdu(resData)
                        )
                    )
                } else {
                    onResponse(
                        SimpleModbusResponse(
                            SimpleModbusExceptionCode.NoError,
                            request, resData, getPduFromAdu(request), getPduFromAdu(resData),
                            requestTime.format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss.SSS")),
                            LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss.SSS"))
                        )
                    )
                }
            } else {
                onResponse(
                    SimpleModbusResponse(
                        SimpleModbusExceptionCode.Timeout,
                        request,
                    )
                )
            }
        }
    }

    /**
     * verify response validate
     * @param all size response
     */
    private fun validateResponse(request: ByteArray, response: ByteArray): SimpleModbusExceptionCode {
        return if (response.size < 4) { // 长度校验
            SimpleModbusExceptionCode.InvalidLength
        }else if(!Util.checkCrc16(response)){ // crc校验
            SimpleModbusExceptionCode.CrcError
        }else if(response[1].toUByte().toInt() == request[1]+0x80){ // 错误码校验
            SimpleModbusExceptionCode.fromInt(response[2].toInt())
        }else{
            SimpleModbusExceptionCode.NoError
        }
    }

    fun getPduFromAdu(adu: ByteArray): ByteArray{
        if(adu.size<4) return byteArrayOf()
        return adu.copyOfRange(1, adu.size-2)
    }
}

data class SimpleModbusResponse(
    val err: SimpleModbusExceptionCode = SimpleModbusExceptionCode.NoError,
    val request: ByteArray?=null,
    val response: ByteArray?=null,
    val requestPdu: ByteArray?=null,
    val responsePdu: ByteArray?=null,
    val requestTime: String="",
    val responseTime: String="",
)
enum class SimpleModbusExceptionCode(val value: Int) {
    NoError(0),
    IllegalFunction(1),
    IllegalDataAddress(2),
    IllegalDataValue(3),
    ServerDeviceFailure(4),
    Acknowledge(5),
    ServerDeviceBusy(6),
    NegativeAcknowledge(7),
    MemoryParityError(8),
    GatewayPathUnavailable(10),
    GatewayTargetDeviceFailedToRespond(11),
    Timeout(900),
    InValidFrame(901),
    CrcError(902),
    InvalidLength(903),
    NotInitialized(905),
    Unknown(999);
    companion object {
        fun fromInt(value: Int) = entries.firstOrNull{ it.value == value }?: Unknown
    }
}

enum class ModbusFunctionCode(val value: Int) {
    ReadCoils(1),
    ReadDiscreteInputs(2),
    ReadHoldingRegisters(3),
    ReadInputRegisters(4),
    WriteSingleCoil(5),
    WriteSingleRegister(6),
    WriteMultipleCoils(15),
    WriteMultipleRegisters(16),
    MaskWriteRegister(22),
    ReadWriteMultipleRegisters(23),
    ReadFifoQueue(24),
    ReadFileRecord(20),
    WriteFileRecord(21),
    Diagnostic(8),
    GetCommEventCounter(11),
    GetCommEventLog(12),
    ReportSlaveId(17),
    ReadDeviceIdentification(43)
}