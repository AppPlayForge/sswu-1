package com.example.myTools.period

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import com.example.myTools.utils.AppBadgeManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import java.util.Calendar

data class PeriodUiState(
    val records: List<PeriodRecord> = emptyList(),
    val currentMonth: Calendar = Calendar.getInstance(),
    val recordToEdit: PeriodRecord? = null,
    val recordToDelete: PeriodRecord? = null,
    val showEducationDialog: Boolean = false,
    val showSettingsDialog: Boolean = false,
    val showDataManagementDialog: Boolean = false,
    val showPrivacyDialog: Boolean = false
) {
    val today: Long = System.currentTimeMillis()

    fun getCurrentPhase(dataManager: PeriodDataManager): Int {
        return dataManager.getCurrentPhase(today, records)
    }

    fun getUpcomingNextPeriod(dataManager: PeriodDataManager): Long? {
        return dataManager.getUpcomingNextPeriod(records)
    }
}

class PeriodViewModel(application: Application) : AndroidViewModel(application) {
    private val dataManager = PeriodDataManager(application)

    private val _uiState = MutableStateFlow(PeriodUiState())
    val uiState: StateFlow<PeriodUiState> = _uiState.asStateFlow()

    init {
        loadData()
    }

    fun loadData() {
        val list = dataManager.getRecords()
        _uiState.update { it.copy(records = list) }
        AppBadgeManager.refreshPeriodBadges(getApplication())
    }

    fun setCurrentMonth(calendar: Calendar) {
        _uiState.update { it.copy(currentMonth = calendar) }
    }

    fun changeMonth(amount: Int) {
        _uiState.update {
            val newCal = (it.currentMonth.clone() as Calendar).apply {
                add(Calendar.MONTH, amount)
            }
            it.copy(currentMonth = newCal)
        }
    }

    fun addRecord(startDate: Long, endDate: Long? = null) {
        dataManager.addRecord(startDate, endDate)
        loadData()
    }

    fun updateRecord(oldRecord: PeriodRecord, newRecord: PeriodRecord) {
        dataManager.updateRecord(oldRecord, newRecord)
        loadData()
        _uiState.update { it.copy(recordToEdit = null) }
    }

    fun updateLastRecord(endDate: Long) {
        dataManager.updateLastRecord(endDate)
        loadData()
    }

    fun deleteRecord(record: PeriodRecord) {
        dataManager.deleteRecord(record)
        loadData()
        _uiState.update { it.copy(recordToDelete = null) }
    }

    fun setRecordToEdit(record: PeriodRecord?) {
        _uiState.update { it.copy(recordToEdit = record) }
    }

    fun setRecordToDelete(record: PeriodRecord?) {
        _uiState.update { it.copy(recordToDelete = record) }
    }

    fun setShowEducationDialog(show: Boolean) {
        _uiState.update { it.copy(showEducationDialog = show) }
    }

    fun setShowSettingsDialog(show: Boolean) {
        _uiState.update { it.copy(showSettingsDialog = show) }
    }

    fun setShowDataManagementDialog(show: Boolean) {
        _uiState.update { it.copy(showDataManagementDialog = show) }
    }

    fun setShowPrivacyDialog(show: Boolean) {
        _uiState.update { it.copy(showPrivacyDialog = show) }
    }
}
