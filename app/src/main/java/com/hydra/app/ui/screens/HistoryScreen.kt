package com.hydra.app.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.FitnessCenter
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Psychology
import androidx.compose.material.icons.filled.Restaurant
import androidx.compose.material.icons.filled.TouchApp
import androidx.compose.material.icons.filled.WaterDrop
import androidx.compose.material.icons.filled.Wc
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.hydra.app.data.local.EventEntity
import com.hydra.app.data.model.EventType
import com.hydra.app.ui.viewmodel.HydraUiState
import com.hydra.app.ui.viewmodel.HydraViewModel
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun HistoryScreen(
    viewModel: HydraViewModel,
    uiState: HydraUiState
) {
    var selectedFilter by remember { mutableStateOf("ALL") }
    var eventToDelete by remember { mutableStateOf<EventEntity?>(null) }

    val filteredEvents = remember(uiState.allEvents, selectedFilter) {
        if (selectedFilter == "ALL") {
            uiState.allEvents
        } else {
            uiState.allEvents.filter { it.type == selectedFilter }
        }
    }

    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.TopCenter
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .widthIn(max = 600.dp)
                .padding(horizontal = 20.dp, vertical = 12.dp)
        ) {
            Text(
                text = "سجل النشاطات والأحداث",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = "جميع الأحداث المسجلة محلياً في جهازك",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(12.dp))

            // Filter Chips Row
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                item {
                    FilterChip(
                        selected = selectedFilter == "ALL",
                        onClick = { selectedFilter = "ALL" },
                        label = { Text("الكل") },
                        modifier = Modifier.testTag("filter_all")
                    )
                }
                item {
                    FilterChip(
                        selected = selectedFilter == EventType.WATER.name,
                        onClick = { selectedFilter = EventType.WATER.name },
                        label = { Text("مياه") },
                        modifier = Modifier.testTag("filter_water")
                    )
                }
                item {
                    FilterChip(
                        selected = selectedFilter == EventType.FOOD.name,
                        onClick = { selectedFilter = EventType.FOOD.name },
                        label = { Text("طعام") },
                        modifier = Modifier.testTag("filter_food")
                    )
                }
                item {
                    FilterChip(
                        selected = selectedFilter == EventType.CREATINE.name,
                        onClick = { selectedFilter = EventType.CREATINE.name },
                        label = { Text("كرياتين") },
                        modifier = Modifier.testTag("filter_creatine")
                    )
                }
                item {
                    FilterChip(
                        selected = selectedFilter == EventType.BATHROOM.name,
                        onClick = { selectedFilter = EventType.BATHROOM.name },
                        label = { Text("حمام") },
                        modifier = Modifier.testTag("filter_bathroom")
                    )
                }
                item {
                    FilterChip(
                        selected = selectedFilter == EventType.NOTE.name,
                        onClick = { selectedFilter = EventType.NOTE.name },
                        label = { Text("ملاحظات") },
                        modifier = Modifier.testTag("filter_note")
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            if (filteredEvents.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            imageVector = Icons.Default.FilterList,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.outline,
                            modifier = Modifier.size(48.dp)
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "لا توجد أحداث مسجلة لهذا التصنيف",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            } else {
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    items(filteredEvents, key = { it.id }) { event ->
                        EventHistoryItem(
                            event = event,
                            onDelete = { eventToDelete = event }
                        )
                    }
                }
            }
        }
    }

    // Delete Confirmation Dialog
    eventToDelete?.let { event ->
        AlertDialog(
            onDismissRequest = { eventToDelete = null },
            title = { Text("حذف هذا الحدث؟") },
            text = { Text("هل تريد بالتأكيد حذف هذا الحدث من السجل؟ لن يمكن استرجاعه.") },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.deleteEvent(event)
                        eventToDelete = null
                    }
                ) {
                    Text("حذف", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { eventToDelete = null }) {
                    Text("إلغاء")
                }
            }
        )
    }
}

