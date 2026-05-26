package com.example.ui

import android.widget.Toast
import androidx.compose.animation.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.BuildConfig
import com.example.data.model.Candidate
import com.example.data.model.EvaluationResult
import com.example.ui.viewmodel.ResumeFilterViewModel
import kotlinx.coroutines.delay

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ResumeFilterApp(
    viewModel: ResumeFilterViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val candidates by viewModel.candidates.collectAsStateWithLifecycle()
    val evaluations by viewModel.evaluations.collectAsStateWithLifecycle()

    // Screen-level notification flow
    LaunchedEffect(key1 = true) {
        viewModel.uiEvents.collect { event ->
            Toast.makeText(context, event, Toast.LENGTH_LONG).show()
        }
    }

    // Dynamic scanning loading message rotation to make it feel super alive!
    var loadingMessage by remember { mutableStateOf("Contacting Gemini Engine...") }
    LaunchedEffect(viewModel.isAnalyzing) {
        if (viewModel.isAnalyzing) {
            val messages = listOf(
                "Scanning candidate database...",
                "Reading Sarah Jenkins' resume details...",
                "Extracting technical parameters...",
                "Comparing skills with target requirements...",
                "Scoring experience milestones...",
                "Gemini is evaluating top matches...",
                "Compiling structured recruiters output..."
            )
            var index = 0
            while (viewModel.isAnalyzing) {
                loadingMessage = messages[index % messages.size]
                delay(3500)
                index++
            }
        }
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(
                                    Brush.linearGradient(
                                        colors = listOf(
                                            MaterialTheme.colorScheme.primary,
                                            MaterialTheme.colorScheme.tertiary
                                        )
                                    )
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Settings,
                                contentDescription = "AI Scanner Logo",
                                tint = MaterialTheme.colorScheme.onPrimary,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                        Column {
                            Text(
                                text = "Resume Filter AI",
                                fontWeight = FontWeight.Bold,
                                fontSize = 18.sp
                            )
                            Text(
                                text = "Powered by Gemini 3.5 Flash",
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.outline
                            )
                        }
                    }
                },
                actions = {
                    // Quick clear results if on matched tab
                    if (viewModel.currentTab == 2 && evaluations.isNotEmpty()) {
                        IconButton(
                            onClick = { viewModel.clearHistory() },
                            modifier = Modifier.minimumInteractiveComponentSize()
                        ) {
                            Icon(
                                imageVector = Icons.Default.Refresh,
                                contentDescription = "Reset filter"
                            )
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
                )
            )
        },
        bottomBar = {
            NavigationBar(
                windowInsets = WindowInsets.navigationBars
            ) {
                NavigationBarItem(
                    selected = viewModel.currentTab == 0,
                    onClick = { viewModel.currentTab = 0 },
                    icon = { Icon(Icons.Default.Person, contentDescription = "Candidates list button") },
                    label = { Text("Database") }
                )
                NavigationBarItem(
                    selected = viewModel.currentTab == 1,
                    onClick = { viewModel.currentTab = 1 },
                    icon = { Icon(Icons.Default.Settings, contentDescription = "Criteria filter options button") },
                    label = { Text("Parser Engine") }
                )
                NavigationBarItem(
                    selected = viewModel.currentTab == 2,
                    onClick = { viewModel.currentTab = 2 },
                    icon = { Icon(Icons.Default.Star, contentDescription = "Ranked matching results button") },
                    label = { Text("Rankings") }
                )
            }
        },
        floatingActionButton = {
            if (viewModel.currentTab == 0) {
                FloatingActionButton(
                    onClick = { viewModel.isShowingAddCandidateDialog = true },
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                    modifier = Modifier.testTag("add_candidate_fab")
                ) {
                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = "Add Candidate Resume"
                    )
                }
            }
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                // Main UI Tab Content Switcher
                AnimatedContent(
                    targetState = viewModel.currentTab,
                    transitionSpec = {
                        fadeIn() togetherWith fadeOut()
                    },
                    modifier = Modifier.weight(1f),
                    label = "TabContentTransition"
                ) { tab ->
                    when (tab) {
                        0 -> CandidatesTab(
                            candidates = candidates,
                            searchQuery = viewModel.resumeSearchQuery,
                            onSearchQueryChanged = { viewModel.resumeSearchQuery = it },
                            onDeleteCandidate = { viewModel.deleteCandidate(it) },
                            onSelectCandidate = { viewModel.selectedCandidateId = it.id }
                        )
                        1 -> FilterEngineTab(
                            viewModel = viewModel
                        )
                        2 -> RankingsTab(
                            evaluations = evaluations,
                            candidates = candidates,
                            onSelectCandidate = { viewModel.selectedCandidateId = it.id }
                        )
                    }
                }
            }

            // --- Custom Popups / Overlays ---

            // 1. Loading Scanner Sheet
            if (viewModel.isAnalyzing) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.Black.copy(alpha = 0.6f))
                        .clickable(enabled = false) {}, // Intercept clicks
                    contentAlignment = Alignment.Center
                ) {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth(0.85f)
                            .padding(16.dp),
                        shape = RoundedCornerShape(16.dp),
                        elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(24.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            Text(
                                text = "Running AI Candidate Filter",
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold,
                                textAlign = TextAlign.Center
                            )
                            
                            CircularProgressIndicator(
                                modifier = Modifier.size(56.dp),
                                strokeWidth = 5.dp,
                                color = MaterialTheme.colorScheme.primary
                            )

                            Text(
                                text = loadingMessage,
                                style = MaterialTheme.typography.bodyMedium,
                                textAlign = TextAlign.Center,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.animateContentSize()
                            )

                            LinearProgressIndicator(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(4.dp)
                                    .clip(CircleShape),
                                color = MaterialTheme.colorScheme.secondary
                            )
                        }
                    }
                }
            }

            // 2. Candidate Detail View Overlaid Dialog
            val activeCandidateId = viewModel.selectedCandidateId
            if (activeCandidateId != null) {
                val selectedCandidate = candidates.find { it.id == activeCandidateId }
                if (selectedCandidate != null) {
                    val matchingEvaluation = evaluations.find { it.candidateId == selectedCandidate.id }
                    CandidateDetailDialog(
                        candidate = selectedCandidate,
                        evaluation = matchingEvaluation,
                        onDismiss = { viewModel.selectedCandidateId = null }
                    )
                }
            }

            // 3. Add Candidate Dialog Form
            if (viewModel.isShowingAddCandidateDialog) {
                AddCandidateDialog(
                    viewModel = viewModel,
                    onDismiss = { viewModel.isShowingAddCandidateDialog = false }
                )
            }
        }
    }
}

