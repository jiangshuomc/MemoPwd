package com.example.memopwd

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import com.example.memopwd.security.BiometricAuthManager
import com.example.memopwd.security.CryptoManager
import com.example.memopwd.ui.common.MainTabScreen
import com.example.memopwd.ui.main.MainLandingScreen
import com.example.memopwd.ui.splash.SplashScreen
import com.example.memopwd.ui.theme.MemoPwdTheme

class MainActivity : ComponentActivity() {

    private lateinit var cryptoManager: CryptoManager
    private lateinit var biometricAuthManager: BiometricAuthManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        cryptoManager = CryptoManager(this)
        biometricAuthManager = BiometricAuthManager(this)

        setContent {
            MemoPwdTheme {
                // 0=启动页, 1=引导页, 2=主界面
                var stage by remember { mutableIntStateOf(0) }

                when (stage) {
                    0 -> SplashScreen(
                        modifier = Modifier.fillMaxSize(),
                        onSplashFinished = { stage = 1 }
                    )

                    1 -> MainLandingScreen(
                        modifier = Modifier.fillMaxSize(),
                        onEnterApp = { stage = 2 }
                    )

                    2 -> MainTabScreen(
                        modifier = Modifier.fillMaxSize(),
                        biometricAuthManager = biometricAuthManager,
                        cryptoManager = cryptoManager,
                        onLockLost = { /* 密钥被系统重置：保持在当前页，下次访问会重新校验 */ }
                    )
                }
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        // 退出时清掉内存里的密钥引用，不留残留
        biometricAuthManager.invalidate()
    }
}
