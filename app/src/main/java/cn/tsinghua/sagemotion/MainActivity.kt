package cn.tsinghua.sagemotion

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import cn.tsinghua.sagemotion.ui.SageMotionApp
import cn.tsinghua.sagemotion.ui.theme.SageMotionTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            SageMotionTheme {
                SageMotionApp()
            }
        }
    }
}

