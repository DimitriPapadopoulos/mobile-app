package app.eduroam.geteduroam.institutions

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.Scaffold
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.ExperimentalLifecycleComposeApi
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import app.eduroam.geteduroam.EduTopAppBar
import app.eduroam.geteduroam.R
import app.eduroam.geteduroam.Screens
import app.eduroam.shared.models.DataState
import app.eduroam.shared.models.ItemDataSummary
import app.eduroam.shared.response.Institution
import app.eduroam.shared.response.Profile
import app.eduroam.shared.select.SelectInstitutionViewModel
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.util.*
import java.util.stream.Collectors

@OptIn(ExperimentalLifecycleComposeApi::class)
@Composable
fun SelectInstitutionScreen(
    viewModel: SelectInstitutionViewModel,
    goToOAuth: (String, Profile) -> Unit,
    gotToProfileSelection: (String) -> Unit,
    goToConfigScreen: (String) -> Unit,
) {
    val uiDataState: DataState<ItemDataSummary> by viewModel.uiDataState.collectAsStateWithLifecycle()
    val authorizationUrl by viewModel.authorizationUrl.collectAsStateWithLifecycle(null)
    val selectedInstitution by viewModel.currentInstitution.collectAsStateWithLifecycle(null)
    val configData by viewModel.configData.collectAsStateWithLifecycle(null)

    selectedInstitution?.let {
        LaunchedEffect(it) {
            viewModel.clearCurrentInstitutionSelection()
            val institutionArgument = Json.encodeToString(it)
            gotToProfileSelection(institutionArgument)
        }
    }

    configData?.let {
        LaunchedEffect(it) {
            viewModel.clearWifiConfigData()
            val wifiConfigData = Json.encodeToString(it)
            goToConfigScreen(wifiConfigData)
        }
    }

    authorizationUrl?.let {
        LaunchedEffect(it) {
            val firstProfile = viewModel.getFirstProfile()
            if (firstProfile != null) {
                viewModel.handledAuthorization()
                goToOAuth(it, firstProfile)
            }
        }
    }

    SelectInstitutionContent(
        institutionsState = uiDataState,
        onSelectInstitution = { institution ->
            viewModel.onInstitutionSelect(
                institution, Screens.OAuth.redirectUrl, Screens.OAuth.APP_ID
            )
        },
        searchText = uiDataState.data?.filterOn.orEmpty(),
        onSearchTextChange = { viewModel.onSearchTextChange(it) },
    )
}

@Composable
fun SelectInstitutionContent(
    institutionsState: DataState<ItemDataSummary>,
    onSelectInstitution: (Institution) -> Unit,
    searchText: String,
    onSearchTextChange: (String) -> Unit = {},
) = Scaffold(topBar = {
    EduTopAppBar(stringResource(R.string.name))
}) { paddingValues ->
    Column(
        Modifier
            .padding(paddingValues)
            .fillMaxSize()
            .systemBarsPadding()
            .padding(horizontal = 16.dp)
    ) {
        LazyColumn {
            item {
                InstitutionSearchHeader(
                    searchText = searchText,
                    onSearchTextChange = onSearchTextChange,
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(Modifier.height(8.dp))
            }

            if (institutionsState.loading) {
                item {
                    LinearProgressIndicator(
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            } else if (institutionsState.exception != null) {
                item {
                    Text(
                        text = institutionsState.exception.orEmpty(),
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodyLarge,
                    )
                }
            } else {
                if (institutionsState.empty) {
                    item {
                        Text(stringResource(id = R.string.institutions_no_results))
                    }
                } else {

                    item {
                        Text(
                            text = stringResource(id = R.string.institutions_choose_one),
                            style = MaterialTheme.typography.bodyMedium,
                        )
                        Spacer(Modifier.height(8.dp))
                    }

                    institutionsState.data?.institutions?.forEach { institution ->
                        item {
                            InstitutionRow(institution, onSelectInstitution)
                        }
                    }
                }
            }
        }
    }
}

fun main() {
    val newArray = Arrays.stream(
        getCertificates(
            arrayOf("1", "2", "3")
        )
    ).filter { certificate -> // We really shouldn't expect any certificate here to NOT be a CA,
        // CAT shows a nice red warning when you try to configure this,
        // but experience shows that sometimes this is not enough of a deterrent.
        // We may very well block profiles like this, but then it should be done BEFORE
        // the user enters their username/password, not after.
        if (certificate == null) {
            false
        } else {
            certificate is String
        }
    }.collect(Collectors.toList())
    println("Result $newArray")
}

fun getCertificates(list: Array<String>): Array<String?> {
    val newList = arrayOfNulls<String>(list.size)
    for (i in list.indices) {
        if (i == 1) throw IllegalArgumentException("Fail now")
        else newList[i] = list[i]
    }
    return newList
}