// ==========================================
// TAB 0: CANDIDATES DATABASE SCREEN
// ==========================================

@Composable
fun CandidatesTab(
    candidates: List<Candidate>,
    searchQuery: String,
    onSearchQueryChanged: (String) -> Unit,
    onDeleteCandidate: (Candidate) -> Unit,
    onSelectCandidate: (Candidate) -> Unit
) {
    val filteredCandidates = remember(candidates, searchQuery) {
        if (searchQuery.isBlank()) {
            candidates
        } else {
            val query = searchQuery.lowercase().trim()
            candidates.filter {
                it.name.lowercase().contains(query) ||
                it.title.lowercase().contains(query) ||
                it.skills.lowercase().contains(query) ||
                it.resumeText.lowercase().contains(query)
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        // Search Entry
        OutlinedTextField(
            value = searchQuery,
            onValueChange = onSearchQueryChanged,
            placeholder = { Text("Search candidates, resumes, skills...") },
            leadingIcon = { Icon(Icons.Default.Search, contentDescription = "Search Icon") },
            trailingIcon = if (searchQuery.isNotEmpty()) {
                {
                    IconButton(
                        onClick = { onSearchQueryChanged("") },
                        modifier = Modifier.minimumInteractiveComponentSize()
                    ) {
                        Icon(Icons.Default.Close, contentDescription = "Clear search query")
                    }
                }
            } else null,
            modifier = Modifier
                .fillMaxWidth()
                .testTag("resume_search")
                .padding(bottom = 12.dp),
            singleLine = true,
            colors = OutlinedTextFieldDefaults.colors(
                focusedContainerColor = MaterialTheme.colorScheme.surface,
                unfocusedContainerColor = MaterialTheme.colorScheme.surface
            )
        )

        Text(
            text = "Candidates Library (${filteredCandidates.size})",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(bottom = 8.dp)
        )

        if (filteredCandidates.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Search,
                        contentDescription = "Empty Resumes Icon",
                        modifier = Modifier.size(64.dp),
                        tint = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f)
                    )
                    Text(
                        text = if (searchQuery.isNotEmpty()) "No resumes match your search filter." else "No resumes added yet.",
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = "Click the bottom-right FAB button (+) to import a resume.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.outline,
                        textAlign = TextAlign.Center
                    )
                }
            }
        } else {
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                items(filteredCandidates, key = { it.id }) { candidate ->
                    CandidateCard(
                        candidate = candidate,
                        onClick = { onSelectCandidate(candidate) },
                        onDelete = { onDeleteCandidate(candidate) }
                    )
                }
            }
        }
    }
}

