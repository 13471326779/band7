package com.band7.tool.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream
import java.io.PrintStream
import javax.script.ScriptEngineManager

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ScriptScreen() {
    var code by remember { mutableStateOf('''// Simple calculator helper
fun add(a, b) { return a + b; }
fun multiply(a, b) { return a * b; }

console.log("3 + 5 = " + add(3, 5));
console.log("4 * 7 = " + multiply(4, 7));
''') }
    var output by remember { mutableStateOf("") }
    var isRunning by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Script Mode") },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    titleContentColor = Color.White
                )
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier.fillMaxSize().padding(padding).padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text("Write and execute JavaScript snippets", fontSize = 13.sp, color = Color.Gray)

            OutlinedTextField(
                value = code,
                onValueChange = { code = it },
                modifier = Modifier.fillMaxWidth().weight(1f),
                textStyle = LocalTextStyle.current.copy(
                    fontFamily = FontFamily.Monospace,
                    fontSize = 12.sp
                )
            )

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(
                    onClick = {
                        isRunning = true
                        output = "Running script...\n"
                        try {
                            val engine = ScriptEngineManager().getEngineByExtension("js")
                            if (engine != null) {
                                val baos = ByteArrayOutputStream()
                                val ps = PrintStream(baos)
                                engine.put("console", object {
                                    fun log(msg: Any?) { ps.println(msg) }
                                })
                                engine.eval(code)
                                output += baos.toString()
                            } else {
                                output += "JavaScript engine not available on this device.\n"
                                output += "This is a preview of the scripting feature.\n"
                            }
                        } catch (e: Exception) {
                            output += "Error: " + e.message + "\n"
                        }
                        isRunning = false
                    },
                    enabled = !isRunning
                ) { Text(if (isRunning) "Running..." else "Run Script") }

                OutlinedButton(onClick = {
                    code = ""; output = ""
                }) { Text("Clear") }
            }

            if (output.isNotEmpty()) {
                Card(modifier = Modifier.fillMaxWidth().heightIn(min = 100.dp)) {
                    Text(
                        output,
                        modifier = Modifier.padding(12.dp).verticalScroll(rememberScrollState()),
                        fontFamily = FontFamily.Monospace,
                        fontSize = 12.sp
                    )
                }
            }
        }
    }
}
