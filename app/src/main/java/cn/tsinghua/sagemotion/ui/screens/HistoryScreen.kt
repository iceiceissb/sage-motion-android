package cn.tsinghua.sagemotion.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.CloudDone
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Storage
import androidx.compose.material3.Button
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import cn.tsinghua.sagemotion.model.HistoryEvent
import cn.tsinghua.sagemotion.model.SessionDetail
import cn.tsinghua.sagemotion.model.SessionSummary
import cn.tsinghua.sagemotion.ui.theme.SageDivider
import cn.tsinghua.sagemotion.ui.theme.SageGreen
import cn.tsinghua.sagemotion.ui.theme.SageGreenDark
import cn.tsinghua.sagemotion.ui.theme.SageInk
import cn.tsinghua.sagemotion.ui.theme.SageMist
import cn.tsinghua.sagemotion.ui.theme.SageMuted
import cn.tsinghua.sagemotion.ui.theme.SageSurface
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun HistoryScreen(
    sessions: List<SessionSummary>,
    selectedDetail: SessionDetail?,
    onBack: () -> Unit,
    onSelect: (String) -> Unit,
    onCloseDetail: () -> Unit,
    onExportSession: (String) -> Unit,
    onExportAll: () -> Unit,
    onDeleteSession: (String) -> Unit,
    onDeleteAll: () -> Unit,
) {
    var pendingDelete by remember { mutableStateOf<SessionSummary?>(null) }
    var confirmDeleteAll by remember { mutableStateOf(false) }
    BackHandler(onBack = if (selectedDetail != null) onCloseDetail else onBack)
    if (selectedDetail != null) {
        HistoryDetailScreen(
            detail = selectedDetail,
            onBack = onCloseDetail,
            onExport = { onExportSession(selectedDetail.summary.fileName) },
            onDelete = { onDeleteSession(selectedDetail.summary.fileName) },
        )
        return
    }

    Column(
        Modifier.fillMaxSize().background(SageSurface).safeDrawingPadding(),
    ) {
        HistoryTopBar(title = "本地实验数据", onBack = onBack)
        if (sessions.isEmpty()) {
            EmptyHistory(Modifier.weight(1f))
        } else {
            LazyColumn(
                modifier = Modifier.weight(1f),
                contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 18.dp, vertical = 10.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                item {
                    StorageOverview(sessions)
                }
                items(sessions, key = { it.fileName }) { session ->
                    SessionCard(
                        session = session,
                        onClick = { onSelect(session.fileName) },
                        onDelete = { pendingDelete = session },
                    )
                }
                item { Spacer(Modifier.height(8.dp)) }
            }
        }
        Surface(color = Color.White, shadowElevation = 8.dp) {
            Column(Modifier.fillMaxWidth().padding(horizontal = 18.dp, vertical = 14.dp)) {
                Button(
                    onClick = onExportAll,
                    enabled = sessions.isNotEmpty(),
                    modifier = Modifier.fillMaxWidth().height(52.dp),
                    shape = RoundedCornerShape(17.dp),
                ) {
                    Icon(Icons.Default.Download, null, modifier = Modifier.size(19.dp))
                    Spacer(Modifier.width(8.dp))
                    Text("导出全部 CSV（ZIP）")
                }
                TextButton(
                    onClick = { confirmDeleteAll = true },
                    enabled = sessions.isNotEmpty(),
                    modifier = Modifier.fillMaxWidth().padding(top = 3.dp),
                ) {
                    Icon(Icons.Default.DeleteSweep, null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(7.dp))
                    Text("删除全部历史数据", color = Color(0xFFB45A4C))
                }
                Text(
                    "记录仅保存在本机应用私有目录，除非你主动导出。",
                    color = SageMuted,
                    fontSize = 11.sp,
                    modifier = Modifier.padding(top = 8.dp).align(Alignment.CenterHorizontally),
                )
            }
        }
    }
    pendingDelete?.let { session ->
        AlertDialog(
            onDismissRequest = { pendingDelete = null },
            icon = { Icon(Icons.Default.DeleteOutline, null, tint = Color(0xFFB45A4C)) },
            title = { Text("删除 ${session.participantId} 的会话？") },
            text = { Text("将永久删除本次 CSV 和该会话记录到的过程照片，无法恢复。") },
            confirmButton = {
                Button(onClick = { onDeleteSession(session.fileName); pendingDelete = null }) { Text("确认删除") }
            },
            dismissButton = { TextButton(onClick = { pendingDelete = null }) { Text("取消") } },
        )
    }
    if (confirmDeleteAll) {
        AlertDialog(
            onDismissRequest = { confirmDeleteAll = false },
            icon = { Icon(Icons.Default.DeleteSweep, null, tint = Color(0xFFB45A4C)) },
            title = { Text("删除全部历史数据？") },
            text = { Text("将删除全部非活动会话、关联照片和历史导出包。当前进行中的会话会受到保护。") },
            confirmButton = {
                Button(onClick = { onDeleteAll(); confirmDeleteAll = false }) { Text("删除全部") }
            },
            dismissButton = { TextButton(onClick = { confirmDeleteAll = false }) { Text("取消") } },
        )
    }
}