@Composable
fun CandidateCard(
    candidate: Candidate,
    onClick: () -> Unit,
    onDelete: () -> Unit
) {
    Card(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.25f)
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Left profile initials icon representation
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primaryContainer),
                contentAlignment = Alignment.Center
            ) {
                val initials = candidate.name.split(" ").mapNotNull { it.firstOrNull() }.take(2).joinToString("")
                Text(
                    text = initials.uppercase(),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = candidate.name,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = candidate.title,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.secondary
                )
                
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(top = 4.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Info,
                        contentDescription = "Experience badge",
                        modifier = Modifier.size(14.dp),
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "${candidate.experienceYears} Years Exp",
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                // Render first few skills as badges
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 6.dp),
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    candidate.skills.split(",").take(3).forEach { skill ->
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(6.dp))
                                .background(MaterialTheme.colorScheme.secondaryContainer)
                                .padding(horizontal = 6.dp, vertical = 2.dp)
                        ) {
                            Text(
                                text = skill.trim(),
                                style = MaterialTheme.typography.labelSmall,
                                fontSize = 10.sp,
                                color = MaterialTheme.colorScheme.onSecondaryContainer
                            )
                        }
                    }
                    if (candidate.skills.split(",").size > 3) {
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(6.dp))
                                .background(MaterialTheme.colorScheme.surfaceVariant)
                                .padding(horizontal = 6.dp, vertical = 2.dp)
                        ) {
                            Text(
                                text = "+${candidate.skills.split(",").size - 3}",
                                style = MaterialTheme.typography.labelSmall,
                                fontSize = 10.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }

            // Action: Delete custom candidate
            IconButton(
                onClick = onDelete,
                modifier = Modifier.minimumInteractiveComponentSize()
            ) {
                Icon(
                    imageVector = Icons.Default.Delete,
                    contentDescription = "Delete Resume",
                    tint = MaterialTheme.colorScheme.error.copy(alpha = 0.7f)
                )
            }
        }
    }
}

// ==========================================
// TAB 1: AI PARSER CONFIGURATION SCREEN
// ==========================================

