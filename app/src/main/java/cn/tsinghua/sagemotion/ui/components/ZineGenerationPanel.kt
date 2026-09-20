package cn.tsinghua.sagemotion.ui.components

import android.graphics.BitmapFactory
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import cn.tsinghua.sagemotion.BuildConfig
import cn.tsinghua.sagemotion.ui.theme.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

@Composable
fun ZineGenerationPanel(photoUri: String?, modifier: Modifier = Modifier) {
    val binding = LocalJourneyBinding.current ?: return
    val state = binding.state
    val configured = BuildConfig.SAGE_AGENT_BACKEND_URL.let { it.startsWith("https://") || (BuildConfig.DEBUG && it.startsWith("http://")) }
    var approvedPhoto by remember { mutableStateOf<String?>(null) }
    var bitmap by remember { mutableStateOf<android.graphics.Bitmap?>(null) }
    LaunchedEffect(state.generatedZinePath, state.zineBusy) {
        if (!state.zineBusy) bitmap = withContext(Dispatchers.IO) { state.generatedZinePath?.let { BitmapFactory.decodeFile(it) } }
    }
    val displayedBitmap = bitmap
    DisposableEffect(displayedBitmap) { onDispose { displayedBitmap?.recycle() } }
    Column(modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        bitmap?.let {
            Image(it.asImageBitmap(), "已保存的 AI 拾景纸刊", Modifier.fillMaxWidth().aspectRatio(it.width.toFloat() / it.height))
            Text("AI 生成 · 场景细节请对照原照片", color = SageMuted, fontSize = 11.sp)
            OutlinedButton(onClick = binding.onShareZine, modifier = Modifier.fillMaxWidth()) { Text("分享这张纸刊") }
        }
        if (state.zineBusy) LinearProgressIndicator(Modifier.fillMaxWidth())
        Text(state.zineMessage ?: when {
            !configured -> "云端纸刊尚未配置，下方为本机拼贴预览。"
            photoUri == null -> "先拍一张照片，再生成专属纸刊。"
            else -> "使用当前选中的照片生成一张纸刊，完成后可保存和分享。"
        }, color = SageMuted, fontSize = 12.sp)
        Button(onClick = { approvedPhoto = photoUri }, enabled = configured && photoUri != null && !state.zineBusy,
            modifier = Modifier.fillMaxWidth()) {
            Text(if (state.zineBusy) "正在生成…" else if (state.generatedZinePath != null) "用选中照片重新生成" else "生成 AI 拾景纸刊")
        }
    }
    approvedPhoto?.let { selectedPhoto ->
        AlertDialog(
            onDismissRequest = { approvedPhoto = null },
            title = { Text("生成这张照片的纸刊") },
            text = { Text("将选中的一张照片和标题发送到已配置的云端服务进行图像生成，可能使用服务额度。完成后图片保存在本机。") },
            confirmButton = { TextButton(onClick = { approvedPhoto = null; binding.onGenerateZine(selectedPhoto) }) { Text("上传并生成") } },
            dismissButton = { TextButton(onClick = { approvedPhoto = null }) { Text("取消") } },
        )
    }
}
