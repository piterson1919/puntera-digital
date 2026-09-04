package com.punteradigital.inventory

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.*
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import kotlin.math.absoluteValue
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Assignment
import androidx.compose.material.icons.automirrored.outlined.Assignment
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.toRoute
import com.punteradigital.inventory.domain.model.Origin
import com.punteradigital.inventory.presentation.auth.LoginScreen
import com.punteradigital.inventory.presentation.dashboard.DashboardScreen
import com.punteradigital.inventory.presentation.dispatch.DispatchListScreen
import com.punteradigital.inventory.presentation.inbound.EntryScreen
import com.punteradigital.inventory.presentation.muestras.MuestrasScreen
import com.punteradigital.inventory.presentation.refill.RefillMasterBoxScreen
import com.punteradigital.inventory.presentation.scanner.UnifiedScannerScreen
import com.punteradigital.inventory.presentation.home.HomeScreen
import com.punteradigital.inventory.presentation.settings.PrinterConfigScreen
import com.punteradigital.inventory.presentation.settings.SettingsScreen
import com.punteradigital.inventory.presentation.settings.CatalogScreen
import com.punteradigital.inventory.presentation.settings.CatalogEditScreen
import com.punteradigital.inventory.presentation.settings.CentralSyncSettingsScreen
import com.punteradigital.inventory.presentation.settings.QRHistoryScreen
import com.punteradigital.inventory.presentation.settings.RackMapScreen
import com.punteradigital.inventory.presentation.settings.UserManagementScreen
import com.punteradigital.inventory.presentation.traceability.TraceabilityScreen
import com.punteradigital.inventory.presentation.viewmodel.InventoryViewModel
import com.punteradigital.inventory.presentation.viewmodel.*
import com.punteradigital.inventory.presentation.settings.SizeTableScreen
import com.punteradigital.inventory.presentation.settings.BoxingConfigScreen
import com.punteradigital.inventory.presentation.settings.SyncConfigScreen
import com.punteradigital.inventory.presentation.pedidos.PedidoListScreen
import com.punteradigital.inventory.presentation.pedidos.PedidoCreateScreen
import com.punteradigital.inventory.presentation.pedidos.PedidoDetailScreen
import com.punteradigital.inventory.presentation.components.*
import com.punteradigital.inventory.ui.theme.*
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import kotlinx.serialization.Serializable

import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    @Inject
    lateinit var printerPreferences: com.punteradigital.inventory.data.local.PrinterPreferences

    @Inject
    lateinit var printRepository: com.punteradigital.inventory.data.repository.PrintRepository

    @Inject
    lateinit var themePreferences: com.punteradigital.inventory.data.local.ThemePreferences

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val viewModel: InventoryViewModel = hiltViewModel()
            val origin by viewModel.currentOrigin.collectAsState()
            val isDarkMode by themePreferences.isDarkMode.collectAsState()

            // Dynamic theme based on origin and dark/light
            PunteraDigitalTheme(origin = origin, isDarkMode = isDarkMode) {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    PunteraApp(viewModel, printerPreferences, printRepository, themePreferences)
                }
            }
        }
    }
}

// Routes
@Serializable object Login
@Serializable object MainHub
@Serializable data class ScannerRoute(val mode: String, val scanType: String)
@Serializable object DispatchListRoute
@Serializable object MuestrasRoute
@Serializable object RefillMasterBoxRoute
@Serializable object SettingsRoute
@Serializable object CatalogRoute
@Serializable data class CatalogEditRoute(val itemId: String?)
@Serializable object PrinterConfigRoute
@Serializable object QRHistoryRoute
@Serializable object RackMapRoute
@Serializable object UserManagementRoute
@Serializable object SizeTableRoute
@Serializable object BoxingConfigRoute
@Serializable object SyncConfigRoute
@Serializable object PedidoCreateRoute
@Serializable object PedidoListRoute
@Serializable data class PedidoDetailRoute(val pedidoIndex: Int)
@Serializable object EmpaqueRoute
@Serializable object TransferRoute
@Serializable object AuditRoute

