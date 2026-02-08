package kr.seheon.urlpolice

import android.net.Uri
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.core.graphics.drawable.toBitmap
import kotlinx.coroutines.launch

private object URLPoliceColors {
    val safe = Color(0xFF4CAF50)
    val danger = Color(0xFFF44336)
    val warning = Color(0xFFFF9800)
    val secure = Color(0xFF4CAF50)
    val insecure = Color(0xFFFF9800)
}

private sealed class ValidationState {
    data object Idle : ValidationState()
    data object Validating : ValidationState()
    data class Validated(val result: URLValidationResult) : ValidationState()
}

private object URLPoliceSpacing {
    val screenPadding: Dp = 16.dp
    val sectionGap: Dp = 24.dp
    val elementGap: Dp = 8.dp
    val smallGap: Dp = 4.dp
    val cardPadding: Dp = 16.dp
    val dividerVerticalPadding: Dp = 12.dp
}

private object URLPoliceSizing {
    val browserIconSize: Dp = 40.dp
    val buttonIconSize: Dp = 24.dp
    val validationIconSize: Dp = 32.dp
    val cornerRadius: Dp = 8.dp
    val smallCornerRadius: Dp = 4.dp
    val browserItemPadding: Dp = 12.dp
    val browserIconSpacing: Dp = 16.dp
}

private const val URL_MAX_LINES = 4
private const val HTTPS_SCHEME = "https"

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun URLPoliceApp(
    interceptedUrl: Uri?,
    onDismiss: () -> Unit,
    onOpenInBrowser: (Uri, Browser?) -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val browserManager = remember { BrowserManager(context) }
    val browserPreferences = remember { BrowserPreferences(context) }
    val validationService = remember { URLValidationService() }
    val installedBrowsers = remember { browserManager.getInstalledBrowsers() }

    val defaultBrowserPackage by browserPreferences.defaultBrowserPackage.collectAsState(initial = null)
    val defaultBrowser = installedBrowsers.find { it.packageName == defaultBrowserPackage }

    var validationState by remember { mutableStateOf<ValidationState>(ValidationState.Idle) }

    LaunchedEffect(interceptedUrl) {
        if (interceptedUrl != null) {
            validationState = ValidationState.Validating
            val result = validationService.validateUrl(interceptedUrl)
            validationState = ValidationState.Validated(result)
        } else {
            validationState = ValidationState.Idle
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("URL Police") }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(URLPoliceSpacing.screenPadding),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            if (interceptedUrl == null) {
                WelcomeView(
                    installedBrowsers = installedBrowsers,
                    selectedBrowser = defaultBrowser,
                    onBrowserSelected = { browser ->
                        scope.launch {
                            browserPreferences.setDefaultBrowser(browser?.packageName)
                        }
                    }
                )
                return@Scaffold
            }

            when (val state = validationState) {
                is ValidationState.Idle -> {
                    // Should not happen when interceptedUrl is not null
                }
                is ValidationState.Validating -> {
                    ValidationLoadingView(url = interceptedUrl)
                }
                is ValidationState.Validated -> {
                    ValidationResultView(
                        url = interceptedUrl,
                        result = state.result,
                        defaultBrowser = defaultBrowser,
                        onOpenInBrowser = { onOpenInBrowser(interceptedUrl, defaultBrowser) },
                        onSelectDefaultBrowser = onDismiss,
                        onDismiss = onDismiss
                    )
                }
            }
        }
    }
}

@Composable
private fun ValidationLoadingView(url: Uri) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        URLCard(url = url)

        Spacer(modifier = Modifier.height(URLPoliceSpacing.sectionGap))

        Card(modifier = Modifier.fillMaxWidth()) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(URLPoliceSpacing.cardPadding),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                CircularProgressIndicator()

                Spacer(modifier = Modifier.height(URLPoliceSpacing.screenPadding))

                Text(
                    text = "Analyzing URL...",
                    style = MaterialTheme.typography.titleMedium
                )

                Spacer(modifier = Modifier.height(URLPoliceSpacing.elementGap))

                Text(
                    text = "Checking for phishing, malware, and other threats",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center
                )
            }
        }
    }
}

