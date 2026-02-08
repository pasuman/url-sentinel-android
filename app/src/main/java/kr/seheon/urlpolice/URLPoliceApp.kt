package kr.seheon.urlpolice

import android.net.Uri
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.ui.window.Dialog
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
    val alwaysShowResults by browserPreferences.alwaysShowResults.collectAsState(initial = false)
    val hasSeenWelcome by browserPreferences.hasSeenWelcome.collectAsState(initial = false)

    var validationState by remember { mutableStateOf<ValidationState>(ValidationState.Idle) }
    var showSettings by remember { mutableStateOf(false) }

    LaunchedEffect(interceptedUrl) {
        if (interceptedUrl != null) {
            validationState = ValidationState.Validating
            val result = validationService.validateUrl(interceptedUrl)
            validationState = ValidationState.Validated(result)

            // Auto-open safe URLs if conditions are met
            if (result.isSafe && !alwaysShowResults && defaultBrowser != null) {
                onOpenInBrowser(interceptedUrl, defaultBrowser)
            }
        } else {
            validationState = ValidationState.Idle
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("URL 파수꾼") },
                actions = {
                    IconButton(onClick = { showSettings = !showSettings }) {
                        Icon(
                            imageVector = Icons.Default.Settings,
                            contentDescription = "설정"
                        )
                    }
                }
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
            // Show welcome screen on first launch
            if (!hasSeenWelcome) {
                FirstLaunchWelcomeScreen(
                    installedBrowsers = installedBrowsers,
                    selectedBrowser = defaultBrowser,
                    alwaysShowResults = alwaysShowResults,
                    onBrowserSelected = { browser ->
                        scope.launch {
                            browserPreferences.setDefaultBrowser(browser?.packageName)
                        }
                    },
                    onAlwaysShowResultsChanged = { enabled ->
                        scope.launch {
                            browserPreferences.setAlwaysShowResults(enabled)
                        }
                    },
                    onComplete = {
                        scope.launch {
                            browserPreferences.setHasSeenWelcome(true)
                        }
                    }
                )
                return@Scaffold
            }

            // No URL - show home screen
            if (interceptedUrl == null) {
                HomeScreen(
                    installedBrowsers = installedBrowsers,
                    selectedBrowser = defaultBrowser,
                    alwaysShowResults = alwaysShowResults,
                    showSettings = showSettings,
                    onBrowserSelected = { browser ->
                        scope.launch {
                            browserPreferences.setDefaultBrowser(browser?.packageName)
                        }
                    },
                    onAlwaysShowResultsChanged = { enabled ->
                        scope.launch {
                            browserPreferences.setAlwaysShowResults(enabled)
                        }
                    },
                    onSettingsDismiss = { showSettings = false }
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
                    // Safe URL that was auto-opened - don't show UI
                    if (state.result.isSafe && !alwaysShowResults && defaultBrowser != null) {
                        // URL was already opened, just show a brief message or dismiss
                        return@Scaffold
                    }

                    // Show validation results for unsafe URLs or when user wants to see results
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
private fun FirstLaunchWelcomeScreen(
    installedBrowsers: List<Browser>,
    selectedBrowser: Browser?,
    alwaysShowResults: Boolean,
    onBrowserSelected: (Browser?) -> Unit,
    onAlwaysShowResultsChanged: (Boolean) -> Unit,
    onComplete: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "URL 파수꾼에 오신 것을 환영합니다",
            style = MaterialTheme.typography.headlineMedium,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(URLPoliceSpacing.sectionGap))

        Card(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(URLPoliceSpacing.cardPadding)) {
                Text(
                    text = "URL 파수꾼 사용 방법",
                    style = MaterialTheme.typography.titleMedium
                )

                Spacer(modifier = Modifier.height(URLPoliceSpacing.elementGap))

                InstructionStep(
                    number = "1",
                    title = "기본 브라우저로 설정",
                    description = "설정 → 앱 → 기본 앱 → 브라우저로 이동하여 URL 파수꾼을 선택하세요"
                )

                Spacer(modifier = Modifier.height(URLPoliceSpacing.elementGap))

                InstructionStep(
                    number = "2",
                    title = "브라우저 선택",
                    description = "링크를 열 브라우저를 선택하세요"
                )

                Spacer(modifier = Modifier.height(URLPoliceSpacing.elementGap))

                InstructionStep(
                    number = "3",
                    title = "표시 설정",
                    description = "모든 URL의 검증 결과를 볼지, 위험한 URL만 볼지 선택하세요"
                )
            }
        }

        Spacer(modifier = Modifier.height(URLPoliceSpacing.sectionGap))

        Card(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(URLPoliceSpacing.cardPadding)) {
                Text(
                    text = "선호하는 브라우저 선택",
                    style = MaterialTheme.typography.titleMedium
                )

                Spacer(modifier = Modifier.height(URLPoliceSpacing.elementGap))

                installedBrowsers.forEach { browser ->
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

        Spacer(modifier = Modifier.height(URLPoliceSpacing.sectionGap))

        Card(modifier = Modifier.fillMaxWidth()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(URLPoliceSpacing.cardPadding),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "항상 검증 결과 표시",
                        style = MaterialTheme.typography.bodyLarge
                    )
                    Text(
                        text = "안전한 URL을 포함한 모든 URL의 결과 표시",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Switch(
                    checked = alwaysShowResults,
                    onCheckedChange = onAlwaysShowResultsChanged
                )
            }
        }

        Spacer(modifier = Modifier.height(URLPoliceSpacing.sectionGap))

        Button(
            onClick = onComplete,
            modifier = Modifier.fillMaxWidth(),
            enabled = selectedBrowser != null
        ) {
            Text("Get Started")
        }
    }
}

@Composable
private fun InstructionStep(
    number: String,
    title: String,
    description: String
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.Top
    ) {
        Surface(
            modifier = Modifier.size(32.dp),
            shape = MaterialTheme.shapes.small,
            color = MaterialTheme.colorScheme.primaryContainer
        ) {
            Box(contentAlignment = Alignment.Center) {
                Text(
                    text = number,
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                )
            }
        }

        Spacer(modifier = Modifier.width(URLPoliceSpacing.elementGap))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = androidx.compose.ui.text.font.FontWeight.Medium
            )
            Text(
                text = description,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun HomeScreen(
    installedBrowsers: List<Browser>,
    selectedBrowser: Browser?,
    alwaysShowResults: Boolean,
    showSettings: Boolean,
    onBrowserSelected: (Browser?) -> Unit,
    onAlwaysShowResultsChanged: (Boolean) -> Unit,
    onSettingsDismiss: () -> Unit
) {
    if (showSettings) {
        SettingsDialog(
            alwaysShowResults = alwaysShowResults,
            onAlwaysShowResultsChanged = onAlwaysShowResultsChanged,
            onDismiss = onSettingsDismiss
        )
    }

    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "URL Sentinel is active",
            style = MaterialTheme.typography.headlineSmall,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(URLPoliceSpacing.elementGap))

        Text(
            text = "All URL clicks will be checked for security threats",
            style = MaterialTheme.typography.bodyMedium,
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Spacer(modifier = Modifier.height(URLPoliceSpacing.sectionGap))

        DefaultBrowserSelector(
            browsers = installedBrowsers,
            selectedBrowser = selectedBrowser,
            onBrowserSelected = onBrowserSelected
        )

        Spacer(modifier = Modifier.height(URLPoliceSpacing.sectionGap))

        Card(modifier = Modifier.fillMaxWidth()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(URLPoliceSpacing.cardPadding),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "항상 검증 결과 표시",
                        style = MaterialTheme.typography.bodyLarge
                    )
                    Text(
                        text = "Safe URLs will open directly when disabled",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Switch(
                    checked = alwaysShowResults,
                    onCheckedChange = onAlwaysShowResultsChanged
                )
            }
        }
    }
}

@Composable
private fun SettingsDialog(
    alwaysShowResults: Boolean,
    onAlwaysShowResultsChanged: (Boolean) -> Unit,
    onDismiss: () -> Unit
) {
    Dialog(onDismissRequest = onDismiss) {
        Card {
            Column(modifier = Modifier.padding(URLPoliceSpacing.cardPadding)) {
                Text(
                    text = "Settings",
                    style = MaterialTheme.typography.titleLarge
                )

                Spacer(modifier = Modifier.height(URLPoliceSpacing.sectionGap))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "항상 검증 결과 표시",
                            style = MaterialTheme.typography.bodyLarge
                        )
                        Spacer(modifier = Modifier.height(URLPoliceSpacing.smallGap))
                        Text(
                            text = "안전한 URL을 포함한 모든 URL의 결과 표시",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Switch(
                        checked = alwaysShowResults,
                        onCheckedChange = onAlwaysShowResultsChanged
                    )
                }

                Spacer(modifier = Modifier.height(URLPoliceSpacing.sectionGap))

                Button(
                    onClick = onDismiss,
                    modifier = Modifier.align(Alignment.End)
                ) {
                    Text("Close")
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