private fun NavController.goTo(destination: Any) {
    navigate(destination)
}

private fun NavController.goToAndClear(destination: Any, clearUpTo: Any) {
    navigate(destination) {
        popUpTo(clearUpTo) { inclusive = true }
    }
}

@Composable
fun PunteraApp(
    viewModel: InventoryViewModel,
    printerPreferences: com.punteradigital.inventory.data.local.PrinterPreferences,
    printRepository: com.punteradigital.inventory.data.repository.PrintRepository,
    themePreferences: com.punteradigital.inventory.data.local.ThemePreferences
) {
    val navController = rememberNavController()

    NavHost(
        navController = navController,
        startDestination = Login,
        enterTransition = {
            slideIntoContainer(
                towards = AnimatedContentTransitionScope.SlideDirection.Start,
                animationSpec = tween(durationMillis = 100, easing = FastOutSlowInEasing)
            )
        },
        exitTransition = {
            slideOutOfContainer(
                towards = AnimatedContentTransitionScope.SlideDirection.Start,
                animationSpec = tween(durationMillis = 100, easing = FastOutSlowInEasing)
            )
        },
        popEnterTransition = {
            slideIntoContainer(
                towards = AnimatedContentTransitionScope.SlideDirection.End,
                animationSpec = tween(durationMillis = 100, easing = FastOutSlowInEasing)
            )
        },
        popExitTransition = {
            slideOutOfContainer(
                towards = AnimatedContentTransitionScope.SlideDirection.End,
                animationSpec = tween(durationMillis = 100, easing = FastOutSlowInEasing)
            )
        }
    ) {
        composable<Login> {
            LoginScreen(
                viewModel = viewModel,
                onLoginSuccess = { user ->
                    viewModel.setCurrentUser(user)
                    val nextRoute = if (user.role == "OPERADOR_EMPAQUE" || user.role == "SUPERVISOR_EMPAQUE") {
                        EmpaqueRoute
                    } else {
                        MainHub
                    }
                    navController.goToAndClear(nextRoute, Login)
                }
            )
        }
        composable<MainHub> {
            MainDashboard(
                viewModel = viewModel,
                themePreferences = themePreferences,
                onNavigateToScanner = { mode, scanType ->
                    navController.goTo(ScannerRoute(mode, scanType))
                },
                onNavigateToDispatchList = { navController.goTo(DispatchListRoute) },
                onNavigateToMuestras = { navController.goTo(MuestrasRoute) },
                onNavigateToRefill = { navController.goTo(RefillMasterBoxRoute) },
                onNavigateToCatalog = { navController.goTo(CatalogRoute) },
                onNavigateToPrinter = { navController.goTo(PrinterConfigRoute) },
                onNavigateToQRHistory = { navController.goTo(QRHistoryRoute) },
                onNavigateToRackMap = { navController.goTo(RackMapRoute) },
                onNavigateToUsers = { navController.goTo(UserManagementRoute) },
                onNavigateToSizeTable = { navController.goTo(SizeTableRoute) },
                onNavigateToBoxing = { navController.goTo(BoxingConfigRoute) },
                onNavigateToSync = { navController.goTo(SyncConfigRoute) },
                onNavigateToPedidoList = { navController.goTo(PedidoListRoute) },
                onNavigateToEmpaque = { navController.goTo(EmpaqueRoute) },
                onNavigateToTransfer = { navController.goTo(TransferRoute) },
                onNavigateToAudit = { navController.goTo(AuditRoute) },
                onLogout = {
                    viewModel.logout()
                    navController.goToAndClear(Login, MainHub)
                }
            )
        }
        composable<ScannerRoute> { backStackEntry ->
            val route = backStackEntry.toRoute<ScannerRoute>()
            UnifiedScannerScreen(
                viewModel = viewModel,
                moduleName = route.mode,
                scanType = route.scanType,
                onBack = { navController.popBackStack() }
            )
        }
        composable<DispatchListRoute> {
            DispatchListScreen(
                viewModel = viewModel,
                onBack = { navController.popBackStack() },
                onVerifyScan = {
                    navController.goTo(ScannerRoute("VERIFY", "MANUAL"))
                }
            )
        }
        composable<EmpaqueRoute> {
            com.punteradigital.inventory.presentation.empaque.EmpaqueScreen(
                viewModel = viewModel,
                onBack = {
                    val currentUser = viewModel.currentUser.value
                    if (currentUser?.role == "OPERADOR_EMPAQUE" || currentUser?.role == "SUPERVISOR_EMPAQUE") {
                        viewModel.logout()
                        navController.goToAndClear(Login, EmpaqueRoute)
                    } else {
                        navController.popBackStack()
                    }
                },
                onNavigateToPrinter = { navController.goTo(PrinterConfigRoute) },
                onNavigateToQRHistory = { navController.goTo(QRHistoryRoute) },
                onNavigateToScanner = { mode, scanType -> navController.goTo(ScannerRoute(mode, scanType)) }
            )
        }
        composable<MuestrasRoute> {
            MuestrasScreen(
                viewModel = viewModel,
                onBack = { navController.popBackStack() },
                onNavigateToScanner = { navController.goTo(ScannerRoute("MUESTRA_LOOKUP", "MANUAL")) }
            )
        }
        composable<RefillMasterBoxRoute> {
            RefillMasterBoxScreen(
                viewModel = viewModel,
                onBack = { navController.popBackStack() }
            )
        }
        composable<SettingsRoute> {
            val currentUser by viewModel.currentUser.collectAsState()
            SettingsScreen(
                onBack = { navController.popBackStack() },
                onNavigateToCatalog = { navController.goTo(CatalogRoute) },
                onNavigateToPrinter = { navController.goTo(PrinterConfigRoute) },
                onNavigateToQRHistory = { navController.goTo(QRHistoryRoute) },
                onNavigateToRackMap = { navController.goTo(RackMapRoute) },
                onNavigateToUsers = { navController.goTo(UserManagementRoute) },
                onNavigateToSizeTable = { navController.goTo(SizeTableRoute) },
                onNavigateToBoxing = { navController.goTo(BoxingConfigRoute) },
                onNavigateToSync = { navController.goTo(SyncConfigRoute) },
                onLogout = {
                    viewModel.logout()
                    navController.goToAndClear(Login, MainHub)
                },
                themePreferences = themePreferences,
                currentUser = currentUser
            )
        }
        composable<CatalogRoute> {
            CatalogScreen(
                viewModel = viewModel,
                onBack = { navController.popBackStack() },
                onNavigateToEdit = { id -> navController.goTo(CatalogEditRoute(id)) }
            )
        }
        composable<CatalogEditRoute> { backStackEntry ->
            val route = backStackEntry.toRoute<CatalogEditRoute>()
            CatalogEditScreen(
                viewModel = viewModel,
                itemId = route.itemId,
                onBack = { navController.popBackStack() }
            )
        }
        composable<PrinterConfigRoute> {
            PrinterConfigScreen(
                printerPreferences = printerPreferences,
                printRepository = printRepository,
                onBack = { navController.popBackStack() }
            )
        }
        composable<QRHistoryRoute> {
            QRHistoryScreen(
                viewModel = viewModel,
                onBack = { navController.popBackStack() }
            )
        }
        composable<RackMapRoute> {
            RackMapScreen(
                viewModel = viewModel,
                onBack = { navController.popBackStack() }
            )
        }
        composable<UserManagementRoute> {
            UserManagementScreen(
                viewModel = viewModel,
                onBack = { navController.popBackStack() }
            )
        }
        composable<SizeTableRoute> {
            SizeTableScreen(
                onBack = { navController.popBackStack() }
            )
        }
        composable<BoxingConfigRoute> {
            BoxingConfigScreen(
                onBack = { navController.popBackStack() }
            )
        }
        composable<SyncConfigRoute> {
            val context = LocalContext.current
            CentralSyncSettingsScreen(
                prefs = com.punteradigital.inventory.data.local.SyncPreferences(context.applicationContext),
                onBack = { navController.popBackStack() }
            )
        }
        composable<PedidoCreateRoute> {
            val currentUser by viewModel.currentUser.collectAsState()
            PedidoCreateScreen(
                onBack = { navController.popBackStack() },
                currentUserName = currentUser?.name ?: "Usuario"
            )
        }
        composable<PedidoListRoute> {
            val currentUser by viewModel.currentUser.collectAsState()
            PedidoListScreen(
                onBack = { navController.popBackStack() },
                onNavigateToCreate = { navController.goTo(PedidoCreateRoute) },
                onNavigateToDetail = { index -> navController.goTo(PedidoDetailRoute(index)) },
                currentUserRole = currentUser?.role ?: "ADMIN"
            )
        }
        composable<PedidoDetailRoute> { backStackEntry ->
            val route = backStackEntry.toRoute<PedidoDetailRoute>()
            val currentUser by viewModel.currentUser.collectAsState()
            PedidoDetailScreen(
                pedidoIndex = route.pedidoIndex,
                onBack = { navController.popBackStack() },
                currentUserName = currentUser?.name ?: "Usuario",
                viewModel = viewModel
            )
        }
        composable<TransferRoute> {
            com.punteradigital.inventory.presentation.transfer.TransferScreen(
                viewModel = viewModel,
                onBack = { navController.popBackStack() }
            )
        }
        composable<AuditRoute> {
            com.punteradigital.inventory.presentation.audit.AuditScreen(
                viewModel = viewModel,
                onBack = { navController.popBackStack() }
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainDashboard(
    viewModel: InventoryViewModel,
    themePreferences: com.punteradigital.inventory.data.local.ThemePreferences,
    onNavigateToScanner: (String, String) -> Unit,
    onNavigateToDispatchList: () -> Unit,
    onNavigateToMuestras: () -> Unit,
    onNavigateToRefill: () -> Unit,
    onNavigateToCatalog: () -> Unit,
    onNavigateToPrinter: () -> Unit,
    onNavigateToQRHistory: () -> Unit,
    onNavigateToRackMap: () -> Unit,
    onNavigateToUsers: () -> Unit = {},
    onNavigateToSizeTable: () -> Unit = {},
    onNavigateToBoxing: () -> Unit = {},
    onNavigateToSync: () -> Unit = {},
    onNavigateToPedidoList: () -> Unit = {},
    onNavigateToEmpaque: () -> Unit = {},
    onNavigateToTransfer: () -> Unit = {},
    onNavigateToAudit: () -> Unit = {},
    onLogout: () -> Unit
) {
    val origin by viewModel.currentOrigin.collectAsState()
    val currentUser by viewModel.currentUser.collectAsState()
    var selectedTab by remember { mutableIntStateOf(0) }
    val coroutineScope = rememberCoroutineScope()

    val navItems = listOf(
        KineticNavItem("Inicio", Icons.Outlined.Home, Icons.Filled.Home, "🏠"),
        KineticNavItem("Entrada", Icons.AutoMirrored.Outlined.Assignment, Icons.AutoMirrored.Filled.Assignment, "📋"),
        KineticNavItem("Movim.", Icons.Outlined.SwapHoriz, Icons.Filled.SwapHoriz, "🔄"),
        KineticNavItem("Traza", Icons.Outlined.History, Icons.Filled.History, "📜"),
        KineticNavItem("Config", Icons.Outlined.Settings, Icons.Filled.Settings, "⚙")
    )

    // HorizontalPager state
    val pagerState = rememberPagerState(pageCount = { navItems.size })

    // Sync: pager swipe → bottom nav (ONE direction only, using settledPage to avoid flicker)
    LaunchedEffect(pagerState) {
        snapshotFlow { pagerState.settledPage }.collect { page ->
            selectedTab = page
        }
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        bottomBar = {
            KineticBottomNavBar(
                items = navItems,
                selectedIndex = selectedTab,
                onItemSelected = { index ->
                    selectedTab = index
                    coroutineScope.launch {
                        pagerState.animateScrollToPage(index)
                    }
                }
            )
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .padding(innerPadding)
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
        ) {
            HorizontalPager(
                state = pagerState,
                modifier = Modifier.fillMaxSize(),
                // 0 = only the visible page is composed and active.
                // beyondViewportPageCount=1 kept 3 pages alive simultaneously,
                // each subscribing their own DB flows even when off-screen.
                beyondViewportPageCount = 0
            ) { page ->
                val pageOffset = (pagerState.currentPage - page) + pagerState.currentPageOffsetFraction
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .graphicsLayer {
                            // Fade out off-screen pages smoothly
                            alpha = (1f - (pageOffset.absoluteValue * 0.5f)).coerceIn(0f, 1f)
                            // Scale down slightly to create a nice depth/layering effect
                            val scale = (1f - (pageOffset.absoluteValue * 0.08f)).coerceIn(0.85f, 1f)
                            scaleX = scale
                            scaleY = scale
                            // Parallax offset: shift the page a bit slower than the page swipe
                            translationX = pageOffset * size.width * 0.2f
                        }
                ) {
                    when (page) {
                        0 -> HomeScreen(
                            viewModel = viewModel,
                            onNavigateToEntry = { selectedTab = 1 },
                            onNavigateToMovements = { selectedTab = 2 },
                            onNavigateToRefill = onNavigateToRefill
                        )
                        1 -> EntryScreen(viewModel = viewModel, onNavigateToScanner = onNavigateToScanner)
                        2 -> MovementHubScreen(
                            viewModel = viewModel,
                            onNavigateToScanner = onNavigateToScanner,
                            onNavigateToDispatchList = onNavigateToDispatchList,
                            onNavigateToMuestras = onNavigateToMuestras,
                            onNavigateToPedidoList = onNavigateToPedidoList,
                            onNavigateToEmpaque = onNavigateToEmpaque,
                            onNavigateToTransfer = onNavigateToTransfer,
                            onNavigateToAudit = onNavigateToAudit,
                            currentUserRole = currentUser?.role ?: "ADMIN"
                        )
                        3 -> TraceabilityScreen(viewModel = viewModel)
                        4 -> SettingsScreen(
                            onNavigateToCatalog = onNavigateToCatalog,
                            onNavigateToPrinter = onNavigateToPrinter,
                            onNavigateToQRHistory = onNavigateToQRHistory,
                            onNavigateToRackMap = onNavigateToRackMap,
                            onNavigateToUsers = onNavigateToUsers,
                            onNavigateToSizeTable = onNavigateToSizeTable,
                            onNavigateToBoxing = onNavigateToBoxing,
                            onNavigateToSync = onNavigateToSync,
                            onLogout = onLogout,
                            themePreferences = themePreferences,
                            currentUser = currentUser
                        )
                    }
                }
            }
        }
    }
}

/**
 * Movement hub with Stand-By, Dispatch, Quality, and Muestras Retornables.
 * Dispatch now navigates to a list-based screen instead of scanner.
 */
@Composable
fun MovementHubScreen(
    viewModel: InventoryViewModel,
    onNavigateToScanner: (String, String) -> Unit,
    onNavigateToDispatchList: () -> Unit,
    onNavigateToMuestras: () -> Unit,
    onNavigateToPedidoList: () -> Unit = {},
    onNavigateToEmpaque: () -> Unit = {},
    onNavigateToTransfer: () -> Unit = {},
    onNavigateToAudit: () -> Unit = {},
    currentUserRole: String = "ADMIN"
) {
    val origin by viewModel.currentOrigin.collectAsState()
    var standByExpanded by remember { mutableStateOf(false) }
    var dispatchExpanded by remember { mutableStateOf(false) }
    var calidadExpanded by remember { mutableStateOf(false) }

    androidx.compose.foundation.lazy.LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        contentPadding = PaddingValues(vertical = 24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Text(
                "Módulos de Movimiento",
                style = MaterialTheme.typography.headlineSmall,
                color = MaterialTheme.colorScheme.onBackground
            )
        }

        val allowedEmpaqueRoles = listOf("ADMIN", "SUPERVISOR_EMPAQUE", "OPERADOR_EMPAQUE", "SUPERVISOR_ALMACEN", "AUXILIAR_ALMACEN")
        if (allowedEmpaqueRoles.any { currentUserRole.equals(it, ignoreCase = true) }) {
            item {
                MovementTileCard(
                    title = "Área de Empaque",
                    subtitle = "Generar etiquetas · Pre-Entradas",
                    emoji = "🏷",
                    accentColor = MaterialTheme.colorScheme.primary,
                    onClick = onNavigateToEmpaque
                )
            }
        }

        item {
            MovementTileCard(
                title = "Pedidos de Venta",
                subtitle = "Crear pedidos · Escanear para cumplir",
                emoji = "📦",
                accentColor = DispatchGreen,
                onClick = onNavigateToPedidoList,
                trailingIcon = { Icon(Icons.Default.ChevronRight, null, tint = MaterialTheme.colorScheme.onSurfaceVariant) }
            )
        }

        item {
            MovementTileCard(
                title = "Traslado Interno",
                subtitle = "Re-ubicar calzado de un rack a otro",
                emoji = "🔄",
                accentColor = MaterialTheme.colorScheme.secondary,
                onClick = onNavigateToTransfer,
                trailingIcon = { Icon(Icons.Default.ChevronRight, null, tint = MaterialTheme.colorScheme.onSurfaceVariant) }
            )
        }

        item {
            MovementTileCard(
                title = "Auditoría de Rack",
                subtitle = "Conteo físico vs sistema y corrección",
                emoji = "🔍",
                accentColor = MaterialTheme.colorScheme.tertiary,
                onClick = onNavigateToAudit,
                trailingIcon = { Icon(Icons.Default.ChevronRight, null, tint = MaterialTheme.colorScheme.onSurfaceVariant) }
            )
        }

        // ⏳ Stand-By
        item {
            ExpandableMovementCard(
                title = "Stand-By",
                subtitle = "Pre-despacho → ZONA_PREDESPACHO",
                emoji = "⏸",
                color = StandByAmber,
                expanded = standByExpanded,
                onCardClick = { standByExpanded = !standByExpanded },
                option1Text = "⚡ Ráfaga RFID",
                onOption1Click = { onNavigateToScanner("STANDBY", "RAPID") },
                option2Text = "📋 Cola Stand-By",
                onOption2Click = onNavigateToDispatchList
            )
        }

        // 🚚 Despacho
        item {
            ExpandableMovementCard(
                title = "Despacho",
                subtitle = "Salidas definitivas del almacén",
                emoji = "🚚",
                color = DispatchGreen,
                expanded = dispatchExpanded,
                onCardClick = { dispatchExpanded = !dispatchExpanded },
                option1Text = "👆 Manual",
                onOption1Click = onNavigateToDispatchList,
                option2Text = "⚡ Salida RFID",
                onOption2Click = { onNavigateToScanner("DISPATCH", "RAPID") }
            )
        }

        // ⊖ Calidad / Bajas
        item {
            ExpandableMovementCard(
                title = "Calidad / Bajas",
                subtitle = "Inspección y mermas de mercancía",
                emoji = "⊖",
                color = QualityPurple,
                expanded = calidadExpanded,
                onCardClick = { calidadExpanded = !calidadExpanded },
                option1Text = "👆 Manual",
                onOption1Click = { onNavigateToScanner("QUALITY", "MANUAL") },
                option2Text = "⚡ Inspección RFID",
                onOption2Click = { onNavigateToScanner("QUALITY", "RAPID") }
            )
        }

        item {
            MovementTileCard(
                title = "Muestras Retornables",
                subtitle = "Uso comercial, devuelta al stock",
                emoji = "🏪",
                accentColor = MuestraTeal,
                onClick = onNavigateToMuestras,
                trailingIcon = { Icon(Icons.Default.ChevronRight, null, tint = MaterialTheme.colorScheme.onSurfaceVariant) }
            )
        }
    }
}

@Composable
private fun MovementTileCard(
    title: String,
    subtitle: String,
    emoji: String,
    accentColor: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    trailingIcon: @Composable (() -> Unit)? = null
) {
    KineticCard(
        modifier = modifier.fillMaxWidth(),
        onClick = onClick,
        padding = 20.dp
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = accentColor.copy(alpha = 0.15f),
                modifier = Modifier.size(48.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Text(emoji, fontSize = 28.sp)
                }
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(title, style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onSurface)
                Text(subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            trailingIcon?.invoke()
        }
    }
}

@Composable
fun SubMenuOptionCard(
    text: String,
    onClick: () -> Unit,
    color: Color,
    modifier: Modifier = Modifier
) {
    // Extract emoji/symbol prefix from text
    val emoji = text.takeWhile { !it.isLetterOrDigit() }.trim()
    val cleanText = text.drop(emoji.length).trim()

    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.94f else 1f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessLow),
        label = "press_scale"
    )

    Surface(
        onClick = onClick,
        interactionSource = interactionSource,
        shape = RoundedCornerShape(16.dp),
        color = color.copy(alpha = 0.08f),
        border = BorderStroke(1.dp, color.copy(alpha = 0.35f)),
        modifier = modifier
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            }
    ) {
        Column(
            modifier = Modifier
                .padding(vertical = 16.dp, horizontal = 12.dp)
                .fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            if (emoji.isNotEmpty()) {
                Text(
                    text = emoji,
                    fontSize = 22.sp
                )
            }
            Text(
                text = cleanText,
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.ExtraBold,
                color = MaterialTheme.colorScheme.onSurface,
                textAlign = TextAlign.Center
            )
        }
    }
}

