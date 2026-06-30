package com.band7.tool.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CalculatorScreen() {
    var display by remember { mutableStateOf("0") }
    var expression by remember { mutableStateOf("") }
    var lastResult by remember { mutableStateOf("") }
    var newInput by remember { mutableStateOf(true) }
    var memory by remember { mutableDoubleStateOf(0.0) }

    fun onButtonClick(text: String) {
        when (text) {
            "C" -> { display = "0"; expression = ""; newInput = true }
            "=" -> {
                try {
                    val result = evaluate(expression + display)
                    display = result
                    expression = ""
                    lastResult = result
                    newInput = true
                } catch (e: Exception) {
                    display = "Error"
                }
            }
            "+/-" -> {
                if (display.startsWith("-")) display = display.drop(1)
                else display = "-" + display
            }
            "%" -> { try { display = (display.toDouble() / 100).toString() } catch (e: Exception) {} }
            "MC" -> memory = 0.0
            "MR" -> { display = memory.toString(); newInput = false }
            "M+" -> { try { memory += display.toDouble() } catch (e: Exception) {} }
            "M-" -> { try { memory -= display.toDouble() } catch (e: Exception) {} }
            "+", "-", "*", "/" -> {
                if (lastResult.isNotEmpty()) {
                    expression = lastResult + " " + text + " "
                    lastResult = ""
                    newInput = true
                }
                else {
                    expression = display + " " + text + " "
                    newInput = true
                }
            }
            else -> {
                if (newInput) { display = text; newInput = false }
                else { display += text }
            }
        }
    }

    fun evaluate(expr: String): String {
        val sanitized = expr.replace("x", "*").replace("\u00f7", "/")
        val tokens = sanitized.split(" ")
        if (tokens.size < 3) return tokens.last()
        val a = tokens[0].toDouble()
        val op = tokens[1]
        val b = tokens[2].toDouble()
        val result = when (op) {
            "+" -> a + b
            "-" -> a - b
            "*" -> a * b
            "/" -> if (b != 0.0) a / b else throw ArithmeticException()
            else -> a
        }
        return if (result == result.toLong().toDouble()) result.toLong().toString() else result.toString()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Calculator") },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    titleContentColor = Color.White
                )
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier.fillMaxSize().padding(padding).padding(8.dp),
            verticalArrangement = Arrangement.Bottom
        ) {
            // Display
            Card(modifier = Modifier.fillMaxWidth().padding(8.dp)) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(expression, fontSize = 16.sp, color = Color.Gray,
                        modifier = Modifier.fillMaxWidth(), textAlign = TextAlign.End)
                    Text(display, fontSize = 36.sp, fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.fillMaxWidth(), textAlign = TextAlign.End)
                }
            }

            Spacer(Modifier.height(8.dp))

            // Buttons
            val btnStyle = "font-size:18sp; font-weight:bold; min-height:56dp"
            val buttons = listOf(
                listOf("MC","MR","M+","M-"),
                listOf("C","+/-","%","/"),
                listOf("7","8","9","*"),
                listOf("4","5","6","-"),
                listOf("1","2","3","+"),
                listOf("0",".","=")
            )

            buttons.forEach { row ->
                Row(modifier = Modifier.fillMaxWidth().height(56.dp), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    row.forEach { btn ->
                        val isOperator = btn in listOf("+","-","*","/","=")
                        val isMemory = btn in listOf("MC","MR","M+","M-")
                        val isClear = btn == "C"
                        Button(
                            onClick = { onButtonClick(btn) },
                            modifier = Modifier.weight(if (btn == "0") 2f else 1f).fillMaxHeight(),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = when {
                                    isClear -> MaterialTheme.colorScheme.error
                                    isOperator -> Color(0xFFFF9800)
                                    isMemory -> Color(0xFF9C27B0)
                                    else -> Color(0xFFF5F5F5)
                                },
                                contentColor = when {
                                    isOperator || isClear || isMemory -> Color.White
                                    else -> Color.Black
                                }
                            ),
                            shape = ButtonDefaults.shape
                        ) {
                            Text(btn, fontSize = if (btn == "=") 22.sp else 18.sp,
                                fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }
}
