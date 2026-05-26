package com.example.ui.viewmodel

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.data.model.Candidate
import com.example.data.model.EvaluationResult
import com.example.data.repository.ResumeFilterRepository
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class ResumeFilterViewModel(private val repository: ResumeFilterRepository) : ViewModel() {

    // --- Database Streams ---
    val candidates: StateFlow<List<Candidate>> = repository.allCandidates
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    val evaluations: StateFlow<List<EvaluationResult>> = repository.allEvaluations
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    // --- UI State Variables ---
    var currentTab by mutableStateOf(1) // 0: Resumes Database, 1: Job Match Filter, 2: Analytics History

    // Recruiter Filter Criteria State
    var filterJobTitle by mutableStateOf("Android Developer")
    var filterRequiredSkills by mutableStateOf("Kotlin, Jetpack Compose, Room, Coroutines")
    var filterMinExperience by mutableStateOf(3.0)
    var filterAdditionalNotes by mutableStateOf("Knowledge of MVVM architecture and Retrofit REST parsing is a plus.")

    // Custom API Key fallback state (saved in memory)
    var customApiKey by mutableStateOf("")

    // Processing & Error Indicators
    var isAnalyzing by mutableStateOf(false)
    var analysisError by mutableStateOf<String?>(null)
    var analysisSuccessMessage by mutableStateOf<String?>(null)

    // Candidate search & detail selection flags
    var resumeSearchQuery by mutableStateOf("")
    var selectedCandidateId by mutableStateOf<Int?>(null)
    var isShowingAddCandidateDialog by mutableStateOf(false)

    // Temporary storage for Adding Custom Candidate
    var newCandidateName by mutableStateOf("")
    var newCandidateTitle by mutableStateOf("")
    var newCandidateEmail by mutableStateOf("")
    var newCandidatePhone by mutableStateOf("")
    var newCandidateExperience by mutableStateOf("2.0")
    var newCandidateSkills by mutableStateOf("")
    var newCandidateResume by mutableStateOf("")

    private val _uiEvents = MutableSharedFlow<String>()
    val uiEvents: SharedFlow<String> = _uiEvents.asSharedFlow()

    init {
        // Automatically pre-populate candidates if none are found in local DB
        viewModelScope.launch {
            candidates.collect { list ->
                if (list.isEmpty()) {
                    seedCandidateProfiles()
                }
            }
        }
    }

    private fun seedCandidateProfiles() {
        viewModelScope.launch {
            val hasCandidates = candidates.value.isNotEmpty()
            if (!hasCandidates) {
                val seeds = listOf(
                    Candidate(
                        name = "Sarah Jenkins",
                        title = "Senior Android Engineer",
                        email = "sarah.j@techmail.com",
                        phone = "555-019-2834",
                        experienceYears = 6.5,
                        skills = "Kotlin, Jetpack Compose, Room Database, Coroutines, Flow, MVVM, Retrofit, Dagger Hilt, Git",
                        resumeText = """
                            Sarah Jenkins
                            Mobile: 555-019-2834 | Email: sarah.j@techmail.com
                            
                            SUMMARY:
                            Resourceful and highly committed Senior Mobile Engineer with over 6 years of expertise building top-performing Android applications. Specializes in Jetpack Compose, state management using Flows, and robust local caching architectures.
                            
                            EXPERIENCE:
                            Lead Android Architect | PixelCraft Solutions (2022 - Present)
                            - Led modernization of high-traffic shopping app to 100% Jetpack Compose, reducing UI render latency by 35%.
                            - Overhauled database state persistence layer with Room, enabling seamless offline capabilities.
                            - Introduced structured MVVM architectures coupled with reactive coroutine flows.
                            
                            Android Software Engineer | AppForge Studio (2019 - 2022)
                            - Developed custom client widgets and integrated third-party RESTful integrations with Retrofit.
                            - Managed automated pipeline checks in GitLab CI and decreased memory leak rates by 15%.
                            
                            EDUCATION & CERTIFICATIONS:
                            B.S. in Computer Science | Oregon State University
                        """.trimIndent()
                    ),
                    Candidate(
                        name = "David Chen",
                        title = "Web Developer",
                        email = "david.chen@webstack.io",
                        phone = "555-043-9821",
                        experienceYears = 4.0,
                        skills = "JavaScript, TypeScript, React, Node.js, Express, MongoDB, REST APIs, Tailwind CSS, Git",
                        resumeText = """
                            David Chen
                            Email: david.chen@webstack.io | Phone: 555-043-9821
                            
                            OBJECTIVE:
                            Agile Full-stack Developer with four years of core engineering experience designing interactive, responsive React dashboards and fast Express backend databases.
                            
                            SKILLS & QUALIFICATIONS:
                            - React.js, Webpack, Redux Toolkit, HTML5, CSS3, Flexbox/Grids
                            - Node.js, Express, MongoDB, PostgreSQL, Mongoose
                            - Excellent team participant, Agile delivery methodology proponent.
                            
                            CORE DELIVERABLES:
                            - Built premium analytical CRM dashboard integrating customizable widgets using SVG graphs and REST feeds.
                            - Restructured backend query systems, boosting API delivery speeds by 2x.
                        """.trimIndent()
                    ),
                    Candidate(
                        name = "Maya Rodriguez",
                        title = "Native App Specialist",
                        email = "maya.rodriguez@codecraft.org",
                        phone = "555-076-1211",
                        experienceYears = 1.5,
                        skills = "Swift, SwiftUI, Kotlin, Android SDK, Git, UI/UX Design Models, Figma",
                        resumeText = """
                            Maya Rodriguez
                            Email: maya.rodriguez@codecraft.org | Phone: 555-076-1211
                            
                            BIO:
                            Motivated and eager Junior Mobile Creator with deep affinity for outstanding UI/UX layouts. Experienced in SwiftUI for iOS, actively learning and building Jetpack Compose projects for Android.
                            
                            PROJECTS & APPS:
                            - NoteQuest Application: Simple note-taking Android experiment utilizing room databases and custom text sliders.
                            - Budget-Pal App (iOS): Lightweight budgeting helper featuring custom interactive dials and native widgets.
                            
                            EDUCATION:
                            Associate Degree in Mobile Systems and Software Interfaces | Bay Area Tech Academy (2024)
                        """.trimIndent()
                    ),
                    Candidate(
                        name = "Robert Kim",
                        title = "Quality Assurance Engineer",
                        email = "robert.kim@testautomation.net",
                        phone = "555-081-3254",
                        experienceYears = 5.0,
                        skills = "Java, Selenium, Appium, Python, TestNG, Jenkins, CI/CD Pipeline controllers, JUnit, Git",
                        resumeText = """
                            Robert Kim
                            Email: robert.kim@testautomation.net | Phone: 555-081-3254
                            
                            PROFESSIONAL PROFILE:
                            Detail-oriented QA Automation Lead focused on constructing reliable automated regression suites. 5 years of background validating Web, Android, and iOS systems.
                            
                            KEY EXPERIENCES:
                            - Formulated Appium-based regression model covering the complete Android payment journey.
                            - Maintained CI/CD pipelines in Jenkins to trigger regression builds hourly instantly reporting leaks.
                        """.trimIndent()
                    ),
                    Candidate(
                        name = "Amina Al-Mansoor",
                        title = "Technical Project Manager",
                        email = "amina.am@leadcorp.com",
                        phone = "555-055-6712",
                        experienceYears = 8.0,
                        skills = "Agile, Scrum Master, Jira, Product Roadmap alignment, Budget planning, Stakeholder Reporting, Team leadership",
                        resumeText = """
                            Amina Al-Mansoor
                            Email: amina.am@leadcorp.com | Phone: 555-055-6712
                            
                            SUMMARY:
                            Empathetic Certified Scrum Master and Engineering Manager with 8 years of success directing Agile sprints and coordinating stakeholder delivery. Expert at planning timelines and reducing process blockages.
                            
                            KEY ACCOMPLISHMENTS:
                            - Led 2 cross-functional teams of up to 15 engineers to launch fintech service on-schedule and 5% under-budget.
                            - Successfully eliminated manual task sheets by fully transitioning departments to customizable Jira boards.
                        """.trimIndent()
                    )
                )

                seeds.forEach { repository.insertCandidate(it) }
            }
        }
    }

    // --- Candidate Actions ---

    fun addCustomCandidate() {
        val name = newCandidateName.trim()
        val title = newCandidateTitle.trim()
        val email = newCandidateEmail.trim()
        val phone = newCandidatePhone.trim()
        val skills = newCandidateSkills.trim()
        val resumeText = newCandidateResume.trim()
        val parsedExp = newCandidateExperience.toDoubleOrNull() ?: 0.0

        if (name.isEmpty() || title.isEmpty() || skills.isEmpty() || resumeText.isEmpty()) {
            viewModelScope.launch {
                _uiEvents.emit("Full Name, Job Title, Skills, and Resume fields are required.")
            }
            return
        }

        viewModelScope.launch {
            val cand = Candidate(
                name = name,
                title = title,
                email = email,
                phone = phone,
                experienceYears = parsedExp,
                skills = skills,
                resumeText = resumeText
            )
            repository.insertCandidate(cand)
            _uiEvents.emit("Candidate added successfully!")
            resetAddCandidateForm()
            isShowingAddCandidateDialog = false
        }
    }

    private fun resetAddCandidateForm() {
        newCandidateName = ""
        newCandidateTitle = ""
        newCandidateEmail = ""
        newCandidatePhone = ""
        newCandidateExperience = "2.0"
        newCandidateSkills = ""
        newCandidateResume = ""
    }

    fun deleteCandidate(candidate: Candidate) {
        viewModelScope.launch {
            repository.deleteCandidate(candidate)
            _uiEvents.emit("Deleted resume for ${candidate.name}")
        }
    }

    // --- Action triggering Gemini parsing and filter ---

    fun runAutomaticResumeAnalysis() = viewModelScope.launch {
        val jobTitle = filterJobTitle.trim()
        val skillReqs = filterRequiredSkills.trim()
        val notes = filterAdditionalNotes.trim()
        val activeCandList = candidates.value

        if (jobTitle.isEmpty() || skillReqs.isEmpty()) {
            analysisError = "Position Title and Required Skills must not be empty."
            return@launch
        }

        if (activeCandList.isEmpty()) {
            analysisError = "No candidates found in database. Add candidates or wait for pre-seed to complete."
            return@launch
        }

        isAnalyzing = true
        analysisError = null
        analysisSuccessMessage = null

        try {
            // First clear past evaluations of this job title to avoid cluttering saved matches
            repository.clearEvaluationsByJob(jobTitle)

            // API Call through repo
            val results = repository.filterCandidatesWithGemini(
                candidates = activeCandList,
                jobTitle = jobTitle,
                requiredSkills = skillReqs,
                minExperience = filterMinExperience,
                additionalNotes = notes,
                customApiKey = customApiKey.trim().takeIf { it.isNotEmpty() }
            )

            if (results.isEmpty()) {
                analysisError = "No evaluation matches were returned from the analysis"
            } else {
                analysisSuccessMessage = "Successfully verified and ranked ${results.size} candidates with Gemini AI."
                currentTab = 2 // Transition directly to matched overview tab to see visual feedback
            }
        } catch (e: Exception) {
            e.printStackTrace()
            analysisError = e.localizedMessage ?: "Unknown error occurred while contacting the Gemini AI engine."
        } finally {
            isAnalyzing = false
        }
    }

    fun clearHistory() {
        viewModelScope.launch {
            repository.clearAllEvaluations()
            _uiEvents.emit("Cleared all matching analysis history.")
        }
    }
}

class ResumeFilterViewModelFactory(private val repository: ResumeFilterRepository) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(ResumeFilterViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return ResumeFilterViewModel(repository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