@Composable
private fun ValidationResultView(
    url: Uri,
    result: URLValidationResult,
    defaultBrowser: Browser?,
    onOpenInBrowser: () -> Unit,
    onSelectDefaultBrowser: () -> Unit,
    onDismiss: () -> Unit
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        URLCard(url = url)

        Spacer(modifier = Modifier.height(URLPoliceSpacing.screenPadding))

        ValidationResultCard(result = result)

        Spacer(modifier = Modifier.height(URLPoliceSpacing.sectionGap))

        OpenInBrowserButton(
            defaultBrowser = defaultBrowser,
            isSafe = result.isSafe,
            onOpenClick = onOpenInBrowser,
            onSelectDefaultClick = onSelectDefaultBrowser
        )

        Spacer(modifier = Modifier.height(URLPoliceSpacing.elementGap))

        OutlinedButton(
            onClick = onDismiss,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Dismiss")
        }
    }
}

@Composable
private fun ValidationResultCard(result: URLValidationResult) {
    val isSafe = result.isSafe
    val cardColor = if (isSafe) {
        MaterialTheme.colorScheme.primaryContainer
    } else {
        MaterialTheme.colorScheme.errorContainer
    }
    val contentColor = if (isSafe) {
        MaterialTheme.colorScheme.onPrimaryContainer
    } else {
        MaterialTheme.colorScheme.onErrorContainer
    }
    val icon = if (isSafe) Icons.Default.Check else Icons.Default.Warning
    val title = if (isSafe) "Safe" else getThreatTitle(result.threatType)

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = cardColor)
    ) {
        Column(modifier = Modifier.padding(URLPoliceSpacing.cardPadding)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = contentColor,
                    modifier = Modifier.size(URLPoliceSizing.validationIconSize)
                )
                Spacer(modifier = Modifier.width(URLPoliceSpacing.elementGap))
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleLarge,
                    color = contentColor
                )
            }

            Spacer(modifier = Modifier.height(URLPoliceSpacing.elementGap))

            Text(
                text = result.message,
                style = MaterialTheme.typography.bodyMedium,
                color = contentColor
            )

            Spacer(modifier = Modifier.height(URLPoliceSpacing.elementGap))

            val confidencePercent = (result.confidence * 100).toInt()
            Text(
                text = "Confidence: $confidencePercent%",
                style = MaterialTheme.typography.labelSmall,
                color = contentColor.copy(alpha = 0.7f)
            )
        }
    }
}

private fun getThreatTitle(threatType: ThreatType?): String {
    return when (threatType) {
        ThreatType.PHISHING -> "Phishing Detected"
        ThreatType.MALWARE -> "Malware Detected"
        ThreatType.SCAM -> "Scam Detected"
        ThreatType.SUSPICIOUS -> "Suspicious URL"
        null -> "Unsafe"
    }
}

@Composable
private fun OpenInBrowserButton(
    defaultBrowser: Browser?,
    isSafe: Boolean = true,
    onOpenClick: () -> Unit,
    onSelectDefaultClick: () -> Unit
) {
    val hasDefaultBrowser = defaultBrowser != null

    if (hasDefaultBrowser) {
        val buttonColors = if (isSafe) {
            ButtonDefaults.buttonColors()
        } else {
            ButtonDefaults.buttonColors(
                containerColor = URLPoliceColors.danger
            )
        }
        val buttonText = if (isSafe) {
            "Open in ${defaultBrowser?.name}"
        } else {
            "Open anyway in ${defaultBrowser?.name}"
        }

        Button(
            onClick = onOpenClick,
            modifier = Modifier.fillMaxWidth(),
            colors = buttonColors
        ) {
            if (defaultBrowser?.icon != null) {
                Image(
                    bitmap = defaultBrowser.icon.toBitmap().asImageBitmap(),
                    contentDescription = null,
                    modifier = Modifier
                        .size(URLPoliceSizing.buttonIconSize)
                        .clip(RoundedCornerShape(URLPoliceSizing.smallCornerRadius))
                )
                Spacer(modifier = Modifier.width(URLPoliceSpacing.elementGap))
            }
            Text(buttonText)
        }
    } else {
        OutlinedButton(
            onClick = onSelectDefaultClick,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Select default browser")
        }
    }
}

