package br.com.aditum.paytrial

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.material3.Text
import br.com.aditum.paytrial.designsystem.PtScaffold
import br.com.aditum.paytrial.designsystem.PtTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent { PtTheme { PtScaffold("Aditum Pay Trial") { Text("ok") } } }
    }
}
