package com.ionut.quizapp

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Surface
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
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
import com.ionut.quizapp.ui.LeaderboardScreen
import com.ionut.quizapp.ui.LearningQuizScreen
import com.ionut.quizapp.ui.LearningScreen
import com.ionut.quizapp.ui.theme.QuizAppTheme
import com.ionut.quizapp.ui.theme.QuizTheme
import com.ionut.quizapp.ui.LoginScreen
import com.ionut.quizapp.ui.SignUpScreen
import com.ionut.quizapp.ui.MainScreen
import com.ionut.quizapp.ui.ProfileScreen
import com.ionut.quizapp.ui.QuizGeneratorScreen
import com.ionut.quizapp.ui.QuizScreen
import com.ionut.quizapp.ui.ResultScreen
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
            val menuViewModel: MenuViewModel = viewModel()
            val quizViewModel: QuizViewModel = viewModel()
            val authViewModel: AuthViewModel = viewModel()
            val isUtm = menuViewModel.isUtmMode

            var isAuthLoaded by remember { mutableStateOf(false) }
            var startDestination by remember { mutableStateOf("login") }

            LaunchedEffect(Unit) {
                // Așteptăm ca Supabase să termine de verificat memoria telefonului
                SupabaseClient.client.auth.awaitInitialization()
                val user = SupabaseClient.client.auth.currentUserOrNull()

                // Decidem încotro o ia aplicația
                if (user != null) {
                    startDestination = "main_menu"
                } else {
                    startDestination = "login"
                }
                isAuthLoaded = true // Dăm drumul la aplicație!
            }

            // Cât timp Supabase citește, arătăm un ecran de încărcare gol (durează cam 0.1s)
            if (!isAuthLoaded) {
                Surface(modifier = Modifier.fillMaxSize()) {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator() // Rotița de încărcare
                    }
                }
                return@setContent // Oprește desenarea restului până e gata
            }

            QuizAppTheme(isUtmMode = isUtm) {
                val navController = rememberNavController()

                // === MUZICA DE FUNDAL CONTINUĂ ===
                val lifecycleOwner = LocalLifecycleOwner.current
                DisposableEffect(lifecycleOwner) {
                    val observer = LifecycleEventObserver { _, event ->
                        if (event == Lifecycle.Event.ON_RESUME) {
                            soundManager.playBGM() // Joacă muzica când aplicația e pe ecran
                        } else if (event == Lifecycle.Event.ON_PAUSE) {
                            soundManager.pauseBGM() // Pauză dacă ieși din aplicație
                        }
                    }
                    lifecycleOwner.lifecycle.addObserver(observer)
                    onDispose {
                        lifecycleOwner.lifecycle.removeObserver(observer)
                        soundManager.releaseAll() // Curățăm RAM-ul la finalizarea totală
                    }
                }

                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = QuizTheme.colors.background
                ) {
                    NavHost(
                        navController = navController,
                        startDestination = startDestination
                    ) {
                        composable("login") {
                            LoginScreen(
                                onLoginSuccess = {
                                    navController.navigate("main_menu") {
                                        popUpTo("login") { inclusive = true }
                                    }
                                },
                                onSignUpClick = {
                                    navController.navigate("signup")
                                },
                                onGuestLogin = {
                                    navController.navigate("main_menu") {
                                        popUpTo("login") { inclusive = true }
                                    }
                                }
                            )
                        }

                        composable("signup") {
                            SignUpScreen(
                                onSignUpSuccess = {
                                    navController.navigate("login")
                                },
                                onBackToLogin = {
                                    navController.popBackStack()
                                }
                            )
                        }

                        composable("main_menu") {
                            MainScreen(
                                menuViewModel = menuViewModel,
                                onNavigateToProfile = { navController.navigate("profile") },
                                onNavigateToLeaderboard = { navController.navigate("leaderboard") },
                                onNavigateToCustomQuiz = { navController.navigate("quiz_generator") },
                                onNavigateToLogin = { // Navigarea nouă
                                    navController.navigate("login") {
                                        popUpTo("main_menu") { inclusive = true }
                                    }
                                },
                                onStartQuiz = { mode, categories, isUtmFlag, count ->
                                    if (mode == "Learning") {
                                        // Navigăm către Hub-ul de Learning (cel cu carduri și progrese)
                                        navController.navigate("learning_selection")
                                    } else {
                                        val categoriesJson = categories.joinToString(",")
                                        navController.navigate("quiz/$mode/$categoriesJson/$isUtmFlag/$count")
                                    }
                                }
                            )
                        }

                        composable("profile") {
                            ProfileScreen(
                                authViewModel = viewModel(),
                                quizViewModel = quizViewModel,
                                menuViewModel = menuViewModel,
                                onLogoutSuccess = {
                                    navController.navigate("login") {
                                        popUpTo("main_menu") { inclusive = true }
                                    }
                                },
                                onBack = { navController.popBackStack() },
                                onNavigateToGame = {
                                    // Trimitem utilizatorul direct în ecranul de studiu/learning!
                                    navController.navigate("learning_quiz")
                                }
                            )
                        }

                        composable("learning_selection") {
                            val learningViewModel: LearningViewModel = viewModel()

                            LearningScreen(
                                learningViewModel = learningViewModel,
                                quizViewModel = quizViewModel,
                                isUtm = menuViewModel.isUtmMode,
                                onBack = { navController.popBackStack() },
                                onDifficultySelect = { category, difficulty ->
                                    quizViewModel.loadLearningQuestions(category, difficulty, menuViewModel.isUtmMode)
                                    navController.navigate("learning_quiz")
                                },
                                onLoginClick = {
                                    // Când apasă pe LOG IN din banner, îl trimitem la ecranul de login
                                    // și curățăm stiva ca să nu dea back înapoi în zona de learning ca guest
                                    navController.navigate("login") {
                                        popUpTo("main_menu") { inclusive = true }
                                    }
                                }
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
                                onBack = { navController.popBackStack() },
                                onNavigateToGame = {
                                    // Navigăm direct către ecranul de Learning!
                                    navController.navigate("learning_quiz")
                                }
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
                        ) { backStackEntry ->
                            val mode = backStackEntry.arguments?.getString("mode") ?: "Classic"
                            val categories = backStackEntry.arguments?.getString("categories") ?: ""
                            val isUtmFlag = backStackEntry.arguments?.getBoolean("isUtm") ?: false
                            val count = backStackEntry.arguments?.getInt("count") ?: 10

                            QuizScreen(
                                viewModel = quizViewModel, // Pasăm ViewModel-ul partajat
                                soundManager = soundManager,
                                mode = mode,
                                isUtm = isUtmFlag,
                                categories = categories,
                                count = count,
                                onFinish = {
                                    // Navigăm la rezultate și curățăm stiva de quiz
                                    navController.navigate("results")
                                },
                                onExit = {
                                    // Te trimite înapoi la meniu și curăță stiva
                                    navController.navigate("main_menu") {
                                        popUpTo("main_menu") { inclusive = true }
                                    }
                                }
                            )
                        }

                        composable("results") {
                            ResultScreen(
                                viewModel = quizViewModel,
                                soundManager = soundManager,
                                onNavigateBack = {
                                    navController.navigate("main_menu") {
                                        popUpTo("main_menu") { inclusive = true }
                                    }
                                }
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