@Composable
fun FilterEngineTab(
    viewModel: ResumeFilterViewModel
) {
    val scrollState = rememberScrollState()
    val candidates by viewModel.candidates.collectAsStateWithLifecycle()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Card(
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.15f)
            )
        ) {
            Row(
                modifier = Modifier.padding(14.dp),
                verticalAlignment = Alignment.Top,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Info,
                    contentDescription = "Intro Info",
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(24.dp)
                )
                Column {
                    Text(
                        text = "Specify Candidate Requirements",
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.titleMedium
                    )
                    Text(
                        text = "The Gemini model will parse all ${candidates.size} loaded resumes. It scores, ranks, and provides detailed HR analytics compared to your criteria.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        // 1. Position Title Entry
        OutlinedTextField(
            value = viewModel.filterJobTitle,
            onValueChange = { viewModel.filterJobTitle = it },
            label = { Text("Job Position Title") },
            placeholder = { Text("e.g. Senior Android Engineer") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next)
        )

        // 2. Desired Skills
        OutlinedTextField(
            value = viewModel.filterRequiredSkills,
            onValueChange = { viewModel.filterRequiredSkills = it },
            label = { Text("Required/Key Technical Skills") },
            placeholder = { Text("e.g. Kotlin, Compose, Room, REST APIs") },
            modifier = Modifier.fillMaxWidth(),
            supportingText = { Text("Provide skills separated by commas") },
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next)
        )

        // 3. Minimum Years Exp
        Column(modifier = Modifier.fillMaxWidth()) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Minimum Experience Required",
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = "${viewModel.filterMinExperience.toInt()} Years",
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary,
                    style = MaterialTheme.typography.titleMedium
                )
            }
            Slider(
                value = viewModel.filterMinExperience.toFloat(),
                onValueChange = { viewModel.filterMinExperience = it.toDouble() },
                valueRange = 0f..10f,
                steps = 9,
                modifier = Modifier.fillMaxWidth()
            )
        }

        // 4. Instructions / Hiring Notes
        OutlinedTextField(
            value = viewModel.filterAdditionalNotes,
            onValueChange = { viewModel.filterAdditionalNotes = it },
            label = { Text("Secondary Recruiter Directives (Optional)") },
            placeholder = { Text("e.g. Must know architecture or Git, team leadership qualities preferred...") },
            modifier = Modifier.fillMaxWidth(),
            maxLines = 3,
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done)
        )

        // 5. Custom API Key panel if environment has none configured
        val isDefaultKey = BuildConfig.GEMINI_API_KEY.isBlank() || BuildConfig.GEMINI_API_KEY == "MY_GEMINI_API_KEY"
        if (isDefaultKey) {
            Card(
                shape = RoundedCornerShape(12.dp),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.5f)),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            ) {
                Column(
                    modifier = Modifier.padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Warning,
                            contentDescription = "Warning",
                            tint = MaterialTheme.colorScheme.error
                        )
                        Text(
                            text = "Platform API Key Required",
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.error
                        )
                    }
                    Text(
                        text = "To analyze candidates, please paste your Gemini API key from AI Studio secrets console below.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    OutlinedTextField(
                        value = viewModel.customApiKey,
                        onValueChange = { viewModel.customApiKey = it },
                        label = { Text("Personal Gemini API Key") },
                        placeholder = { Text("AIzaSy...") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password)
                    )
                }
            }
        }

        // Error message if any
        viewModel.analysisError?.let { err ->
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(12.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Warning,
                        contentDescription = "Error icon",
                        tint = MaterialTheme.colorScheme.onErrorContainer
                    )
                    Text(
                        text = err,
                        color = MaterialTheme.colorScheme.onErrorContainer,
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }
        }

        // Action Buttons
        Button(
            onClick = { viewModel.runAutomaticResumeAnalysis() },
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp)
                .testTag("run_ai_filter"),
            colors = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary
            )
        ) {
            Icon(
                imageVector = Icons.Default.Settings,
                contentDescription = "Filter icon"
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = "Run AI Candidate Filter",
                fontWeight = FontWeight.Bold,
                fontSize = 15.sp
            )
        }
    }
}

// ==========================================
// TAB 2: RANKINGS & MATCHES SCREEN
// ==========================================

