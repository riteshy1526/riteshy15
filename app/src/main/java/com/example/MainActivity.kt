package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import com.example.data.db.AppDatabase
import com.example.data.repository.ResumeFilterRepository
import com.example.ui.ResumeFilterApp
import com.example.ui.theme.MyApplicationTheme
import com.example.ui.viewmodel.ResumeFilterViewModel
import com.example.ui.viewmodel.ResumeFilterViewModelFactory

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // Initialize local database and repositories
        val database = AppDatabase.getDatabase(this)
        val repository = ResumeFilterRepository(
            candidateDao = database.candidateDao(),
            evaluationResultDao = database.evaluationResultDao()
        )
        val factory = ResumeFilterViewModelFactory(repository)

        setContent {
            MyApplicationTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    val viewModel: ResumeFilterViewModel by viewModels { factory }
                    ResumeFilterApp(viewModel = viewModel)
                }
            }
        }
    }
}
