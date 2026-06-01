package com.ionut.quizapp

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Surface
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.ionut.quizapp.auth.AuthViewModel
import com.ionut.quizapp.data.SupabaseClient
import com.ionut.quizapp.features.home.MainScreen
import com.ionut.quizapp.features.learning.ui.LearningQuizScreen
import com.ionut.quizapp.features.learning.ui.LearningScreen
import com.ionut.quizapp.features.core.theme.QuizAppTheme
import com.ionut.quizapp.features.core.theme.QuizTheme
import com.ionut.quizapp.features.profile.*
import com.ionut.quizapp.features.quiz.QuizGeneratorScreen
import com.ionut.quizapp.features.quiz.QuizScreen
import com.ionut.quizapp.features.quiz.ResultScreen
import com.ionut.quizapp.viewmodels.LearningViewModel
import com.ionut.quizapp.viewmodels.MenuViewModel
import com.ionut.quizapp.viewmodels.QuizViewModel
import com.ionut.quizapp.viewmodels.SoundManager
import io.github.jan.supabase.gotrue.auth

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val soundManager = SoundManager(applicationContext)

        setContent {
            // ViewModel-uri instanțiate o singură dată la nivel de activitate
            val menuViewModel: MenuViewModel = viewModel()
            val quizViewModel: QuizViewModel = viewModel()
            val authViewModel: AuthViewModel = viewModel()

            val isUtm = menuViewModel.isUtmMode
            var isAuthLoaded by remember { mutableStateOf(false) }
            var startDestination by remember { mutableStateOf("login") }

            // Logica inițială Supabase
            LaunchedEffect(Unit) {
                SupabaseClient.client.auth.awaitInitialization()
                val user = SupabaseClient.client.auth.currentUserOrNull()
                startDestination = if (user != null) "main_menu" else "login"
                isAuthLoaded = true
            }

            // Ecran de încărcare cât timp Supabase verifică sesiunea
            if (!isAuthLoaded) {
                Surface(modifier = Modifier.fillMaxSize()) {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator()
                    }
                }
                return@setContent
            }

            QuizAppTheme(isUtmMode = isUtm) {
                val navController = rememberNavController()

                // Gestie Muzică Fundal
                val lifecycleOwner = LocalLifecycleOwner.current
                DisposableEffect(lifecycleOwner) {
                    val observer = LifecycleEventObserver { _, event ->
                        when (event) {
                            Lifecycle.Event.ON_RESUME -> soundManager.playBGM()
                            Lifecycle.Event.ON_PAUSE -> soundManager.pauseBGM()
                            else -> {}
                        }
                    }
                    lifecycleOwner.lifecycle.addObserver(observer)
                    onDispose {
                        lifecycleOwner.lifecycle.removeObserver(observer)
                        soundManager.releaseAll()
                    }
                }

                Surface(modifier = Modifier.fillMaxSize(), color = QuizTheme.colors.background) {
                    NavHost(navController = navController, startDestination = startDestination) {

                        composable("login") {
                            LoginScreen(
                                onLoginSuccess = { navController.navigate("main_menu") { popUpTo("login") { inclusive = true } } },
                                onSignUpClick = { navController.navigate("signup") },
                                onGuestLogin = { navController.navigate("main_menu") { popUpTo("login") { inclusive = true } } }
                            )
                        }

                        composable("signup") {
                            SignUpScreen(
                                onSignUpSuccess = { navController.navigate("login") { popUpTo("signup") { inclusive = true } } },
                                onBackToLogin = { navController.popBackStack() }
                            )
                        }

                        composable("main_menu") {
                            MainScreen(
                                menuViewModel = menuViewModel,
                                quizViewModel = quizViewModel,
                                authViewModel = authViewModel,
                                soundManager = soundManager,
                                onNavigateToProfile = { navController.navigate("profile") },
                                onNavigateToLeaderboard = { navController.navigate("leaderboard") },
                                onNavigateToCustomQuiz = { navController.navigate("quiz_generator") },
                                onNavigateToSettings = { navController.navigate("settings") },
                                onNavigateToLogin = { navController.navigate("login") { popUpTo("main_menu") { inclusive = true } } },
                                onStartQuiz = { mode, categories, isUtmFlag, count ->
                                    if (mode == "Learning") {
                                        navController.navigate("learning_selection")
                                    } else {
                                        val cats = categories.joinToString(",")
                                        navController.navigate("quiz/$mode/$cats/$isUtmFlag/$count")
                                    }
                                }
                            )
                        }

                        composable("profile") {
                            ProfileScreen(
                                authViewModel = authViewModel,
                                quizViewModel = quizViewModel,
                                menuViewModel = menuViewModel,
                                onLogoutSuccess = { navController.navigate("login") { popUpTo("main_menu") { inclusive = true } } },
                                onBack = { navController.popBackStack() },
                                onNavigateToGame = { navController.navigate("learning_quiz") }
                            )
                        }

                        composable("settings") {
                            SettingsScreen(
                                authViewModel = authViewModel,
                                soundManager = soundManager,
                                onBack = { navController.popBackStack() },
                                onNavigateToLogin = { navController.navigate("login") { popUpTo("main_menu") { inclusive = true } } }
                            )
                        }

                        composable("learning_selection") {
                            LearningScreen(
                                learningViewModel = viewModel(),
                                quizViewModel = quizViewModel,
                                authViewModel = authViewModel,
                                isUtm = menuViewModel.isUtmMode,
                                onBack = { navController.popBackStack() },
                                onDifficultySelect = { cat, diff ->
                                    quizViewModel.loadLearningQuestions(cat, diff, menuViewModel.isUtmMode)
                                    navController.navigate("learning_quiz")
                                },
                                onLoginClick = { navController.navigate("login") { popUpTo("main_menu") { inclusive = true } } }
                            )
                        }

                        composable("learning_quiz") {
                            LearningQuizScreen(
                                quizViewModel = quizViewModel,
                                authViewModel = authViewModel,
                                soundManager = soundManager,
                                onExit = { navController.popBackStack() }
                            )
                        }

                        composable("quiz_generator") {
                            QuizGeneratorScreen(
                                viewModel = quizViewModel,
                                soundManager = soundManager,
                                onBack = { navController.popBackStack() },
                                onNavigateToGame = { navController.navigate("learning_quiz") }
                            )
                        }

                        composable(
                            route = "quiz/{mode}/{categories}/{isUtm}/{count}",
                            arguments = listOf(
                                navArgument("mode") { type = NavType.StringType },
                                navArgument("categories") { type = NavType.StringType },
                                navArgument("isUtm") { type = NavType.BoolType },
                                navArgument("count") { type = NavType.IntType }
                            )
                        ) { back ->
                            QuizScreen(
                                viewModel = quizViewModel,
                                soundManager = soundManager,
                                mode = back.arguments?.getString("mode") ?: "Classic",
                                isUtm = back.arguments?.getBoolean("isUtm") ?: false,
                                categories = back.arguments?.getString("categories") ?: "",
                                count = back.arguments?.getInt("count") ?: 10,
                                onFinish = { navController.navigate("results") { popUpTo("quiz/{mode}/{categories}/{isUtm}/{count}") { inclusive = true } } },
                                onExit = { navController.navigate("main_menu") { popUpTo("main_menu") { inclusive = true } } }
                            )
                        }

                        composable("results") {
                            ResultScreen(
                                viewModel = quizViewModel,
                                soundManager = soundManager,
                                onNavigateBack = { navController.navigate("main_menu") { popUpTo("main_menu") { inclusive = true } } }
                            )
                        }

                        composable("leaderboard") {
                            LeaderboardScreen(
                                quizViewModel = quizViewModel,
                                menuViewModel = menuViewModel,
                                onBack = { navController.popBackStack() }
                            )
                        }
                    }
                }
            }
        }
    }
}