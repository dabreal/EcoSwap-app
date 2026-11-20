package com.example.ecoswap

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.example.ecoswap.ui.theme.EcoSwapTheme
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        super.onCreate(savedInstanceState)

        //crear BD y repositorio
        val db = EcoSwapDatabase.getInstance(applicationContext)
        val repository = ProductRepository(db.productDao())
        EcoSwapNotifications.ensureChannel(this)

        val homeVmFactory = HomeViewModelFactory(application, repository)

        //inicializar productos de ejemplo la primera vez, siempre que la Base de datos esté vacía, lo hacemos para tener pruebas
        lifecycleScope.launch {
            val allProducts = db.productDao().getAllProducts().firstOrNull()
            if (allProducts?.isEmpty() == true) {
                repository.initializeSampleProducts()
            }
        }

        enableEdgeToEdge()

        setContent {
            EcoSwapTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    val navController = rememberNavController()

                    //obtener HomeViewModel con factory
                    val homeViewModel: HomeViewModel = viewModel(factory = homeVmFactory)

                    NavHost(
                        navController = navController,
                        startDestination = NavRoutes.home()
                    ) {
                        composable(
                            route = NavRoutes.HOME_ARGS,
                            arguments = listOf(
                                navArgument("tab") {
                                    type = NavType.IntType
                                    defaultValue = 0
                                },
                                navArgument("focusId") {
                                    type = NavType.LongType
                                    defaultValue = -1L
                                }
                            )
                        ) { backStackEntry ->
                            val tab = backStackEntry.arguments?.getInt("tab") ?: 0
                            val focusIdRaw = backStackEntry.arguments?.getLong("focusId") ?: -1L
                            val focusId = focusIdRaw.takeIf { it >= 0 }

                            HomeScreen(
                                homeViewModel = homeViewModel,
                                startTab = tab,
                                focusProductId = focusId,
                                onProductClick = { id ->
                                    navController.navigate(NavRoutes.detail(id))
                                }
                            )
                        }
                        composable(NavRoutes.DETAIL) { backStackEntry ->
                            val id = backStackEntry.arguments
                                ?.getString("productId")
                                ?.toLongOrNull() ?: -1L

                            ProductDetailScreen(
                                productId = id,
                                repository = repository, 
                                onShowOnMap = { productId ->
                                    navController.navigate(NavRoutes.home(tab = 2, focusId = productId)) {
                                        popUpTo(NavRoutes.home()) { inclusive = false }
                                        launchSingleTop = true
                                    }
                                },
                                onBack = { navController.popBackStack() }
                            )
                        }
                    }
                }
            }
        }
    }
}