@Composable
private fun HistoryTopBar(title: String, onBack: () -> Unit) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 8.dp),
    ) {
        IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "返回") }
        Text(title, color = SageInk, fontSize = 21.sp, fontWeight = FontWeight.SemiBold)
    }
}

@Composable
private fun StorageOverview(sessions: List<SessionSummary>) {
    val completed = sessions.count { it.completed }
    val tasks = sessions.sumOf { it.completedTaskCount }
    val size = sessions.sumOf { it.sizeBytes }
    Card(
        colors = CardDefaults.cardColors(containerColor = SageGreenDark),
        shape = RoundedCornerShape(24.dp),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(Modifier.padding(18.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(Modifier.size(38.dp).background(Color.White.copy(alpha = .13f), CircleShape), contentAlignment = Alignment.Center) {
                    Icon(Icons.Default.Storage, null, tint = Color.White, modifier = Modifier.size(20.dp))
                }
                Column(Modifier.padding(start = 11.dp)) {
                    Text("本地存储正常", color = Color.White, fontWeight = FontWeight.SemiBold)
                    Text("每次操作都会实时写入 CSV", color = Color.White.copy(alpha = .72f), fontSize = 12.sp)
                }
            }
            Row(Modifier.fillMaxWidth().padding(top = 18.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                OverviewMetric("${sessions.size}", "会话")
                OverviewMetric("$completed", "已完成")
                OverviewMetric("$tasks", "任务")
                OverviewMetric(formatBytes(size), "占用")
            }
        }
    }
}

@Composable
private fun OverviewMetric(value: String, label: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(value, color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.SemiBold)
        Text(label, color = Color.White.copy(alpha = .65f), fontSize = 11.sp)
    }
}

@Composable
private fun SessionCard(session: SessionSummary, onClick: () -> Unit, onDelete: () -> Unit) {
    val formatter = remember { SimpleDateFormat("yyyy-MM-dd  HH:mm", Locale.getDefault()) }
    Card(
        colors = CardDefaults.cardColors(containerColor = Color.White),
        shape = RoundedCornerShape(20.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick),
    ) {
        Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(
                Modifier.size(44.dp).background(if (session.completed) SageMist else Color(0xFFFFF0DE), CircleShape),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    if (session.completed) Icons.Default.CheckCircle else Icons.Default.Schedule,
                    null,
                    tint = if (session.completed) SageGreen else Color(0xFFAA662A),
                    modifier = Modifier.size(23.dp),
                )
            }
            Column(Modifier.padding(start = 12.dp).weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(session.participantId, color = SageInk, fontWeight = FontWeight.SemiBold)
                    Text(
                        if (session.completed) "已完成" else "未结束",
                        color = if (session.completed) SageGreen else Color(0xFFAA662A),
                        fontSize = 10.sp,
                        modifier = Modifier.padding(start = 8.dp).background(
                            if (session.completed) SageMist else Color(0xFFFFF0DE),
                            RoundedCornerShape(7.dp),
                        ).padding(horizontal = 6.dp, vertical = 3.dp),
                    )
                }
                Text(
                    "${modeLabel(session.demoMode)} · ${formatter.format(Date(session.startedAtMillis))} · ${session.completedTaskCount} 个任务 · 顺序 ${session.conditionOrder}",
                    color = SageMuted,
                    fontSize = 12.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.padding(top = 4.dp),
                )
            }
            IconButton(onClick = onDelete) {
                Icon(Icons.Default.DeleteOutline, "删除会话", tint = Color(0xFFB45A4C), modifier = Modifier.size(20.dp))
            }
            Icon(Icons.Default.ChevronRight, null, tint = SageMuted, modifier = Modifier.size(18.dp))
        }
    }
}