@Composable
fun EventHistoryItem(
    event: EventEntity,
    onDelete: () -> Unit
) {
    val timeFormatter = remember { SimpleDateFormat("hh:mm a", Locale.getDefault()) }
    val dateFormatter = remember { SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()) }
    val eventDate = remember(event.createdAt) { Date(event.createdAt) }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("event_item_${event.id}"),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainer
        )
    ) {
        Row(
            modifier = Modifier.padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Icon Badge
            Surface(
                shape = CircleShape,
                color = when (event.type) {
                    EventType.WATER.name -> MaterialTheme.colorScheme.primaryContainer
                    EventType.FOOD.name -> MaterialTheme.colorScheme.tertiaryContainer
                    EventType.CREATINE.name -> MaterialTheme.colorScheme.secondaryContainer
                    EventType.BATHROOM.name -> MaterialTheme.colorScheme.surfaceContainerHigh
                    else -> MaterialTheme.colorScheme.surfaceVariant
                },
                modifier = Modifier.size(42.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = when (event.type) {
                            EventType.WATER.name -> Icons.Default.WaterDrop
                            EventType.FOOD.name -> Icons.Default.Restaurant
                            EventType.CREATINE.name -> Icons.Default.FitnessCenter
                            EventType.BATHROOM.name -> Icons.Default.Wc
                            else -> Icons.Default.Description
                        },
                        contentDescription = null,
                        tint = when (event.type) {
                            EventType.WATER.name -> MaterialTheme.colorScheme.primary
                            EventType.FOOD.name -> MaterialTheme.colorScheme.tertiary
                            EventType.CREATINE.name -> MaterialTheme.colorScheme.secondary
                            EventType.BATHROOM.name -> MaterialTheme.colorScheme.primary
                            else -> MaterialTheme.colorScheme.onSurfaceVariant
                        },
                        modifier = Modifier.size(22.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.width(12.dp))

            // Info Column
            Column(modifier = Modifier.weight(1f)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(
                        text = when (event.type) {
                            EventType.WATER.name -> "${event.amount?.toInt() ?: 0} مل ماء"
                            EventType.FOOD.name -> event.description ?: "وجبة"
                            EventType.CREATINE.name -> "كرياتين (${event.amount?.toInt() ?: 5} جم)"
                            EventType.BATHROOM.name -> event.description ?: "دخول الحمام"
                            else -> event.description ?: "ملاحظة"
                        },
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold
                    )

                    // Source Badge
                    SourceBadge(source = event.source)
                }

                // If Food has nutrients
                if (event.type == EventType.FOOD.name && (event.calories != null || event.protein != null)) {
                    val calText = event.calories?.let { "$it سعرة" } ?: ""
                    val proText = event.protein?.let { " • بروتين: ${it.toInt()} جم" } ?: ""
                    Text(
                        text = "$calText$proText (تقديري)",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary
                    )
                }

                Text(
                    text = "${timeFormatter.format(eventDate)} • ${dateFormatter.format(eventDate)}",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            // Delete action
            IconButton(
                onClick = onDelete,
                modifier = Modifier.size(36.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Delete,
                    contentDescription = "حذف الحدث",
                    tint = MaterialTheme.colorScheme.outline,
                    modifier = Modifier.size(20.dp)
                )
            }
        }
    }
}

@Composable
fun SourceBadge(source: String) {
    val (label, icon) = when (source) {
        "VOICE" -> Pair("صوت", Icons.Default.Mic)
        "AI" -> Pair("ذكاء", Icons.Default.Psychology)
        "QUICK_ACTION" -> Pair("سريع", Icons.Default.TouchApp)
        else -> Pair("يدوي", null)
    }

    Surface(
        shape = RoundedCornerShape(6.dp),
        color = MaterialTheme.colorScheme.surfaceVariant,
        modifier = Modifier.padding(horizontal = 2.dp)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            icon?.let {
                Icon(imageVector = it, contentDescription = null, modifier = Modifier.size(11.dp))
            }
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
