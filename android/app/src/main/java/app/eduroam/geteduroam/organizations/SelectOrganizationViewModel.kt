package app.eduroam.geteduroam.organizations

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import app.eduroam.geteduroam.R
import app.eduroam.geteduroam.di.api.GetEduroamApi
import app.eduroam.geteduroam.extensions.removeNonSpacingMarks
import app.eduroam.geteduroam.models.Organization
import app.eduroam.geteduroam.ui.ErrorData
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import timber.log.Timber
import javax.inject.Inject

@HiltViewModel
class SelectOrganizationViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    val api: GetEduroamApi,
) : ViewModel() {

    var uiState by mutableStateOf(UiState())
        private set

    private var allOrganizations by mutableStateOf(emptyList<Organization>())

    val creds: MutableStateFlow<Pair<String?, String?>> =
        MutableStateFlow(Pair<String?, String?>(null, null))

    init {
        uiState = uiState.copy(isLoading = true)
        viewModelScope.launch (Dispatchers.IO) {
            try {
                val response = api.getOrganizations()
                val organizationResult = response.body()
                if (response.isSuccessful && organizationResult != null) {
                    withContext(Dispatchers.Main) {
                        allOrganizations = organizationResult.instances
                        uiState = uiState.copy(isLoading = false)
                        if (uiState.filter.isNotEmpty()) {
                            onSearchTextChange(uiState.filter)
                        }
                    }
                    do {
                        var canImproveSearchWords = false
                        allOrganizations.forEach {
                            val result = it.improveMatchWords()
                            canImproveSearchWords =  canImproveSearchWords || result
                        }
                        withContext(Dispatchers.Main) {
                            if (uiState.filter.isNotEmpty()) {
                                onSearchTextChange(uiState.filter)
                            }
                        }
                    } while (canImproveSearchWords)
                } else {
                    val failReason = "${response.code()}/${response.message()}]${
                        response.errorBody()?.string()
                    }"
                    Timber.w("Failed to load organizations: $failReason")
                    withContext(Dispatchers.Main) {
                        uiState = uiState.copy(
                            isLoading = false,
                            errorData = ErrorData(
                                titleId = R.string.err_title_generic_fail,
                                messageId = R.string.err_msg_generic_unexpected_with_arg,
                                messageArg = failReason
                            )
                        )
                    }
                }
            } catch (e: Exception) {
                Timber.w(e, "Failed to get organizations")
                withContext(Dispatchers.Main) {
                    uiState = uiState.copy(
                        isLoading = false,
                        errorData = ErrorData(
                            titleId = R.string.err_title_generic_fail,
                            messageId = R.string.err_msg_generic_unexpected_with_arg,
                            messageArg = "${e.message}/${e.javaClass.name}"
                        )
                    )
                }
            }
        }
    }

    fun onStepCompleted() {}

    fun onOrganizationSelect(organization: Organization) {
        Timber.v("User has selected organization ${organization.name} (ID: ${organization.id})")
        uiState = uiState.copy(selectedOrganization = organization)
    }

    fun onSearchTextChange(filter: String) {
        val filtered = if (filter.isNotBlank()) {
            val normalizedFilter = filter.removeNonSpacingMarks()
            val filterWords = filter.split(" ")
            allOrganizations.filter { organization ->
                if (filterWords.size == 1) {
                    organization.matchWords.any {
                        it.startsWith(normalizedFilter, ignoreCase = true)
                    }
                } else {
                    var containsAll = true
                    for (filterWord in filterWords) {
                        containsAll = containsAll && organization.matchWords.any {
                            it.startsWith(filterWord, ignoreCase = true)
                        }
                    }
                    containsAll
                }
            }.sortedBy { it.nameOrId.lowercase() }
        } else {
            emptyList()
        }
        uiState = uiState.copy(filter = filter, organizations = filtered)
    }

    fun clearDialog() {

    }

    fun clearSelection() {
        uiState = uiState.copy(selectedOrganization = null, filter = "")
    }
}