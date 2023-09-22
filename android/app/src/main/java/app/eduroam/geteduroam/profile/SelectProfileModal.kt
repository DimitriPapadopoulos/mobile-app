package app.eduroam.geteduroam.profile

import android.content.res.Configuration
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.sizeIn
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Divider
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.flowWithLifecycle
import app.eduroam.geteduroam.R
import app.eduroam.geteduroam.config.model.EAPIdentityProviderList
import app.eduroam.geteduroam.institutions.TermsOfUseDialog
import app.eduroam.geteduroam.institutions.UsernamePasswordDialog
import app.eduroam.geteduroam.models.Profile
import app.eduroam.geteduroam.ui.AlertDialogWithSingleButton
import app.eduroam.geteduroam.ui.ErrorData
import app.eduroam.geteduroam.ui.PrimaryButton
import app.eduroam.geteduroam.ui.theme.AppTheme
import kotlinx.coroutines.android.awaitFrame
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filter

@Composable
fun SelectProfileModal(
    viewModel: SelectProfileViewModel,
    goToOAuth: (String, String) -> Unit = { _, _ -> },
    goToConfigScreen: (EAPIdentityProviderList) -> Unit = { _ -> },
) {
    val lifecycle = LocalLifecycleOwner.current.lifecycle
    val currentGotoOauth by rememberUpdatedState(newValue = goToOAuth)

    LaunchedEffect(viewModel, lifecycle) {
        snapshotFlow { viewModel.uiState }.distinctUntilChanged()
            .filter { it.promptForOAuth != null }.flowWithLifecycle(lifecycle).collect { state ->
                awaitFrame()
                val profile = state.profiles.first { it.isSelected }.profile
                currentGotoOauth(
                    profile.authorizationEndpoint.orEmpty(), profile.tokenEndpoint.orEmpty()
                )
            }
    }

    LaunchedEffect(viewModel, lifecycle) {
        snapshotFlow { viewModel.uiState }.distinctUntilChanged()
            .filter { it.goToConfigScreenWithProviderList != null }.flowWithLifecycle(lifecycle).collect { state ->
                val providerList = state.goToConfigScreenWithProviderList!!
                goToConfigScreen(providerList)
            }
    }

    SelectProfileContent(
        profiles = viewModel.uiState.profiles,
        institution = viewModel.uiState.institution,
        inProgress = viewModel.uiState.inProgress,
        errorData = viewModel.uiState.errorData,
        errorDataShown = viewModel::errorDataShown,
        setProfileSelected = viewModel::setProfileSelected,
        connectWithSelectedProfile = viewModel::connectWithSelectedProfile
    )

    if (viewModel.uiState.showTermsOfUseDialog) {
        TermsOfUseDialog(onConfirmClicked = {
            viewModel.didAgreeToTerms(true)
        }, onDismiss = {
            viewModel.didAgreeToTerms(false)
        })
    }
    if (viewModel.uiState.showUsernameDialog) {
        UsernamePasswordDialog(
            requiredSuffix = viewModel.uiState.institution?.requiredSuffix,
            cancel = viewModel::didCancelLogin, // Just hide the dialog
            logIn = { username, password ->
                viewModel.didEnterLoginDetails(username = username, password = password)
            }

        )
    }
}

@Composable
fun SelectProfileContent(
    profiles: List<PresentProfile>,
    institution: PresentInstitution? = null,
    inProgress: Boolean = false,
    errorData: ErrorData? = null,
    errorDataShown: () -> Unit = {},
    setProfileSelected: (PresentProfile) -> Unit = {},
    connectWithSelectedProfile: () -> Unit = {}
) = Surface(
    modifier = Modifier
        .heightIn(min = 300.dp, max = 500.dp)
        .systemBarsPadding(),
    color = MaterialTheme.colorScheme.surface.copy(alpha = 0.9f)
) {
    val context = LocalContext.current
    errorData?.let {
        AlertDialogWithSingleButton(
            title = it.title(context),
            explanation = it.message(context),
            buttonLabel = stringResource(id = R.string.button_ok),
            onDismiss = errorDataShown
        )
    }

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.SpaceBetween,
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp, vertical = 24.dp)
    ) {
        Column(
            modifier = Modifier.fillMaxSize()
        ) {
            institution?.name?.let {
                Text(
                    text = it,
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.fillMaxWidth()
                )
            }
            institution?.location?.let {
                Text(
                    text = it,
                    style = MaterialTheme.typography.titleSmall,
                    modifier = Modifier.fillMaxWidth()
                )
            }
            Spacer(Modifier.height(16.dp))
            Text(
                text = stringResource(R.string.profiles_title),
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(Modifier.height(2.dp))
            Divider(
                modifier = Modifier.fillMaxWidth(),
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.12f)
            )
            Spacer(Modifier.height(4.dp))

            if (inProgress) {
                Spacer(Modifier.height(4.dp))
                LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
                Spacer(Modifier.height(4.dp))
            }
            profiles.forEach { profile ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .sizeIn(minHeight = 48.dp)
                        .clickable(onClick = {
                            setProfileSelected(profile)
                        }), verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = profile.profile.name,
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.fillMaxWidth(0.9f)
                    )
                    if (profile.isSelected) {
                        Icon(
                            imageVector = Icons.Outlined.Check,
                            contentDescription = "",
                            modifier = Modifier.size(ButtonDefaults.IconSize)
                        )
                    }
                }
                Divider(
                    modifier = Modifier.fillMaxWidth(),
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.12f)
                )
            }
        }
        PrimaryButton(
            text = stringResource(R.string.button_connect),
            enabled = !inProgress,
            onClick = { connectWithSelectedProfile() },
            modifier = Modifier.fillMaxWidth(),
        )
    }
}


@Preview(uiMode = Configuration.UI_MODE_NIGHT_NO)
@Composable
private fun Preview_SelectProfileModal() {
    AppTheme {
        SelectProfileContent(
            profiles = profileList,
            institution = PresentInstitution("Uninett", "NO")
        )
    }
}

private val profileList = listOf(
    PresentProfile(Profile(id = "id", name = "First profile"), true),
    PresentProfile(Profile(id = "id", name = "Second profile"), false),
)