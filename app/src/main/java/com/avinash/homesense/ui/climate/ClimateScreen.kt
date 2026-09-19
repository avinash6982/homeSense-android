package com.avinash.homesense.ui.climate

import androidx.compose.foundation.background
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
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.AcUnit
import androidx.compose.material.icons.filled.Air
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material.icons.filled.Opacity
import androidx.compose.material.icons.filled.Thermostat
import androidx.compose.material.icons.filled.WbSunny
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.avinash.homesense.data.model.ComfortLevel
import com.avinash.homesense.data.model.Reading
import com.avinash.homesense.ui.components.ClimateErrorState
import com.avinash.homesense.ui.components.DayTimelineCharts
import com.avinash.homesense.ui.components.DayTimelineChartsSkeleton
import com.avinash.homesense.ui.components.EmptyDayState
import com.avinash.homesense.ui.util.rememberRelativeTimeText
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle
import java.util.Locale
import kotlinx.coroutines.launch

private const val PAGE_COUNT = 3650
private const val TODAY_PAGE_INDEX = PAGE_COUNT - 1

private fun pageIndexToDate(page: Int, today: LocalDate): LocalDate =
    today.minusDays((TODAY_PAGE_INDEX - page).toLong())

private fun mergeWithLive(pageReadings: List<Reading>, liveReading: Reading?, isToday: Boolean): List<Reading> =
    if (isToday && liveReading != null) {
        (pageReadings + liveReading).distinctBy { it.timestamp }.sortedBy { it.timestamp }
    } else {
        pageReadings
    }

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ClimateScreen(
    viewModel: ClimateViewModel = viewModel(factory = ClimateViewModel.factory()),
) {
    val today = viewModel.today
    val liveState by viewModel.liveState.collectAsState()
    val pageStates by viewModel.pageStates.collectAsState()

    val pagerState = rememberPagerState(initialPage = TODAY_PAGE_INDEX) { PAGE_COUNT }
    val coroutineScope = rememberCoroutineScope()
    val visibleDate = pageIndexToDate(pagerState.currentPage, today)
    val isVisibleToday = visibleDate == today

    LaunchedEffect(pagerState.currentPage) {
        viewModel.ensureDayLoaded(visibleDate)
    }

    val visiblePageState = pageStates[visibleDate] ?: DayPageState(visibleDate)
    val visibleReadings = remember(visiblePageState.readings, liveState.reading, isVisibleToday) {
        mergeWithLive(visiblePageState.readings, liveState.reading, isVisibleToday)
    }
    val daySummary = remember(visibleReadings, isVisibleToday) {
        if (isVisibleToday) null else DaySummary.from(visibleReadings)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("HomeSense", fontWeight = FontWeight.SemiBold) },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background),
            )
        },
        containerColor = MaterialTheme.colorScheme.background,
    ) { padding ->
        PullToRefreshBox(
            isRefreshing = liveState.isRefreshing,
            onRefresh = { viewModel.refresh(visibleDate) },
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState()),
            ) {
                HeroConditionsCard(
                    isToday = isVisibleToday,
                    liveReading = liveState.reading,
                    liveComfort = liveState.comfortLevel,
                    daySummary = daySummary,
                    isDayLoading = visiblePageState.isLoading,
                )

                Spacer(modifier = Modifier.height(16.dp))

                DateNavRow(
                    visibleDate = visibleDate,
                    today = today,
                    onPrevious = {
                        coroutineScope.launch {
                            pagerState.animateScrollToPage((pagerState.currentPage - 1).coerceAtLeast(0))
                        }
                    },
                    onNext = {
                        coroutineScope.launch {
                            pagerState.animateScrollToPage((pagerState.currentPage + 1).coerceAtMost(TODAY_PAGE_INDEX))
                        }
                    },
                    onJumpToToday = {
                        coroutineScope.launch { pagerState.animateScrollToPage(TODAY_PAGE_INDEX) }
                    },
                )

                Spacer(modifier = Modifier.height(12.dp))

                HorizontalPager(
                    state = pagerState,
                    verticalAlignment = Alignment.Top,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(420.dp),
                ) { page ->
                    val date = pageIndexToDate(page, today)
                    val pageState = pageStates[date] ?: DayPageState(date)
                    val chartReadings = remember(pageState.readings, liveState.reading, date == today) {
                        mergeWithLive(pageState.readings, liveState.reading, date == today)
                    }
                    DayPageContent(
                        pageState = pageState,
                        chartReadings = chartReadings,
                        date = date,
                        isToday = date == today,
                        onRetry = { viewModel.retryDay(date) },
                    )
                }

                Spacer(modifier = Modifier.height(24.dp))
            }
        }
    }
}

