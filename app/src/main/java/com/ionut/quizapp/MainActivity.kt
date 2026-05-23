package com.ionut.quizapp

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.ionut.quizapp.ui.LeaderboardScreen
import com.ionut.quizapp.ui.LearningQuizScreen
import com.ionut.quizapp.ui.LearningScreen
import com.ionut.quizapp.ui.theme.QuizAppTheme
import com.ionut.quizapp.ui.theme.QuizTheme
import com.ionut.quizapp.ui.LoginScreen
import com.ionut.quizapp.ui.SignUpScreen
import com.ionut.quizapp.ui.MainScreen
import com.ionut.quizapp.ui.ProfileScreen
import com.ionut.quizapp.ui.QuizScreen
import com.ionut.quizapp.ui.ResultScreen
import com.ionut.quizapp.viewmodels.LearningViewModel
import com.ionut.quizapp.viewmodels.MenuViewModel
import com.ionut.quizapp.viewmodels.QuizViewModel

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            val menuViewModel: MenuViewModel = viewModel()
            val quizViewModel: QuizViewModel = viewModel()
            val isUtm = menuViewModel.isUtmMode

            QuizAppTheme(isUtmMode = isUtm) {
                val navController = rememberNavController()

                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = QuizTheme.colors.background
                ) {
                    NavHost(
                        navController = navController,
                        startDestination = "login"
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
                                onBack = { navController.popBackStack() }
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
                                }
                            )
                        }

                        composable("learning_quiz") {
                            LearningQuizScreen(
                                viewModel = quizViewModel,
                                onExit = { navController.popBackStack() }
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