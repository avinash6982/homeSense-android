package com.avinash.homesense.ui.climate

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.avinash.homesense.data.model.ClimateError
import com.avinash.homesense.data.model.ClimateResult
import com.avinash.homesense.data.model.ComfortLevel
import com.avinash.homesense.data.model.Reading
import com.avinash.homesense.data.repository.ClimateRepository
import com.avinash.homesense.data.repository.RepositoryProvider
import java.time.LocalDate
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

data class DayPageState(
    val date: LocalDate,
    val readings: List<Reading> = emptyList(),
    val isLoading: Boolean = true,
    val error: ClimateError? = null,
)

data class LiveUiState(
    val reading: Reading? = null,
    val isRefreshing: Boolean = false,
) {
    val comfortLevel: ComfortLevel? get() = reading?.let(ComfortLevel::from)
}

class ClimateViewModel(
    private val repository: ClimateRepository,
    val today: LocalDate = LocalDate.now(),
    private val pollIntervalMillis: Long = 20_000L,
) : ViewModel() {

    private val _pageStates = MutableStateFlow<Map<LocalDate, DayPageState>>(emptyMap())
    val pageStates: StateFlow<Map<LocalDate, DayPageState>> = _pageStates.asStateFlow()

    private val _liveState = MutableStateFlow(LiveUiState())
    val liveState: StateFlow<LiveUiState> = _liveState.asStateFlow()

    init {
        ensureDayLoaded(today)
        startLivePolling()
    }

    /** Called when a pager page becomes visible; fetches once per date. */
    fun ensureDayLoaded(date: LocalDate) {
        if (_pageStates.value[date] != null) return
        loadDay(date)
    }

    fun retryDay(date: LocalDate) = loadDay(date)

    fun refresh(visibleDate: LocalDate) {
        viewModelScope.launch {
            _liveState.update { it.copy(isRefreshing = true) }
            loadDaySuspend(visibleDate)
            if (visibleDate == today) fetchLive()
            _liveState.update { it.copy(isRefreshing = false) }
        }
    }

    private fun loadDay(date: LocalDate) {
        viewModelScope.launch { loadDaySuspend(date) }
    }

    private suspend fun loadDaySuspend(date: LocalDate) {
        _pageStates.update { it + (date to (it[date]?.copy(isLoading = true) ?: DayPageState(date))) }
        when (val result = repository.getReadingsForDay(date)) {
            is ClimateResult.Success -> _pageStates.update {
                it + (date to DayPageState(date, result.data, isLoading = false))
            }
            is ClimateResult.Failure -> _pageStates.update {
                it + (date to DayPageState(date, emptyList(), isLoading = false, error = result.error))
            }
        }
    }

    private fun startLivePolling() {
        viewModelScope.launch {
            while (isActive) {
                fetchLive()
                delay(pollIntervalMillis)
            }
        }
    }

    private suspend fun fetchLive() {
        val result = repository.fetchCurrentReading()
        if (result is ClimateResult.Success) {
            _liveState.update { it.copy(reading = result.data) }
        }
    }

    companion object {
        fun factory(repository: ClimateRepository = RepositoryProvider.repository) =
            object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>): T =
                    ClimateViewModel(repository) as T
            }
    }
}
