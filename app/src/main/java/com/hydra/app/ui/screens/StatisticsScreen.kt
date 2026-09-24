package com.hydra.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.Psychology
import androidx.compose.material.icons.filled.WaterDrop
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.hydra.app.data.local.EventEntity
import com.hydra.app.data.model.EventType
import com.hydra.app.ui.viewmodel.HydraUiState
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

@Composable
fun StatisticsScreen(
    uiState: HydraUiState
) {
    // Calculate last 7 days water consumption
    val last7DaysData = remember(uiState.allEvents) {
        calculateLast7DaysWater(uiState.allEvents)
    }

    val maxWaterDay = remember(last7DaysData) {
        last7DaysData.maxOfOrNull { it.waterMl }?.coerceAtLeast(1000) ?: 2500
    }

    // Prediction Performance metrics
    val totalPredictions = uiState.predictionSamples.size
    val completedFeedback = uiState.predictionSamples.filter { it.feedbackGiven && it.actualDelayMinutes != null }
    val avgActualDelay = if (completedFeedback.isNotEmpty()) {
        completedFeedback.mapNotNull { it.actualDelayMinutes }.average().toInt()
    } else 0

    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.TopCenter
    ) {
        LazyColumn(
            modifier = Modifier
                .fillMaxWidth()
                .widthIn(max = 600.dp)
                .padding(horizontal = 20.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                Text(
                    text = "الإحصائيات والتحليلات",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "متابعة تطور العادات واستهلاك الماء والتذكيرات",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            // 1. Water Intake Chart (7 days)
            item {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("chart_water_7days"),
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.WaterDrop,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary
                            )
                            Text(
                                text = "استهلاك الماء (آخر 7 أيام)",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        // Bar Chart Row
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(140.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.Bottom
                        ) {
                            last7DaysData.forEach { dayData ->
                                Column(
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    modifier = Modifier.weight(1f)
                                ) {
                                    val heightFraction = (dayData.waterMl.toFloat() / maxWaterDay).coerceIn(0.05f, 1f)
                                    Box(
                                        modifier = Modifier
                                            .fillMaxHeight(heightFraction)
                                            .width(22.dp)
                                            .clip(RoundedCornerShape(topStart = 6.dp, topEnd = 6.dp))
                                            .background(
                                                if (dayData.isToday)
                                                    MaterialTheme.colorScheme.primary
                                                else
                                                    MaterialTheme.colorScheme.primary.copy(alpha = 0.5f)
                                            )
                                    )
                                    Spacer(modifier = Modifier.height(6.dp))
                                    Text(
                                        text = dayData.dayLabel,
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = if (dayData.isToday) FontWeight.Bold else FontWeight.Normal,
                                        color = if (dayData.isToday) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // 2. Behavioral Reminder Prediction Performance Card
            item {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("card_prediction_stats"),
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Psychology,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.secondary
                            )
                            Text(
                                text = "دقة التذكيرات السلوكية التقديرية",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                        }

                        Spacer(modifier = Modifier.height(14.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceEvenly
                        ) {
                            StatMiniItem(title = "إجمالي التنبيهات", value = "$totalPredictions")
                            StatMiniItem(title = "تأكيدات الاستجابة", value = "${completedFeedback.size}")
                            StatMiniItem(
                                title = "متوسط الفارق الفعلي",
                                value = if (avgActualDelay > 0) "$avgActualDelay د" else "—"
                            )
                        }
                    }
                }
            }

            // 3. Medical Disclaimer Card (Mandatory Rule)
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f)
                    )
                ) {
                    Row(
                        modifier = Modifier.padding(14.dp),
                        verticalAlignment = Alignment.Top,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Info,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.outline,
                            modifier = Modifier.size(20.dp)
                        )
                        Text(
                            text = "تنويه مهم: تطبيق Hydra لا يقدم أي تشخيص أو تقييم طبي. جميع التنبؤات والتذكيرات هي تقديرات سلوكية بحتة مستندة إلى توقيتات إدخال البيانات الشخصية لمساعدتك في المتابعة اليومية.",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            item {
                Spacer(modifier = Modifier.height(24.dp))
            }
        }
    }
}

@Composable
fun StatMiniItem(title: String, value: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = value,
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary
        )
        Text(
            text = title,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )
    }
}

data class DayWaterData(
    val dayLabel: String,
    val waterMl: Int,
    val isToday: Boolean
)

private fun calculateLast7DaysWater(allEvents: List<EventEntity>): List<DayWaterData> {
    val result = mutableListOf<DayWaterData>()
    val cal = Calendar.getInstance()
    val dayFormat = SimpleDateFormat("EEE", Locale("ar"))

    for (i in 6 downTo 0) {
        val targetCal = Calendar.getInstance().apply {
            add(Calendar.DAY_OF_YEAR, -i)
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        val startOfDay = targetCal.timeInMillis
        val endOfDay = startOfDay + (24 * 60 * 60 * 1000L) - 1

        val dayEvents = allEvents.filter {
            it.type == EventType.WATER.name && it.createdAt in startOfDay..endOfDay
        }
        val totalWater = dayEvents.sumOf { it.amount?.toInt() ?: 0 }

        val label = if (i == 0) "اليوم" else dayFormat.format(Date(startOfDay))
        result.add(DayWaterData(dayLabel = label, waterMl = totalWater, isToday = i == 0))
    }

    return result
}