@Composable
fun RankingsTab(
    evaluations: List<EvaluationResult>,
    candidates: List<Candidate>,
    onSelectCandidate: (Candidate) -> Unit
) {
    if (evaluations.isEmpty()) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Star,
                    contentDescription = "No Matches Stars",
                    modifier = Modifier.size(64.dp),
                    tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.3f)
                )
                Text(
                    text = "No AI matching performed yet.",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = "Go to 'Parser Engine' tab, write target requirements, and click 'Run AI Filter'.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.outline,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(horizontal = 24.dp)
                )
            }
        }
        return
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        // Summary Header details
        val matchedJob = evaluations.firstOrNull()?.jobTitle ?: "Position"
        val minExp = evaluations.firstOrNull()?.minExperience ?: 0.0
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 12.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.4f)
            )
        ) {
            Column(modifier = Modifier.padding(14.dp)) {
                Text(
                    text = "Match Rankings: $matchedJob",
                    fontWeight = FontWeight.Bold,
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSecondaryContainer
                )
                Text(
                    text = "Minimum target experience: ${minExp.toInt()} Years. Active ranked candidates: ${evaluations.size}.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        LazyColumn(
            verticalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            items(evaluations, key = { it.id }) { evaluation ->
                val cand = candidates.find { it.id == evaluation.candidateId }
                if (cand != null) {
                    RankingCard(
                        evaluation = evaluation,
                        candidate = cand,
                        onClick = { onSelectCandidate(cand) }
                    )
                }
            }
        }
    }
}

@Composable
fun RankingCard(
    evaluation: EvaluationResult,
    candidate: Candidate,
    onClick: () -> Unit
) {
    // Pick color scheme based on match percentage
    val ringColor = when {
        evaluation.score >= 80 -> Color(0xFF4CAF50) // Strong Match
        evaluation.score >= 60 -> Color(0xFFFF9800) // Moderate Match
        else -> Color(0xFFE53935) // Weak Match
    }

    Card(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Left score circle rim indicator
            Box(
                modifier = Modifier
                    .size(52.dp)
                    .border(3.dp, ringColor, CircleShape)
                    .padding(4.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "${evaluation.score}%",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.ExtraBold,
                    color = ringColor
                )
            }

            Spacer(modifier = Modifier.width(14.dp))

            Column(modifier = Modifier.weight(1f)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = candidate.name,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    
                    // Match level tag
                    val badgeLabel = when {
                        evaluation.score >= 80 -> "Top Recommended"
                        evaluation.score >= 60 -> "Potential Fit"
                        else -> "Low Priority"
                    }
                    val badgeBg = when {
                        evaluation.score >= 80 -> Color(0xFFE8F5E9)
                        evaluation.score >= 60 -> Color(0xFFFFF3E0)
                        else -> Color(0xFFFFEBEE)
                    }
                    val badgeFg = when {
                        evaluation.score >= 80 -> Color(0xFF2E7D32)
                        evaluation.score >= 60 -> Color(0xFFEF6C00)
                        else -> Color(0xFFC62828)
                    }
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(4.dp))
                            .background(badgeBg)
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = badgeLabel,
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold,
                            color = badgeFg
                        )
                    }
                }

                Text(
                    text = candidate.title,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.secondary,
                    modifier = Modifier.padding(bottom = 4.dp)
                )

                Text(
                    text = evaluation.summary,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )

                // Match details summary
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    val splitMatches = evaluation.matchedSkills.split(",").filter { it.isNotBlank() }
                    if (splitMatches.isNotEmpty()) {
                        Text(
                            text = "Matches: ${splitMatches.take(3).joinToString(", ")}",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color(0xFF2E7D32),
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
            }
        }
    }
}

// ==========================================
// SUB COMPONENT: CANDIDATE DETAIL OVERLAY DIALOG
// ==========================================