private fun modeLabel(mode: String): String = when (mode) {
    "ONLINE_AGENT" -> "联网 Agent"
    "EXPERIMENT_OFFLINE" -> "离线实验"
    else -> "旧版会话"
}

@Composable
private fun EmptyHistory(modifier: Modifier = Modifier) {
    Column(
        modifier.fillMaxWidth().padding(36.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Box(Modifier.size(72.dp).background(SageMist, CircleShape), contentAlignment = Alignment.Center) {
            Icon(Icons.Default.History, null, tint = SageGreen, modifier = Modifier.size(34.dp))
        }
        Text("还没有实验记录", color = SageInk, fontSize = 20.sp, fontWeight = FontWeight.SemiBold, modifier = Modifier.padding(top = 18.dp))
        Text("开始一次会话后，操作记录会自动出现在这里。", color = SageMuted, fontSize = 13.sp, modifier = Modifier.padding(top = 6.dp))
    }
}

@Composable
private fun HistoryDetailScreen(detail: SessionDetail, onBack: () -> Unit, onExport: () -> Unit, onDelete: () -> Unit) {
    var confirmDelete by remember { mutableStateOf(false) }
    Column(Modifier.fillMaxSize().background(SageSurface).safeDrawingPadding()) {
        HistoryTopBar(title = detail.summary.participantId, onBack = onBack)
        LazyColumn(
            modifier = Modifier.weight(1f),
            contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 18.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            item { DetailSummary(detail.summary) }
            item {
                Text("事件时间线 · ${detail.events.size} 条", color = SageInk, fontWeight = FontWeight.SemiBold, modifier = Modifier.padding(top = 8.dp, bottom = 2.dp))
            }
            items(detail.events.asReversed(), key = { "${it.timestampMillis}-${it.event}-${it.elapsedMillis}" }) { event ->
                EventRow(event)
            }
            item { Spacer(Modifier.height(10.dp)) }
        }
        Surface(color = Color.White, shadowElevation = 8.dp) {
            Row(Modifier.fillMaxWidth().padding(18.dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedButton(onClick = { confirmDelete = true }, modifier = Modifier.weight(1f).height(50.dp), shape = RoundedCornerShape(17.dp)) {
                    Icon(Icons.Default.DeleteOutline, null, tint = Color(0xFFB45A4C), modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(6.dp)); Text("删除", color = Color(0xFFB45A4C))
                }
                OutlinedButton(onClick = onExport, modifier = Modifier.weight(2f).height(50.dp), shape = RoundedCornerShape(17.dp)) {
                    Icon(Icons.Default.Download, null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(8.dp)); Text("导出本次 CSV")
                }
            }
        }
    }
    if (confirmDelete) {
        AlertDialog(
            onDismissRequest = { confirmDelete = false },
            title = { Text("删除本次会话？") },
            text = { Text("CSV、事件记录和关联过程照片都会永久删除。") },
            confirmButton = { Button(onClick = { confirmDelete = false; onDelete() }) { Text("确认删除") } },
            dismissButton = { TextButton(onClick = { confirmDelete = false }) { Text("取消") } },
        )
    }
}

@Composable
private fun DetailSummary(summary: SessionSummary) {
    val formatter = remember { SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()) }
    Card(colors = CardDefaults.cardColors(containerColor = Color.White), shape = RoundedCornerShape(20.dp)) {
        Column(Modifier.padding(17.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.CloudDone, null, tint = SageGreen)
                Text("记录文件完整可用", color = SageGreenDark, fontWeight = FontWeight.Medium, modifier = Modifier.padding(start = 9.dp))
            }
            HorizontalDivider(Modifier.padding(vertical = 13.dp), color = SageDivider)
            DetailLine("开始时间", formatter.format(Date(summary.startedAtMillis)))
            DetailLine("任务结果", "${summary.completedTaskCount} 次 · 采纳 ${summary.adoptedCount} 次")
            DetailLine("事件数量", "${summary.eventCount} 条")
            DetailLine("文件大小", formatBytes(summary.sizeBytes))
        }
    }
}

