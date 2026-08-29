package cn.tsinghua.sagemotion.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import cn.tsinghua.sagemotion.model.SurveyDimension
import cn.tsinghua.sagemotion.model.TaskPerformance
import cn.tsinghua.sagemotion.ui.theme.SageDivider
import cn.tsinghua.sagemotion.ui.theme.SageGreen
import cn.tsinghua.sagemotion.ui.theme.SageGreenDark
import cn.tsinghua.sagemotion.ui.theme.SageInk
import cn.tsinghua.sagemotion.ui.theme.SageMist
import cn.tsinghua.sagemotion.ui.theme.SageMuted
import cn.tsinghua.sagemotion.ui.theme.SageOnSignal
import cn.tsinghua.sagemotion.ui.theme.SagePanelRaised
import cn.tsinghua.sagemotion.ui.theme.SageSignalLime
import cn.tsinghua.sagemotion.ui.theme.SageSurface

/** 条件无关的统一任务后问卷；允许在提交前返回修改已有答案。 */
@Composable
fun PostTaskSurveyScreen(
    performance: TaskPerformance,
    onSubmit: (Map<SurveyDimension, Int>) -> Unit,
) {
    val dimensions = SurveyDimension.entries
    val answers = remember(performance.taskInstance, performance.scenario) {
        mutableStateMapOf<SurveyDimension, Int>()
    }
    var currentIndex by remember(performance.taskInstance, performance.scenario) { mutableIntStateOf(0) }
    BackHandler(enabled = true) {
        if (currentIndex > 0) currentIndex--
    }
    val current = dimensions[currentIndex]
    val animatedProgress by animateFloatAsState(
        targetValue = (currentIndex + 1f) / dimensions.size,
        animationSpec = tween(420),
        label = "surveyProgress",
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(SageSurface)
            .safeDrawingPadding()
            .padding(horizontal = 22.dp, vertical = 18.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Surface(color = SageMist, shape = RoundedCornerShape(100.dp)) {
                Text(
                    "任务 ${performance.scenario.id} 已完成",
                    color = SageSignalLime,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 7.dp),
                )
            }
            Spacer(Modifier.weight(1f))
            Text("${currentIndex + 1} / ${dimensions.size}", color = SageMuted, fontSize = 12.sp)
        }
        Text(
            "刚才的体验如何？",
            color = SageInk,
            fontSize = 28.sp,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.padding(top = 22.dp),
        )
        Text(
            "请根据刚才这个任务作答。所有条件的题目与流程完全相同。",
            color = SageMuted,
            fontSize = 14.sp,
            lineHeight = 21.sp,
            modifier = Modifier.padding(top = 7.dp),
        )
        LinearProgressIndicator(
            progress = { animatedProgress },
            color = SageSignalLime,
            trackColor = SageDivider,
            modifier = Modifier.fillMaxWidth().padding(top = 20.dp).height(5.dp),
        )

        AnimatedContent(
            targetState = currentIndex,
            transitionSpec = {
                (slideInHorizontally(tween(340)) { it / 4 } + fadeIn(tween(280))) togetherWith
                    (slideOutHorizontally(tween(260)) { -it / 5 } + fadeOut(tween(220)))
            },
            label = "surveyQuestion",
            modifier = Modifier.weight(1f),
        ) { index ->
            val item = dimensions[index]
            Column(
                verticalArrangement = Arrangement.Center,
                modifier = Modifier.fillMaxSize(),
            ) {
                Text(
                    item.shortLabel,
                    color = SageSignalLime,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold,
                    letterSpacing = 1.sp,
                )
                Text(
                    item.statement,
                    color = SageInk,
                    fontSize = 23.sp,
                    lineHeight = 33.sp,
                    fontWeight = FontWeight.Medium,
                    modifier = Modifier.padding(top = 13.dp),
                )
                Row(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    modifier = Modifier.fillMaxWidth().padding(top = 32.dp),
                ) {
                    (1..7).forEach { value ->
                        val selected = answers[item] == value
                        Surface(
                            color = if (selected) SageSignalLime else SagePanelRaised,
                            contentColor = if (selected) SageOnSignal else SageInk,
                            shape = CircleShape,
                            shadowElevation = if (selected) 5.dp else 0.dp,
                            border = if (selected) null else androidx.compose.foundation.BorderStroke(1.dp, SageDivider),
                            modifier = Modifier
                                .weight(1f)
                                .height(43.dp)
                                .clickable { answers[item] = value },
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Text(value.toString(), fontWeight = FontWeight.SemiBold)
                            }
                        }
                    }
                }
                Row(Modifier.fillMaxWidth().padding(top = 10.dp)) {
                    Text(item.lowAnchor, color = SageMuted, fontSize = 11.sp)
                    Spacer(Modifier.weight(1f))
                    Text(item.highAnchor, color = SageMuted, fontSize = 11.sp, textAlign = TextAlign.End)
                }
            }
        }

        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            OutlinedButton(
                enabled = currentIndex > 0,
                onClick = { currentIndex-- },
                shape = RoundedCornerShape(18.dp),
                modifier = Modifier.weight(.38f).height(56.dp),
            ) {
                Text("上一题", fontSize = 15.sp)
            }
            Button(
                enabled = answers[current] != null,
                onClick = {
                    if (currentIndex < dimensions.lastIndex) currentIndex++ else onSubmit(answers.toMap())
                },
                shape = RoundedCornerShape(18.dp),
                modifier = Modifier.weight(.62f).height(56.dp),
            ) {
                if (currentIndex == dimensions.lastIndex) {
                    Icon(Icons.Default.Check, contentDescription = null)
                    Text("提交并继续", fontSize = 15.sp, color = SageOnSignal, fontWeight = FontWeight.Bold, modifier = Modifier.padding(start = 6.dp))
                } else {
                    Text("下一题", fontSize = 15.sp, color = SageOnSignal, fontWeight = FontWeight.Bold)
                }
            }
        }
        Text(
            "选择后才能继续 · 评分会随本次会话保存",
            color = SageMuted,
            fontSize = 11.sp,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth().padding(top = 10.dp),
        )
    }
}
