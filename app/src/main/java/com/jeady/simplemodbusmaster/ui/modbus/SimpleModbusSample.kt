package com.jeady.simplemodbusmaster.ui.modbus

import android.content.Context
import android.util.Log
import com.jeady.simple_modbus_master.SimpleModbus
import com.jeady.simple_modbus_master.SimpleModbusExceptionCode

private const val TAG = "SimpleModbusSample"

object SimpleModbusSample {
    private lateinit var simpleModbus: SimpleModbus
    fun init(): SimpleModbusSample {
        if(!SimpleModbusSample::simpleModbus.isInitialized){
            simpleModbus = SimpleModbus("/dev/ttyS7", 1000)
        }
        return this
    }

    /**
     *  send command that set 0xff for 0x1f00 to 0x1f09 to device‘10’
     */
    @OptIn(ExperimentalStdlibApi::class)
    fun sampleWrite3Registers() {
        simpleModbus.write(
            SimpleModbus.createRequestWriteMultipleRegisters(0x10,
                0x1F00,
                3,
                ShortArray(2).apply {
                    fill(0xffff.toShort())
                }
            )
        ) { res ->
            Log.d(TAG, "sampleWrite3Registers: response $res")
            if (res.err!= SimpleModbusExceptionCode.NoError) {
                // response error
            } else {
                // get response
            }
        }
    }

    /**
     *  send command that read 0x1f00 to 0x1f03
     */
    @OptIn(ExperimentalStdlibApi::class)
    fun sampleRead3Register() {
        simpleModbus.write(SimpleModbus.createRequestReadHoldingRegisters(0x10, 0x1F00, 3)) { res ->
            Log.d(TAG, "sampleRead3Register: response $res")
            if (res.err!= SimpleModbusExceptionCode.NoError) {
                // response error
            } else {
                // get response
            }
        }
    }

    @OptIn(ExperimentalStdlibApi::class)
    fun sampleWritePdu(pduString: String="101f00000a14000100020003", slaveId: Int=0x10){
        simpleModbus.write(SimpleModbus.createRequestFromPduString(slaveId, pduString)){ res->
            Log.d(TAG, "sampleWritePdu: response $res")
            if (res.err!= SimpleModbusExceptionCode.NoError) {
                // response error
            } else {
                // get response
            }
        }
    }
}