private data class DaySummary(
    val highTemp: Double,
    val lowTemp: Double,
    val avgTemp: Double,
    val avgHumidity: Double,
    val dominantComfort: ComfortLevel,
) {
    companion object {
        fun from(readings: List<Reading>): DaySummary? {
            if (readings.isEmpty()) return null
            val temps = readings.map { it.temperatureCelsius }
            val humidities = readings.map { it.humidityPercent }
            val dominant = readings
                .map(ComfortLevel::from)
                .groupingBy { it }
                .eachCount()
                .maxByOrNull { it.value }
                ?.key
                ?: ComfortLevel.OPTIMAL
            return DaySummary(
                highTemp = temps.max(),
                lowTemp = temps.min(),
                avgTemp = temps.average(),
                avgHumidity = humidities.average(),
                dominantComfort = dominant,
            )
        }
    }
}

@Composable
private fun HeroConditionsCard(
    isToday: Boolean,
    liveReading: Reading?,
    liveComfort: ComfortLevel?,
    daySummary: DaySummary?,
    isDayLoading: Boolean,
) {
    val comfort = if (isToday) liveComfort else daySummary?.dominantComfort
    val gradient = comfort.heroGradient()
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .clip(RoundedCornerShape(28.dp))
            .background(Brush.linearGradient(gradient)),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp, vertical = 28.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            if (isToday) {
                if (liveReading == null) {
                    HeroLoadingContent()
                    return@Column
                }
                val relativeTime = rememberRelativeTimeText(liveReading.timestamp)

                HeroIcon(comfort)
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "%.1f°C".format(liveReading.temperatureCelsius),
                    style = MaterialTheme.typography.displayLarge,
                    color = Color.White,
                    fontWeight = FontWeight.Medium,
                )
                Spacer(modifier = Modifier.height(4.dp))
                HeroHumidityLine("%.0f%% humidity".format(liveReading.humidityPercent))
                Spacer(modifier = Modifier.height(16.dp))
                if (comfort != null) HeroComfortPill(comfort.label)
                Spacer(modifier = Modifier.height(10.dp))
                Text(text = relativeTime, style = MaterialTheme.typography.bodySmall, color = Color.White.copy(alpha = 0.75f))
            } else {
                if (isDayLoading) {
                    HeroLoadingContent()
                    return@Column
                }
                if (daySummary == null) {
                    HeroIcon(null)
                    Spacer(modifier = Modifier.height(8.dp))
                    Text("No data for this day", color = Color.White, style = MaterialTheme.typography.titleMedium)
                    return@Column
                }
                HeroIcon(comfort)
                Spacer(modifier = Modifier.height(8.dp))
                Row(verticalAlignment = Alignment.Bottom) {
                    Text(
                        text = "%.1f°".format(daySummary.highTemp),
                        style = MaterialTheme.typography.displayLarge,
                        color = Color.White,
                        fontWeight = FontWeight.Medium,
                    )
                    Text(
                        text = "  /  %.1f°C".format(daySummary.lowTemp),
                        style = MaterialTheme.typography.headlineSmall,
                        color = Color.White.copy(alpha = 0.85f),
                        modifier = Modifier.padding(bottom = 10.dp),
                    )
                }
                Spacer(modifier = Modifier.height(4.dp))
                HeroHumidityLine("%.0f%% avg humidity".format(daySummary.avgHumidity))
                Spacer(modifier = Modifier.height(16.dp))
                HeroComfortPill("Mostly ${daySummary.dominantComfort.label}")
                Spacer(modifier = Modifier.height(10.dp))
                Text(text = "Day summary", style = MaterialTheme.typography.bodySmall, color = Color.White.copy(alpha = 0.75f))
            }
        }
    }
}

/**
 * Mirrors the loaded hero content's exact structure (icon, big number line,
 * humidity line, pill, footer line) with a spinner standing in for the icon,
 * so the card doesn't change height once real data replaces it.
 */
@Composable
private fun HeroLoadingContent() {
    CircularProgressIndicator(color = Color.White, modifier = Modifier.size(48.dp))
    Spacer(modifier = Modifier.height(8.dp))
    Text(
        text = "--.-°C",
        style = MaterialTheme.typography.displayLarge,
        color = Color.Transparent,
        fontWeight = FontWeight.Medium,
    )
    Spacer(modifier = Modifier.height(4.dp))
    HeroHumidityLine("")
    Spacer(modifier = Modifier.height(16.dp))
    HeroComfortPill("")
    Spacer(modifier = Modifier.height(10.dp))
    Text(text = "", style = MaterialTheme.typography.bodySmall)
}

@Composable
private fun HeroIcon(comfort: ComfortLevel?) {
    Icon(
        imageVector = comfort?.icon() ?: Icons.Default.Thermostat,
        contentDescription = null,
        modifier = Modifier.size(48.dp),
        tint = Color.White,
    )
}

@Composable
private fun HeroHumidityLine(text: String) {
    Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
        Icon(
            imageVector = Icons.Default.Opacity,
            contentDescription = null,
            modifier = Modifier.size(16.dp),
            tint = Color.White.copy(alpha = 0.9f),
        )
        Text(text = text, style = MaterialTheme.typography.bodyLarge, color = Color.White.copy(alpha = 0.9f))
    }
}

@Composable
private fun HeroComfortPill(text: String) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(50))
            .background(Color.White.copy(alpha = 0.22f))
            .padding(horizontal = 16.dp, vertical = 8.dp),
    ) {
        Text(text, color = Color.White, fontWeight = FontWeight.SemiBold)
    }
}

