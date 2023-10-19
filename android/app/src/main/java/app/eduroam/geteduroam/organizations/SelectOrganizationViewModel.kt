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
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
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
        viewModelScope.launch {
            uiState = uiState.copy(isLoading = true)
            try {
                val response = api.getOrganizations()
                val organizationResult = response.body()
                if (response.isSuccessful && organizationResult != null) {
                    allOrganizations = organizationResult.instances
                    uiState = uiState.copy(isLoading = false)
                } else {
                    val failReason = "${response.code()}/${response.message()}]${
                        response.errorBody()?.string()
                    }"
                    Timber.e("Failed to load organizations: $failReason")
                    uiState = uiState.copy(
                        isLoading = false,
                        errorData = ErrorData(
                            titleId = R.string.err_title_generic_fail,
                            messageId = R.string.err_msg_generic_unexpected_with_arg,
                            messageArg = failReason
                        )
                    )
                }
            } catch (e: Exception) {
                Timber.e("Failed to get organizations", e)
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

    fun onStepCompleted() {

    }

    fun onOrganizationSelect(organization: Organization) {
        uiState = uiState.copy(selectedOrganization = organization)
    }

    fun onSearchTextChange(filter: String) {
        val filtered = if (filter.isNotBlank()) {
            val normalizedFilter = filter.removeNonSpacingMarks()
            allOrganizations.filter { organization ->
                organization.matchWords.any {
                    it.startsWith(normalizedFilter, ignoreCase = true)
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