@Composable
private fun DetailLine(label: String, value: String) {
    Row(Modifier.fillMaxWidth().padding(vertical = 3.dp)) {
        Text(label, color = SageMuted, fontSize = 12.sp, modifier = Modifier.width(72.dp))
        Text(value, color = SageInk, fontSize = 12.sp)
    }
}

@Composable
private fun EventRow(event: HistoryEvent) {
    val time = remember(event.timestampMillis) { SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(Date(event.timestampMillis)) }
    Row(Modifier.fillMaxWidth()) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.width(48.dp)) {
            Box(Modifier.size(9.dp).background(eventColor(event.event), CircleShape))
            Box(Modifier.width(1.dp).height(52.dp).background(SageDivider))
        }
        Card(
            colors = CardDefaults.cardColors(containerColor = Color.White),
            shape = RoundedCornerShape(15.dp),
            modifier = Modifier.weight(1f),
        ) {
            Column(Modifier.padding(horizontal = 13.dp, vertical = 10.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(eventLabel(event.event), color = SageInk, fontSize = 13.sp, fontWeight = FontWeight.Medium, modifier = Modifier.weight(1f))
                    Text(time, color = SageMuted, fontSize = 10.sp)
                }
                Text(
                    listOf(event.taskId, event.aiState, event.action).filter { it.isNotBlank() }.joinToString(" · "),
                    color = SageMuted,
                    fontSize = 11.sp,
                    modifier = Modifier.padding(top = 3.dp),
                )
            }
        }
    }
}

private fun eventLabel(event: String): String = when (event) {
    "session_started" -> "会话开始"
    "session_restored" -> "恢复会话"
    "task_started" -> "任务开始"
    "state_enter" -> "AI 状态变化"
    "state_exit" -> "AI 状态结束"
    "stage_previewed" -> "研究员预览状态"
    "task_result_visible" -> "结果出现"
    "result_adopted" -> "采纳结果"
    "route_selected" -> "切换路线"
    "evidence_opened" -> "查看依据"
    "task_cancelled" -> "取消任务"
    "task_reset" -> "重置任务"
    "demo_completed" -> "五阶段体验完成"
    "session_completed" -> "会话保存完成"
    "task_failed" -> "任务异常"
    else -> event.replace('_', ' ')
}

private fun eventColor(event: String): Color = when (event) {
    "task_failed", "task_cancelled" -> Color(0xFFB45A4C)
    "result_adopted", "demo_completed", "session_completed" -> SageGreen
    "evidence_opened", "route_selected" -> Color(0xFFB86B2C)
    else -> Color(0xFF7A9187)
}

private fun formatBytes(bytes: Long): String = when {
    bytes < 1024 -> "$bytes B"
    bytes < 1024 * 1024 -> "${bytes / 1024} KB"
    else -> String.format(Locale.getDefault(), "%.1f MB", bytes / 1024f / 1024f)
}