private fun ComfortLevel?.heroGradient(): List<Color> = when (this) {
    ComfortLevel.OPTIMAL -> listOf(Color(0xFF0F9B8E), Color(0xFF1BAF7A))
    ComfortLevel.TOO_HOT -> listOf(Color(0xFFEB6834), Color(0xFFE34948))
    ComfortLevel.TOO_COLD -> listOf(Color(0xFF2A78D6), Color(0xFF4A3AA7))
    ComfortLevel.HIGH_HUMIDITY -> listOf(Color(0xFF2A78D6), Color(0xFF4A3AA7))
    ComfortLevel.LOW_HUMIDITY -> listOf(Color(0xFFEDA100), Color(0xFFEB6834))
    null -> listOf(Color(0xFF546E7A), Color(0xFF37474F))
}

private fun ComfortLevel.icon(): ImageVector = when (this) {
    ComfortLevel.OPTIMAL -> Icons.Default.WbSunny
    ComfortLevel.TOO_HOT -> Icons.Default.LocalFireDepartment
    ComfortLevel.TOO_COLD -> Icons.Default.AcUnit
    ComfortLevel.HIGH_HUMIDITY -> Icons.Default.Opacity
    ComfortLevel.LOW_HUMIDITY -> Icons.Default.Air
}

@Composable
private fun DateNavRow(
    visibleDate: LocalDate,
    today: LocalDate,
    onPrevious: () -> Unit,
    onNext: () -> Unit,
    onJumpToToday: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center,
    ) {
        IconButton(
            onClick = onPrevious,
            colors = IconButtonDefaults.iconButtonColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh),
        ) {
            Icon(Icons.AutoMirrored.Filled.KeyboardArrowLeft, contentDescription = "Previous day")
        }
        Text(
            text = formatDateLabel(visibleDate, today),
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.padding(horizontal = 12.dp),
        )
        IconButton(
            onClick = onNext,
            enabled = visibleDate.isBefore(today),
            colors = IconButtonDefaults.iconButtonColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh),
        ) {
            Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = "Next day")
        }
        if (visibleDate != today) {
            Spacer(modifier = Modifier.width(8.dp))
            FilterChip(selected = false, onClick = onJumpToToday, label = { Text("Today") })
        }
    }
}

private fun formatDateLabel(date: LocalDate, today: LocalDate): String = when {
    date == today -> "Today"
    date == today.minusDays(1) -> "Yesterday"
    else -> {
        val weekday = date.dayOfWeek.getDisplayName(TextStyle.SHORT, Locale.getDefault())
        val monthDay = date.format(DateTimeFormatter.ofPattern("MMM d"))
        "$weekday, $monthDay"
    }
}

@Composable
private fun DayPageContent(
    pageState: DayPageState,
    chartReadings: List<Reading>,
    date: LocalDate,
    isToday: Boolean,
    onRetry: () -> Unit,
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
        ) {
            when {
                pageState.isLoading -> Column {
                    DayTimelineChartsSkeleton()
                    Spacer(modifier = Modifier.height(16.dp))
                    DayStatsRowSkeleton()
                }

                pageState.error != null -> ClimateErrorState(error = pageState.error, onRetry = onRetry)

                chartReadings.isEmpty() -> EmptyDayState()

                else -> Column {
                    DayTimelineCharts(readings = chartReadings, date = date, isToday = isToday)
                    Spacer(modifier = Modifier.height(16.dp))
                    DayStatsRow(chartReadings)
                }
            }
        }
    }
}

@Composable
private fun DayStatsRow(readings: List<Reading>) {
    if (readings.isEmpty()) return
    val temps = readings.map { it.temperatureCelsius }
    val humidities = readings.map { it.humidityPercent }

    Column {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
            StatItem(label = "High", value = "%.1f°C".format(temps.max()))
            StatItem(label = "Low", value = "%.1f°C".format(temps.min()))
            StatItem(label = "Avg", value = "%.1f°C".format(temps.average()))
        }
        Spacer(modifier = Modifier.height(12.dp))
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
            StatItem(label = "High", value = "%.0f%%".format(humidities.max()))
            StatItem(label = "Low", value = "%.0f%%".format(humidities.min()))
            StatItem(label = "Avg", value = "%.0f%%".format(humidities.average()))
        }
    }
}

/** Same [StatItem] rows as [DayStatsRow] with placeholder values, so the loading skeleton matches its height. */
@Composable
private fun DayStatsRowSkeleton() {
    Column {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
            StatItem(label = "High", value = "--")
            StatItem(label = "Low", value = "--")
            StatItem(label = "Avg", value = "--")
        }
        Spacer(modifier = Modifier.height(12.dp))
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
            StatItem(label = "High", value = "--")
            StatItem(label = "Low", value = "--")
            StatItem(label = "Avg", value = "--")
        }
    }
}

@Composable
private fun StatItem(label: String, value: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(text = value, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
        Text(text = label, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}
