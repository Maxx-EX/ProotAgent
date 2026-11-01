package com.prootagent.app.ui
import android.annotation.SuppressLint
import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.prootagent.app.ui.theme.*
import kotlinx.coroutines.delay

@SuppressLint("CustomSplashScreen")
class SplashActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            val alpha = remember { Animatable(0f) }
            val offsetY = remember { Animatable(50f) }
            LaunchedEffect(Unit) {
                alpha.animateTo(1f, animationSpec = tween(800))
                offsetY.animateTo(0f, animationSpec = tween(800))
                delay(800)
                startActivity(Intent(this@SplashActivity, MainActivity::class.java))
                finish()
            }
            Box(Modifier.fillMaxSize().background(Brush.verticalGradient(listOf(LiquidBgStart, LiquidBgEnd))), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.graphicsLayer { this.alpha = alpha.value; translationY = offsetY.value }) {
                    Text("ProotAgent", color = TextPrimary, fontSize = 40.sp, fontWeight = FontWeight.Bold)
                    Spacer(Modifier.height(16.dp))
                    Text("MAXX-EX 开发", color = AccentBlue, fontSize = 16.sp, fontWeight = FontWeight.Medium, letterSpacing = 2.sp)
                }
            }
        }
    }
}