@Composable
fun CandidateDetailDialog(
    candidate: Candidate,
    evaluation: EvaluationResult?,
    onDismiss: () -> Unit
) {
    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.85f),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                // Header Area
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(MaterialTheme.colorScheme.primaryContainer)
                        .padding(16.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = candidate.name,
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                            Text(
                                text = candidate.title,
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.secondary
                            )
                        }
                        IconButton(
                            onClick = onDismiss,
                            modifier = Modifier.minimumInteractiveComponentSize()
                        ) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "Close details modal",
                                tint = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        }
                    }
                }

                // Scrollable Content
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .verticalScroll(rememberScrollState())
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    // Contact Info Area
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        if (candidate.email.isNotBlank()) {
                            Card(
                                modifier = Modifier.weight(1f),
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(0.4f))
                            ) {
                                Column(modifier = Modifier.padding(10.dp)) {
                                    Text("Email", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.outline)
                                    Text(candidate.email, style = MaterialTheme.typography.bodyMedium, textDecoration = null, overflow = TextOverflow.Ellipsis, maxLines = 1)
                                }
                            }
                        }
                        if (candidate.phone.isNotBlank()) {
                            Card(
                                modifier = Modifier.weight(1f),
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(0.4f))
                            ) {
                                Column(modifier = Modifier.padding(10.dp)) {
                                    Text("Phone", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.outline)
                                    Text(candidate.phone, style = MaterialTheme.typography.bodyMedium, overflow = TextOverflow.Ellipsis, maxLines = 1)
                                }
                            }
                        }
                    }

                    // Score summary if matches performed
                    if (evaluation != null) {
                        val colorRim = when {
                            evaluation.score >= 80 -> Color(0xFF4CAF50)
                            evaluation.score >= 60 -> Color(0xFFFF9800)
                            else -> Color(0xFFE53935)
                        }
                        Card(
                            shape = RoundedCornerShape(12.dp),
                            border = BorderStroke(1.5.dp, colorRim),
                            colors = CardDefaults.cardColors(containerColor = colorRim.copy(alpha = 0.05f))
                        ) {
                            Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(44.dp)
                                            .border(2.5.dp, colorRim, CircleShape)
                                            .padding(4.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text("${evaluation.score}%", fontWeight = FontWeight.Bold, color = colorRim)
                                    }
                                    Column {
                                        Text("AI HR Evaluation Profile", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodyLarge)
                                        Text("Position: ${evaluation.jobTitle}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.outline)
                                    }
                                }

                                Text(
                                    text = evaluation.summary,
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.Medium
                                )

                                Divider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))

                                // Exp analysis
                                Text("Experience Match Analysis:", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.primary)
                                Text(evaluation.experienceAnalysis, style = MaterialTheme.typography.bodyMedium)

                                // Dynamic matching vs missing lists
                                val matches = evaluation.matchedSkills.split(",").filter { it.isNotBlank() }
                                val missing = evaluation.missingSkills.split(",").filter { it.isNotBlank() }

                                if (matches.isNotEmpty()) {
                                    Text("Matched Skills:", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodySmall, color = Color(0xFF2E7D32))
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                                    ) {
                                        matches.forEach { skill ->
                                            Box(
                                                modifier = Modifier
                                                    .clip(RoundedCornerShape(6.dp))
                                                    .background(Color(0xFFE8F5E9))
                                                    .padding(horizontal = 8.dp, vertical = 4.dp)
                                            ) {
                                                Text(skill.trim(), fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color(0xFF2E7D32))
                                            }
                                        }
                                    }
                                }

                                if (missing.isNotEmpty()) {
                                    Text("Missing Requirements:", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodySmall, color = Color(0xFFC62828))
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                                    ) {
                                        missing.forEach { skill ->
                                            Box(
                                                modifier = Modifier
                                                    .clip(RoundedCornerShape(6.dp))
                                                    .background(Color(0xFFFFEBEE))
                                                    .padding(horizontal = 8.dp, vertical = 4.dp)
                                            ) {
                                                Text(skill.trim(), fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color(0xFFC62828))
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }

                    // Complete list of skills tags
                    Text(
                        text = "Candidate Declared Skills",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        candidate.skills.split(",").forEach { skill ->
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(MaterialTheme.colorScheme.secondaryContainer)
                                    .padding(horizontal = 8.dp, vertical = 4.dp)
                            ) {
                                Text(
                                    text = skill.trim(),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSecondaryContainer
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(4.dp))

                    // Raw Resume Content Panel
                    Text(
                        text = "Full Resume Copy",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(0.2f)),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.2f)),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text(
                            text = candidate.resumeText,
                            style = MaterialTheme.typography.bodySmall,
                            fontSize = 11.sp,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp)
                        )
                    }
                }
            }
        }
    }
}

