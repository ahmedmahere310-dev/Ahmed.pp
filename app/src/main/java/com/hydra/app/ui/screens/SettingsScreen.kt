package com.hydra.app.ui.screens

import android.content.Intent
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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.DeleteForever
import androidx.compose.material.icons.filled.FileDownload
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.NightsStay
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.Psychology
import androidx.compose.material.icons.filled.Restaurant
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material.icons.filled.WaterDrop
import androidx.compose.material.icons.filled.Wc
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import com.hydra.app.data.model.AiProvider
import com.hydra.app.ui.viewmodel.HydraUiState
import com.hydra.app.ui.viewmodel.HydraViewModel
import kotlinx.coroutines.launch

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun SettingsScreen(
    viewModel: HydraViewModel,
    uiState: HydraUiState
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    var apiKeyInput by remember { mutableStateOf("") }
    var isKeyVisible by remember { mutableStateOf(false) }
    var showClearDialog by remember { mutableStateOf(false) }
    var showExportDialog by remember { mutableStateOf(false) }
    var exportedJsonString by remember { mutableStateOf("") }

    // Sliders state
    var minDelay by remember(uiState.userPreferences.minDelayMinutes) {
        mutableFloatStateOf(uiState.userPreferences.minDelayMinutes.toFloat())
    }
    var maxDelay by remember(uiState.userPreferences.maxDelayMinutes) {
        mutableFloatStateOf(uiState.userPreferences.maxDelayMinutes.toFloat())
    }
    var waterGoal by remember(uiState.userPreferences.dailyWaterGoalMl) {
        mutableFloatStateOf(uiState.userPreferences.dailyWaterGoalMl.toFloat())
    }

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
                    text = "الإعدادات والتحكم",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "التذكيرات الدورية، الذكاء الاصطناعي، والخصوصية المحلية",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            // 1. Periodic Reminders (شرب الماء والأكل)
            item {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("card_periodic_reminders"),
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer)
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.NotificationsActive,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary
                            )
                            Text(
                                text = "التذكيرات الدورية (الماء والطعام)",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                        }

                        Text(
                            text = "تنبيهات دورية منتظمة تضمن شربك المستمر للماء وتناول وجباتك في موعدها.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )

                        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))

                        // A. Water Periodic Reminder
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.WaterDrop,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(20.dp)
                                )
                                Column {
                                    Text(
                                        text = "تذكير دوري بشرب الماء",
                                        fontWeight = FontWeight.SemiBold
                                    )
                                    uiState.nextWaterReminderText?.let { nextText ->
                                        Text(
                                            text = "الموعد القادم: $nextText",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.primary
                                        )
                                    }
                                }
                            }
                            Switch(
                                checked = uiState.userPreferences.periodicWaterEnabled,
                                onCheckedChange = { viewModel.setPeriodicWaterReminder(it) },
                                modifier = Modifier.testTag("switch_periodic_water")
                            )
                        }

                        if (uiState.userPreferences.periodicWaterEnabled) {
                            Text(
                                text = "تكرار التذكير كل:",
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Medium
                            )

                            val waterIntervals = listOf(
                                30 to "30 د",
                                45 to "45 د",
                                60 to "ساعة",
                                90 to "90 د",
                                120 to "ساعتان",
                                180 to "3 ساعات"
                            )

                            FlowRow(
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                waterIntervals.forEach { (minutes, label) ->
                                    val isSelected = uiState.userPreferences.waterIntervalMinutes == minutes
                                    FilterChip(
                                        selected = isSelected,
                                        onClick = { viewModel.setPeriodicWaterReminder(true, minutes) },
                                        label = { Text(label) },
                                        leadingIcon = if (isSelected) {
                                            { Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(16.dp)) }
                                        } else null
                                    )
                                }
                            }

                            OutlinedButton(
                                onClick = { viewModel.testPeriodicWaterReminder() },
                                modifier = Modifier.fillMaxWidth().testTag("btn_test_water_reminder")
                            ) {
                                Icon(imageVector = Icons.Default.WaterDrop, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("تجربة إشعار شرب الماء الآن 💧")
                            }
                        }

                        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))

                        // B. Food Periodic Reminder
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Restaurant,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.tertiary,
                                    modifier = Modifier.size(20.dp)
                                )
                                Column {
                                    Text(
                                        text = "تذكير دوري بالوجبات والأكل",
                                        fontWeight = FontWeight.SemiBold
                                    )
                                    uiState.nextFoodReminderText?.let { nextText ->
                                        Text(
                                            text = "الموعد القادم: $nextText",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.tertiary
                                        )
                                    }
                                }
                            }
                            Switch(
                                checked = uiState.userPreferences.periodicFoodEnabled,
                                onCheckedChange = { viewModel.setPeriodicFoodReminder(it) },
                                modifier = Modifier.testTag("switch_periodic_food")
                            )
                        }

                        if (uiState.userPreferences.periodicFoodEnabled) {
                            Text(
                                text = "تكرار تذكير الطعام كل:",
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Medium
                            )

                            val foodIntervals = listOf(
                                120 to "ساعتان",
                                180 to "3 ساعات",
                                240 to "4 ساعات",
                                300 to "5 ساعات",
                                360 to "6 ساعات"
                            )

                            FlowRow(
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                foodIntervals.forEach { (minutes, label) ->
                                    val isSelected = uiState.userPreferences.foodIntervalMinutes == minutes
                                    FilterChip(
                                        selected = isSelected,
                                        onClick = { viewModel.setPeriodicFoodReminder(true, minutes) },
                                        label = { Text(label) },
                                        leadingIcon = if (isSelected) {
                                            { Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(16.dp)) }
                                        } else null
                                    )
                                }
                            }

                            OutlinedButton(
                                onClick = { viewModel.testPeriodicFoodReminder() },
                                modifier = Modifier.fillMaxWidth().testTag("btn_test_food_reminder")
                            ) {
                                Icon(imageVector = Icons.Default.Restaurant, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("تجربة إشعار الوجبات الآن 🍽️")
                            }
                        }

                        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))

                        // C. Quiet Hours Notice
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.NightsStay,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.secondary,
                                modifier = Modifier.size(20.dp)
                            )
                            Column {
                                Text(
                                    text = "ساعات النوم والهدوء (عدم الإزعاج)",
                                    fontWeight = FontWeight.SemiBold,
                                    style = MaterialTheme.typography.bodyMedium
                                )
                                Text(
                                    text = "تلقائياً من ${uiState.userPreferences.quietHoursStart}:00 مساءً إلى ${uiState.userPreferences.quietHoursEnd}:00 صباحاً لا تصدر تذكيرات، ويتم تأجيلها للصباح.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
            }

            // 2. Smart Bathroom Prediction & Reminders Settings
            item {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("card_bathroom_prediction_settings"),
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer)
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Wc,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary
                            )
                            Column {
                                Text(
                                    text = "خوارزمية وتنبيهات دخول الحمام الذكية",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = "حساب السعرات، كمية الماء، وسرعة الهضم التقديرية",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }

                        // Physiology Explanation Banner
                        Surface(
                            shape = RoundedCornerShape(14.dp),
                            color = MaterialTheme.colorScheme.surfaceContainerHigh
                        ) {
                            Column(
                                modifier = Modifier.padding(12.dp),
                                verticalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Text(
                                    text = "💡 كيف تعمل الخوارزمية الفسيولوجية؟",
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary
                                )
                                Text(
                                    text = "• إدرار البول والترطيب: تحسب كمية الماء المتناولة وسرعة فلترة الكلى لجرعات السوائل (بين 35 و70 دقيقة).\n• المنعكس المعدي القولوني: تحسب كثافة السعرات الحرارية للوجبات وتأثيرها في تنشيط حركة الجهاز الهضمي (بين 25 و60 دقيقة).\n• التفاعل المشترك: تدمج تأثير الطعام على تفريغ السوائل وتبطئ أو تسرع التنبيه بناءً على الوقت المنقضي.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }

                        // Reminders Toggle
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(text = "تفعيل خوارزمية وتنبيهات الحمام", fontWeight = FontWeight.SemiBold)
                                Text(
                                    text = "إشعار تفاعلي ذكي يذكرك بدخول الحمام بناءً على الماء والسعرات",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            Switch(
                                checked = uiState.userPreferences.remindersEnabled,
                                onCheckedChange = { viewModel.setRemindersEnabled(it) },
                                modifier = Modifier.testTag("switch_bathroom_reminders")
                            )
                        }

                        // Learning Enabled Toggle
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(text = "التعلم الذاتي التكيفي من استجاباتك", fontWeight = FontWeight.SemiBold)
                                Text(
                                    text = "تعديل وقت التنبيه تدريجياً ليتطابق مع سرعة هضم وترطيب جسمك",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            Switch(
                                checked = uiState.userPreferences.learningEnabled,
                                onCheckedChange = { viewModel.setLearningEnabled(it) },
                                modifier = Modifier.testTag("switch_learning_enabled")
                            )
                        }

                        // Delay Bounds Sliders
                        Text(
                            text = "نطاق وقت التنبيه المقدر: ${minDelay.toInt()} إلى ${maxDelay.toInt()} دقيقة",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.SemiBold
                        )

                        Text(
                            text = "الحد الأدنى: ${minDelay.toInt()} دقيقة",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Slider(
                            value = minDelay,
                            onValueChange = { minDelay = it },
                            onValueChangeFinished = {
                                viewModel.setDelayBounds(minDelay.toInt(), maxDelay.toInt())
                            },
                            valueRange = 15f..60f,
                            steps = 8
                        )

                        Text(
                            text = "الحد الأقصى: ${maxDelay.toInt()} دقيقة",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Slider(
                            value = maxDelay,
                            onValueChange = { maxDelay = it },
                            onValueChangeFinished = {
                                viewModel.setDelayBounds(minDelay.toInt(), maxDelay.toInt())
                            },
                            valueRange = 60f..180f,
                            steps = 11
                        )

                        // Quick Test Button
                        FilledTonalButton(
                            onClick = { viewModel.testBathroomReminder() },
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("btn_test_bathroom_notification")
                        ) {
                            Icon(imageVector = Icons.Default.Wc, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("تجربة إشعار دخول الحمام الآن 🚻")
                        }
                    }
                }
            }

            // 3. AI Integration Card
            item {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("card_ai_settings"),
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
                                tint = MaterialTheme.colorScheme.primary
                            )
                            Text(
                                text = "الذكاء الاصطناعي (اختياري)",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        // Radio: Off
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            RadioButton(
                                selected = uiState.userPreferences.aiProvider == AiProvider.OFF,
                                onClick = { viewModel.setAiProvider(AiProvider.OFF) }
                            )
                            Text(
                                text = "محلي فقط (بدون إنترنت)",
                                style = MaterialTheme.typography.bodyMedium
                            )
                        }

                        // Radio: Gemini
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            RadioButton(
                                selected = uiState.userPreferences.aiProvider == AiProvider.GEMINI,
                                onClick = { viewModel.setAiProvider(AiProvider.GEMINI) }
                            )
                            Text(
                                text = "Gemini AI (مفتاح API الخاص بك)",
                                style = MaterialTheme.typography.bodyMedium
                            )
                        }

                        // If Gemini enabled
                        if (uiState.userPreferences.aiProvider == AiProvider.GEMINI) {
                            Spacer(modifier = Modifier.height(10.dp))
                            OutlinedTextField(
                                value = apiKeyInput,
                                onValueChange = { apiKeyInput = it },
                                label = { Text("أدخل مفتاح Gemini API") },
                                placeholder = { Text("AIzaSy...") },
                                visualTransformation = if (isKeyVisible) VisualTransformation.None else PasswordVisualTransformation(),
                                trailingIcon = {
                                    IconButton(onClick = { isKeyVisible = !isKeyVisible }) {
                                        Icon(
                                            imageVector = if (isKeyVisible) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                                            contentDescription = null
                                        )
                                    }
                                },
                                modifier = Modifier.fillMaxWidth(),
                                singleLine = true
                            )

                            Spacer(modifier = Modifier.height(10.dp))

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Button(
                                    onClick = {
                                        viewModel.saveGeminiApiKey(apiKeyInput.trim())
                                        apiKeyInput = ""
                                    },
                                    enabled = apiKeyInput.isNotBlank(),
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Icon(imageVector = Icons.Default.Key, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("حفظ المفتاح")
                                }

                                OutlinedButton(
                                    onClick = { viewModel.testGeminiConnection() },
                                    enabled = uiState.hasApiKey && !uiState.isTestingApi,
                                    modifier = Modifier.weight(1f)
                                ) {
                                    if (uiState.isTestingApi) {
                                        CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
                                    } else {
                                        Text("اختبار الاتصال")
                                    }
                                }
                            }

                            // Keystore safety note
                            Spacer(modifier = Modifier.height(8.dp))
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Lock,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(14.dp)
                                )
                                Text(
                                    text = if (uiState.hasApiKey) "مفتاح API مشفر ومخزن بأمان داخل Android Keystore" else "لم يتم تخزين مفتاح API بعد",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = if (uiState.hasApiKey) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }

                            uiState.apiTestResult?.let { testResult ->
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    text = testResult,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = if (testResult.startsWith("اتصال ناجح")) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error
                                )
                            }
                        }
                    }
                }
            }

            // 4. Daily Water Goal Card
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
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
                                text = "هدف شرب الماء اليومي: ${waterGoal.toInt()} مل",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                        }

                        Slider(
                            value = waterGoal,
                            onValueChange = { waterGoal = it },
                            onValueChangeFinished = {
                                viewModel.setDailyWaterGoal(waterGoal.toInt())
                            },
                            valueRange = 1000f..5000f,
                            steps = 15
                        )
                    }
                }
            }

            // 5. Data Management & Privacy
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer)
                ) {
                    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Security,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary
                            )
                            Text(
                                text = "إدارة البيانات والخصوصية",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                        }

                        Text(
                            text = "جميع بياناتك مشفرة ومحفوظة داخل جهازك فقط (Offline-First). لا توجد خوادم خارجية أو تتبع.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            OutlinedButton(
                                onClick = {
                                    coroutineScope.launch {
                                        exportedJsonString = viewModel.exportDataJson()
                                        showExportDialog = true
                                    }
                                },
                                modifier = Modifier.weight(1f)
                            ) {
                                Icon(imageVector = Icons.Default.FileDownload, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("تصدير JSON")
                            }

                            Button(
                                onClick = { showClearDialog = true },
                                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                                modifier = Modifier.weight(1f)
                            ) {
                                Icon(imageVector = Icons.Default.DeleteForever, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("مسح البيانات")
                            }
                        }
                    }
                }
            }

            // About Footer
            item {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "HYDRA — Life Tracker",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        text = "الإصدار 1.1 • Offline First • ذكاء اصطناعي محلي وتذكيرات دورية",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }

    // Clear Data Confirmation Dialog
    if (showClearDialog) {
        AlertDialog(
            onDismissRequest = { showClearDialog = false },
            title = { Text("تأكيد مسح البيانات") },
            text = { Text("هل أنت متأكد من رغبتك في حذف جميع الأحداث المسجلة ونماذج التنبؤ السلوكية؟ هذا الإجراء لا يمكن التراجع عنه.") },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.clearAllData()
                        showClearDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("نعم، احذف الكل")
                }
            },
            dismissButton = {
                OutlinedButton(onClick = { showClearDialog = false }) {
                    Text("إلغاء")
                }
            }
        )
    }

    // Export Data Dialog
    if (showExportDialog) {
        AlertDialog(
            onDismissRequest = { showExportDialog = false },
            title = { Text("تصدير البيانات (JSON)") },
            text = {
                Column {
                    Text(
                        text = "نسخة احتياطية بتنسيق JSON لجميع الأحداث المسجلة والتنبؤات السلوكية والتذكيرات:",
                        style = MaterialTheme.typography.bodySmall
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = exportedJsonString,
                        onValueChange = {},
                        readOnly = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(200.dp)
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val shareIntent = Intent(Intent.ACTION_SEND).apply {
                            type = "application/json"
                            putExtra(Intent.EXTRA_TEXT, exportedJsonString)
                        }
                        context.startActivity(Intent.createChooser(shareIntent, "مشاركة بيانات Hydra"))
                        showExportDialog = false
                    }
                ) {
                    Text("مشاركة")
                }
            },
            dismissButton = {
                TextButton(onClick = { showExportDialog = false }) {
                    Text("إغلاق")
                }
            }
        )
    }
}
