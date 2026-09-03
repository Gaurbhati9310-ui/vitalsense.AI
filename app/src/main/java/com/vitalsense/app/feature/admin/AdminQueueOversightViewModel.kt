package com.vitalsense.app.feature.admin

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.vitalsense.app.core.data.model.DoctorQueueSummary
import com.vitalsense.app.core.data.model.QueueEntry
import com.vitalsense.app.core.data.repository.VitalSenseRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.*
import java.text.SimpleDateFormat
import java.util.*
import javax.inject.Inject

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class AdminQueueOversightViewModel @Inject constructor(
    private val repository: VitalSenseRepository
) : ViewModel() {

    val todayDateFormatted: String
        get() = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())

    // All doctor queue summaries across the district for today
    val doctorQueueSummaries: StateFlow<List<DoctorQueueSummary>> =
        repository.observeAllDoctorQueueSummaries(todayDateFormatted)
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _selectedDoctorForDrillDown = MutableStateFlow<DoctorQueueSummary?>(null)
    val selectedDoctorForDrillDown: StateFlow<DoctorQueueSummary?> = _selectedDoctorForDrillDown.asStateFlow()

    // Full queue entries for the selected doctor (Read-only administrative inspection)
    val selectedDoctorFullQueue: StateFlow<List<QueueEntry>> = _selectedDoctorForDrillDown.flatMapLatest { summary ->
        if (summary != null) {
            repository.observeDoctorQueue(summary.doctorId, summary.dateFormatted)
        } else {
            flowOf(emptyList())
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun selectDoctorForDrillDown(summary: DoctorQueueSummary) {
        _selectedDoctorForDrillDown.value = summary
    }

    fun clearDrillDown() {
        _selectedDoctorForDrillDown.value = null
    }
}