// ==========================================
// SUB COMPONENT: ADD CANDIDATE DIALOG FORM
// ==========================================

@Composable
fun AddCandidateDialog(
    viewModel: ResumeFilterViewModel,
    onDismiss: () -> Unit
) {
    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.9f),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                // Header Area
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(MaterialTheme.colorScheme.primaryContainer)
                        .padding(16.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = "Import Candidate Resume",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                            Text(
                                text = "Enter candidate credentials & resume copy",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onPrimaryContainer.copy(0.8f)
                            )
                        }
                        IconButton(
                            onClick = onDismiss,
                            modifier = Modifier.minimumInteractiveComponentSize()
                        ) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "Close modal",
                                tint = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        }
                    }
                }

                // Inputs
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .verticalScroll(rememberScrollState())
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    OutlinedTextField(
                        value = viewModel.newCandidateName,
                        onValueChange = { viewModel.newCandidateName = it },
                        label = { Text("Candidate Full Name *") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )

                    OutlinedTextField(
                        value = viewModel.newCandidateTitle,
                        onValueChange = { viewModel.newCandidateTitle = it },
                        label = { Text("Profile/Job Title *") },
                        placeholder = { Text("e.g. Senior iOS Engineer") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedTextField(
                            value = viewModel.newCandidateEmail,
                            onValueChange = { viewModel.newCandidateEmail = it },
                            label = { Text("Email") },
                            modifier = Modifier.weight(1f),
                            singleLine = true
                        )
                        OutlinedTextField(
                            value = viewModel.newCandidatePhone,
                            onValueChange = { viewModel.newCandidatePhone = it },
                            label = { Text("Phone") },
                            modifier = Modifier.weight(1f),
                            singleLine = true
                        )
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        OutlinedTextField(
                            value = viewModel.newCandidateExperience,
                            onValueChange = { viewModel.newCandidateExperience = it },
                            label = { Text("Experience (Years) *") },
                            modifier = Modifier.weight(1.2f),
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                        )
                    }

                    OutlinedTextField(
                        value = viewModel.newCandidateSkills,
                        onValueChange = { viewModel.newCandidateSkills = it },
                        label = { Text("Key Technical Skills (Comma Separated) *") },
                        placeholder = { Text("e.g. Swift, iOS, Xcode, CoreData, CI/CD") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )

                    OutlinedTextField(
                        value = viewModel.newCandidateResume,
                        onValueChange = { viewModel.newCandidateResume = it },
                        label = { Text("Complete Text of Resume / Bio *") },
                        placeholder = { Text("Paste entire resume copy containing experience, deliverables, profile details and background history...") },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(160.dp),
                        maxLines = 15
                    )
                }

                // Action area
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    OutlinedButton(
                        onClick = onDismiss,
                        modifier = Modifier
                            .weight(1f)
                            .height(44.dp)
                    ) {
                        Text("Cancel")
                    }

                    Button(
                        onClick = { viewModel.addCustomCandidate() },
                        modifier = Modifier
                            .weight(1.5f)
                            .testTag("add_candidate_submit")
                            .height(44.dp)
                    ) {
                        Text("Add Candidate")
                    }
                }
            }
        }
    }
}