@Composable
fun URLCard(url: Uri) {
    val isSecure = url.scheme == HTTPS_SCHEME
    val statusColor = if (isSecure) URLPoliceColors.secure else URLPoliceColors.insecure
    val statusIcon = if (isSecure) Icons.Default.Lock else Icons.Default.Warning
    val statusText = if (isSecure) "Secure Connection" else "Insecure Connection"

    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(URLPoliceSpacing.cardPadding)) {
            SecurityStatusRow(
                icon = statusIcon,
                text = statusText,
                color = statusColor
            )

            HorizontalDivider(
                modifier = Modifier.padding(vertical = URLPoliceSpacing.dividerVerticalPadding)
            )

            LabeledText(label = "Host", text = url.host ?: "Unknown")

            Spacer(modifier = Modifier.height(URLPoliceSpacing.dividerVerticalPadding))

            LabeledText(
                label = "Full URL",
                text = url.toString(),
                textStyle = MaterialTheme.typography.bodySmall,
                fontFamily = FontFamily.Monospace,
                maxLines = URL_MAX_LINES
            )
        }
    }
}

@Composable
private fun SecurityStatusRow(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    text: String,
    color: Color
) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = color
        )
        Spacer(modifier = Modifier.width(URLPoliceSpacing.elementGap))
        Text(
            text = text,
            color = color,
            style = MaterialTheme.typography.labelMedium
        )
    }
}

@Composable
private fun LabeledText(
    label: String,
    text: String,
    textStyle: androidx.compose.ui.text.TextStyle = MaterialTheme.typography.titleMedium,
    fontFamily: FontFamily? = null,
    maxLines: Int = Int.MAX_VALUE
) {
    Text(
        text = label,
        style = MaterialTheme.typography.labelSmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant
    )
    Text(
        text = text,
        style = textStyle,
        fontFamily = fontFamily,
        maxLines = maxLines
    )
}

@Composable
private fun WelcomeView(
    installedBrowsers: List<Browser>,
    selectedBrowser: Browser?,
    onBrowserSelected: (Browser?) -> Unit
) {
    DefaultBrowserSelector(
        browsers = installedBrowsers,
        selectedBrowser = selectedBrowser,
        onBrowserSelected = onBrowserSelected
    )
}

@Composable
private fun DefaultBrowserSelector(
    browsers: List<Browser>,
    selectedBrowser: Browser?,
    onBrowserSelected: (Browser?) -> Unit
) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(URLPoliceSpacing.cardPadding)) {
            Text(
                text = "Default Browser",
                style = MaterialTheme.typography.titleMedium
            )

            Spacer(modifier = Modifier.height(URLPoliceSpacing.elementGap))

            Text(
                text = "Select a browser to open links directly without confirmation.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(URLPoliceSpacing.screenPadding))

            browsers.forEach { browser ->
                val isSelected = browser.packageName == selectedBrowser?.packageName

                BrowserSelectionItem(
                    browser = browser,
                    isSelected = isSelected,
                    onClick = {
                        val newSelection = if (isSelected) null else browser
                        onBrowserSelected(newSelection)
                    }
                )
            }
        }
    }
}

@Composable
private fun BrowserSelectionItem(
    browser: Browser,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    val cornerShape = RoundedCornerShape(URLPoliceSizing.cornerRadius)
    val containerColor = if (isSelected) {
        MaterialTheme.colorScheme.primaryContainer
    } else {
        MaterialTheme.colorScheme.surface
    }

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = URLPoliceSpacing.smallGap)
            .clickable(onClick = onClick),
        shape = cornerShape,
        color = containerColor
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(URLPoliceSizing.browserItemPadding),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (browser.icon != null) {
                Image(
                    bitmap = browser.icon.toBitmap().asImageBitmap(),
                    contentDescription = browser.name,
                    modifier = Modifier
                        .size(URLPoliceSizing.browserIconSize)
                        .clip(cornerShape)
                )
                Spacer(modifier = Modifier.width(URLPoliceSizing.browserIconSpacing))
            }

            Text(
                text = browser.name,
                style = MaterialTheme.typography.bodyLarge,
                modifier = Modifier.weight(1f)
            )

            if (isSelected) {
                Icon(
                    imageVector = Icons.Default.Check,
                    contentDescription = "Selected",
                    tint = MaterialTheme.colorScheme.primary
                )
            }
        }
    }
}

