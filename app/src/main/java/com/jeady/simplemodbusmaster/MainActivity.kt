package com.jeady.simplemodbusmaster

import android.content.Context.MODE_PRIVATE
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.jeady.simple_modbus_master.SimpleModbus
import com.jeady.simple_modbus_master.SimpleModbusResponse
import com.jeady.simplemodbusmaster.ui.common.ButtonText
import com.jeady.simplemodbusmaster.ui.theme.SimpleModbusMasterTheme

private const val TAG = "MainActivity"

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            SimpleModbusMasterTheme {
                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    ModbusSample(Modifier.padding(innerPadding))
                }
            }
        }
    }
}

var response by mutableStateOf(SimpleModbusResponse())
@OptIn(ExperimentalStdlibApi::class)
@Preview
@Composable
fun ModbusSample(modifier: Modifier = Modifier) {
    val context = LocalContext.current
    var slaveId by remember { mutableStateOf("") }
    var pdu by remember { mutableStateOf("") }
    LaunchedEffect(Unit){
        SimpleModbus.init("/dev/ttyS7", 9600)
        context.getSharedPreferences("modbus", MODE_PRIVATE).apply {
            slaveId = getString("slaveId", "1") ?: ""
            pdu = getString("pdu", "03 0001 0002") ?: ""
        }
    }
    Box(modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(Modifier.fillMaxWidth(0.8f),
            verticalArrangement = Arrangement.spacedBy(10.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text("以下编辑框均填写十六进制数，如FF(All hex input)", fontSize = 30.sp)
            OutlinedTextField(value = slaveId, onValueChange = {
                if (it.matches(Regex("[0-9a-f]{0,2}"))) slaveId = it
            }, Modifier.fillMaxWidth(), label = { Text("输入从机地址(slave id)[Additional address]", color= Color.Gray) })
            OutlinedTextField(value = pdu, onValueChange = {
                if (Regex("[0-9a-f ]*").matches(it)) pdu = it
            }, Modifier.fillMaxWidth(), label = { Text("要写入的pdu数据(pdu data)", color= Color.Gray) },
                suffix = {
                    if(pdu.isNotEmpty()) Icon(Icons.Filled.Clear, "", Modifier.clickable { pdu = "" })
                })
            ButtonText("向 ${(slaveId.ifBlank { "0" }).toInt(16)} 号从机发送请求\n(send command to slave ${(slaveId.ifBlank { "0" }).toInt(16)})") {
                sendAndGetResponse((slaveId.ifBlank { "0" }).toInt(16), pdu.replace(" ", ""))
                // 保存数据
                context.getSharedPreferences("modbus", MODE_PRIVATE).edit().putString("slaveId", slaveId).putString("pdu", pdu).apply()
            }
            Text("请 求(request)：\t${response.request?.toHexString()?.replace(Regex("(..)"), "$1 ")}",
                Modifier.align(Alignment.Start), fontFamily = FontFamily.Monospace)
            Text("响应(response)：\t${response.response?.toHexString()?.replace(Regex("(..)"), "$1 ")}",
                Modifier.align(Alignment.Start), fontFamily = FontFamily.Monospace)
            Spacer(Modifier.height(30.dp))
            Image(painterResource(R.drawable.pdu), "pdu description", Modifier.fillMaxWidth())
        }
    }
}

fun sendAndGetResponse(slaveId: Int, pduString: String) {
    try {
        SimpleModbus.write(
            SimpleModbus.createRequestFromPduString(slaveId, pduString)
        ) { res ->
            Log.d(TAG, "sampleWrite3Registers: response $res")
            response = res
        }
    }catch (e: Exception){
        Log.e(TAG, "sendAndGetResponse: ", e)
    }
}
