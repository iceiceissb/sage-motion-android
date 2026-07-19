package cn.tsinghua.sagemotion.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Storage
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import cn.tsinghua.sagemotion.model.ConditionOrder
import cn.tsinghua.sagemotion.model.DemoMode
import cn.tsinghua.sagemotion.ui.theme.SageGreen
import cn.tsinghua.sagemotion.ui.theme.SageInk
import cn.tsinghua.sagemotion.ui.theme.SageMist
import cn.tsinghua.sagemotion.ui.theme.SageSurface

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ResearcherSetupScreen(
    onStart: (String, ConditionOrder, DemoMode) -> Unit,
    onHistory: () -> Unit,
    savedSessionCount: Int,
    statusMessage: String?,
) {
    var participantId by rememberSaveable { mutableStateOf("") }
    var selectedOrder by rememberSaveable { mutableStateOf(ConditionOrder.ABC) }
    var selectedMode by rememberSaveable { mutableStateOf(DemoMode.EXPERIMENT_OFFLINE) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(SageSurface)
            .safeDrawingPadding()
            .padding(horizontal = 24.dp, vertical = 20.dp),
    ) {
        Column(
            modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.Center,
        ) {
            Box(
                modifier = Modifier
                    .size(58.dp)
                    .background(SageMist, CircleShape),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = Icons.Default.CheckCircle,
                    contentDescription = null,
                    tint = SageGreen,
                    modifier = Modifier.size(30.dp),
                )
            }
            Spacer(Modifier.height(20.dp))
            Text(
                text = "SAGE 动效实验",
                color = SageInk,
                fontSize = 28.sp,
                fontWeight = FontWeight.SemiBold,
            )
            Text(
                text = "研究员设置 · 参与者不会看到条件名称",
                color = Color(0xFF6C7671),
                fontSize = 14.sp,
                modifier = Modifier.padding(top = 6.dp, bottom = 28.dp),
            )

            Card(
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
            ) {
                Column(modifier = Modifier.padding(20.dp)) {
                    Text("参与者编号", fontWeight = FontWeight.Medium, color = SageInk)
                    OutlinedTextField(
                        value = participantId,
                        onValueChange = { participantId = it.take(24) },
                        placeholder = { Text("例如 P001") },
                        singleLine = true,
                        supportingText = {
                            Text(if (participantId.isBlank()) "必填 · 请输入匿名参与者编号" else "仅记录匿名编号，不保存姓名或联系方式")
                        },
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 10.dp),
                    )
                    Spacer(Modifier.height(22.dp))
                    Text("条件顺序（Latin square）", fontWeight = FontWeight.Medium, color = SageInk)
                    FlowRow(
                        modifier = Modifier.padding(top = 8.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        ConditionOrder.entries.forEach { order ->
                            FilterChip(
                                selected = selectedOrder == order,
                                onClick = { selectedOrder = order },
                                label = { Text(order.label) },
                            )
                        }
                    }
                    Text(
                        text = "A = Baseline　B = Semantic Motion　C = SAGE Full",
                        fontSize = 12.sp,
                        color = Color(0xFF7A837F),
                        modifier = Modifier.padding(top = 10.dp),
                    )
                    Spacer(Modifier.height(20.dp))
                    Text("运行模式", fontWeight = FontWeight.Medium, color = SageInk)
                    Column(
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.padding(top = 8.dp),
                    ) {
                        DemoMode.entries.forEach { mode ->
                            FilterChip(
                                selected = selectedMode == mode,
                                onClick = { selectedMode = mode },
                                label = {
                                    Column(Modifier.padding(vertical = 3.dp)) {
                                        Text(mode.label, fontWeight = FontWeight.Medium)
                                        Text(mode.description, fontSize = 11.sp, color = Color(0xFF6C7671))
                                    }
                                },
                                modifier = Modifier.fillMaxWidth(),
                            )
                        }
                    }
                    if (selectedMode == DemoMode.ONLINE_AGENT) {
                        Text(
                            "使用清华校园固定演示点，不读取参与者定位；接口失败自动回退。联网结果不得混入正式实验数据分析。",
                            fontSize = 11.sp,
                            color = Color(0xFF805024),
                            modifier = Modifier.padding(top = 8.dp),
                        )
                    }
                }
            }

            Button(
                onClick = { onStart(participantId, selectedOrder, selectedMode) },
                enabled = participantId.isNotBlank(),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 16.dp)
                    .height(56.dp),
                shape = RoundedCornerShape(18.dp),
            ) {
                Text(
                    if (selectedMode == DemoMode.ONLINE_AGENT) "启动联网 Agent 演示" else "开始实验会话",
                    fontSize = 17.sp,
                )
            }
            OutlinedButton(
                onClick = onHistory,
                modifier = Modifier.fillMaxWidth().padding(top = 10.dp).height(52.dp),
                shape = RoundedCornerShape(17.dp),
            ) {
                Icon(Icons.Default.History, null, modifier = Modifier.size(19.dp))
                Text("查看历史数据${if (savedSessionCount > 0) "（$savedSessionCount）" else ""}", modifier = Modifier.padding(start = 8.dp))
            }
            if (statusMessage != null) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth().padding(top = 12.dp),
                ) {
                    Icon(Icons.Default.Storage, null, tint = SageGreen, modifier = Modifier.size(16.dp))
                    Text(statusMessage, color = SageGreen, fontSize = 12.sp, modifier = Modifier.padding(start = 7.dp))
                }
            }
            Text(
                text = "主界面长按顶部状态条 1 秒，可打开研究员控制台。",
                fontSize = 12.sp,
                color = Color(0xFF7A837F),
                textAlign = TextAlign.Center,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 14.dp),
            )
        }
    }
}