@Composable
fun ExpandableMovementCard(
    title: String,
    subtitle: String,
    emoji: String,
    color: Color,
    expanded: Boolean,
    onCardClick: () -> Unit,
    option1Text: String,
    onOption1Click: () -> Unit,
    option2Text: String,
    onOption2Click: () -> Unit
) {
    val arrowRotation by animateFloatAsState(
        targetValue = if (expanded) 180f else 0f,
        animationSpec = tween(durationMillis = 300, easing = FastOutSlowInEasing),
        label = "arrow_rotation"
    )

    val cardModifier = if (expanded) {
        Modifier.border(1.5.dp, color.copy(alpha = 0.5f), RoundedCornerShape(20.dp))
    } else {
        Modifier
    }

    KineticCard(
        modifier = cardModifier.fillMaxWidth(),
        onClick = onCardClick,
        padding = 20.dp
    ) {
        Column {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = color.copy(alpha = 0.15f),
                    modifier = Modifier.size(48.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Text(emoji, fontSize = 28.sp)
                    }
                }
                Column(modifier = Modifier.weight(1f)) {
                    Text(title, style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onSurface, fontWeight = FontWeight.Bold)
                    Text(subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                Icon(
                    imageVector = Icons.Default.ExpandMore,
                    contentDescription = if (expanded) "Colapsar" else "Expandir",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.rotate(arrowRotation)
                )
            }

            AnimatedVisibility(
                visible = expanded,
                enter = expandVertically() + fadeIn(),
                exit = shrinkVertically() + fadeOut()
            ) {
                Column {
                    Spacer(modifier = Modifier.height(16.dp))
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        SubMenuOptionCard(
                            text = option1Text,
                            onClick = onOption1Click,
                            color = color,
                            modifier = Modifier.weight(1f)
                        )
                        SubMenuOptionCard(
                            text = option2Text,
                            onClick = onOption2Click,
                            color = color,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }
        }
    }
}

data class NavigationItem(
    val label: String,
    val unselectedIcon: ImageVector,
    val selectedIcon: ImageVector
)
