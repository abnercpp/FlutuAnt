package com.fatecsp.abnerferreira.flutuant

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.fatecsp.abnerferreira.flutuant.ui.theme.FlutuAntTheme
import java.text.DecimalFormatSymbols

private val LocalDecimalFormatSymbols = compositionLocalOf { DecimalFormatSymbols.getInstance() }

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            FlutuAntTheme {
                val decimalFormatSymbols = DecimalFormatSymbols.getInstance()
                CompositionLocalProvider(LocalDecimalFormatSymbols provides decimalFormatSymbols) {
                    Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                        ConverterApp(modifier = Modifier.padding(innerPadding))
                    }
                }
            }
        }
    }
}

@Composable
fun ConverterApp(modifier: Modifier = Modifier) {
    var input by remember { mutableStateOf(TextFieldValue("")) }
    var result by remember { mutableStateOf<AdHocFloat32?>(AdHocFloat32(0u)) }

    val decimalFormatSymbols = LocalDecimalFormatSymbols.current

    Column(
        modifier = modifier.padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        OutlinedTextField(
            value = input,
            onValueChange = { input = it },
            label = { Text(stringResource(id = R.string.enter_real_number)) },
            modifier = Modifier.fillMaxWidth(),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal)
        )
        Button(
            onClick = { result = AdHocFloat32.fromText(input.text, decimalFormatSymbols) },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(stringResource(id = R.string.convert_button))
        }
        result?.let {
            ResultView(result = it)
        }
        Spacer(modifier = Modifier.weight(1f)) // Push the info box to the bottom
        InfoBox(
            text = stringResource(id = R.string.decimal_separator_info),
            separator = decimalFormatSymbols.decimalSeparator.toString()
        )
    }
}

@Composable
fun ResultView(result: AdHocFloat32) {
    Box(
        modifier = Modifier
            .background(colorResource(id = R.color.background_label))
            .padding(16.dp)
    ) {
        Column(
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            ResultRow(
                title = stringResource(id = R.string.ieee_754_hex),
                value = "0x${result.bits.toString(16).uppercase()}"
            )
            ResultRow(
                title = stringResource(id = R.string.mantissa_binary),
                value = result.mantissa.toString(2).padStart(23, '0')
            )
            ResultRow(
                title = stringResource(id = R.string.exponent_decimal),
                value = result.biasedExponent.toString()
            )
            ResultRow(
                title = stringResource(id = R.string.exponent_binary_bias),
                value = result.biasedExponent.toString(2).padStart(8, '0')
            )
            ResultRow(
                title = stringResource(id = R.string.std_float),
                value = result.stdFloat.toString()
            )
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(40.dp) // Define a fixed height for the NaN warning box
            ) {
                if (result.stdFloat.isNaN()) {
                    Box(
                        modifier = Modifier
                            .background(colorResource(id = R.color.background_nan_warning))
                            .padding(8.dp)
                    ) {
                        Text(
                            text = stringResource(id = R.string.nan_warning),
                            color = MaterialTheme.colorScheme.error,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun ResultRow(title: String, value: String) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically, // Align vertically
        modifier = Modifier.fillMaxWidth()
    ) {
        Text(
            text = title,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.weight(1f)
        )
        Box(
            modifier = Modifier
                .background(colorResource(id = R.color.background_value))
                .padding(8.dp)
        ) {
            Text(text = value)
        }
    }
}

@Composable
fun InfoBox(text: String, separator: String) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 16.dp)
    ) {
        Text(
            text = text,
            fontWeight = FontWeight.Bold
        )
        Box(
            modifier = Modifier
                .background(colorResource(id = R.color.background_separator))
                .padding(8.dp)
        ) {
            Text(text = separator)
        }
    }
}

@Preview(showBackground = true)
@Composable
fun ConverterAppPreview() {
    FlutuAntTheme {
        CompositionLocalProvider(LocalDecimalFormatSymbols provides DecimalFormatSymbols.getInstance()) {
            ConverterApp()
        }
    }
}
