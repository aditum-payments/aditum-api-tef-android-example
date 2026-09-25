package br.com.aditum.paytrial

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import br.com.aditum.paytrial.app.MainViewModel
import br.com.aditum.paytrial.designsystem.ProvideWidthClass
import br.com.aditum.paytrial.designsystem.PtTheme
import br.com.aditum.paytrial.ui.PayTrialRoot

/** Uma activity só; o estado vive no [MainViewModel] e sobrevive à rotação durante a venda. */
class MainActivity : ComponentActivity() {
    private val vm: MainViewModel by viewModels()
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // a faixa de largura (celular × tablet) é medida aqui e vale para todas as telas
        setContent { PtTheme(brand = vm.brand) { ProvideWidthClass { PayTrialRoot(vm) } } }
    }
}
