package cn.tsinghua.sagemotion.ui.components

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.BitmapShader
import android.graphics.Canvas as AndroidCanvas
import android.graphics.Color as AndroidColor
import android.graphics.Matrix
import android.graphics.Paint
import android.graphics.Shader
import android.graphics.Typeface
import android.net.Uri
import android.os.Handler
import android.os.Looper
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import cn.tsinghua.sagemotion.BuildConfig
import cn.tsinghua.sagemotion.R
import cn.tsinghua.sagemotion.ui.theme.SageSignalCoral
import cn.tsinghua.sagemotion.ui.theme.SageSignalLime
import com.amap.api.location.AMapLocation
import com.amap.api.location.AMapLocationClient
import com.amap.api.location.AMapLocationClientOption
import com.amap.api.maps.AMap
import com.amap.api.maps.AMapUtils
import com.amap.api.maps.CameraUpdateFactory
import com.amap.api.maps.CoordinateConverter
import com.amap.api.maps.MapView
import com.amap.api.maps.MapsInitializer
import com.amap.api.maps.model.BitmapDescriptorFactory
import com.amap.api.maps.model.CameraPosition
import com.amap.api.maps.model.CustomMapStyleOptions
import com.amap.api.maps.model.LatLng
import com.amap.api.maps.model.LatLngBounds
import com.amap.api.maps.model.Marker
import com.amap.api.maps.model.MarkerOptions
import com.amap.api.maps.model.Polyline
import com.amap.api.maps.model.PolylineOptions
import com.amap.api.services.core.AMapException
import com.amap.api.services.core.LatLonPoint
import com.amap.api.services.route.BusRouteResultV2
import com.amap.api.services.route.DriveRouteResultV2
import com.amap.api.services.route.RideRouteResultV2
import com.amap.api.services.route.RouteSearchV2
import com.amap.api.services.route.WalkRouteResultV2
import java.util.Locale
import kotlin.math.ceil
import kotlin.math.roundToInt

private const val AMAP_PRIVACY_URL = "https://lbs.amap.com/pages/privacy/"
private const val PRIVACY_PREFERENCES = "sage_amap_privacy"
private const val PRIVACY_AGREED = "privacy_agreed"
private const val PARK_GUIDANCE_RADIUS_METERS = 2_500f
private const val OFFLINE_STYLE_DATA = "amap_style/style.data"
private const val OFFLINE_STYLE_EXTRA = "amap_style/style_extra.data"
private const val OFFLINE_STYLE_TEXTURES = "amap_style/textures.zip"

private val LocalAmapPrivacyAgreed = compositionLocalOf { false }

/**
 * 在创建任何地图、搜索或定位对象之前收集一次高德隐私同意。
 * 定位默认关闭，只有参与者主动点击“开始园内指引”后才会申请系统权限并启动。
 */
@Composable
fun AmapPrivacyGate(content: @Composable () -> Unit) {
    val context = LocalContext.current
    val uriHandler = LocalUriHandler.current
    val preferences = remember(context) {
        context.getSharedPreferences(PRIVACY_PREFERENCES, Context.MODE_PRIVATE)
    }
    var agreed by remember { mutableStateOf(preferences.getBoolean(PRIVACY_AGREED, false)) }
    var promptVisible by rememberSaveable {
        mutableStateOf(BuildConfig.AMAP_API_KEY_CONFIGURED && !agreed)
    }

    CompositionLocalProvider(LocalAmapPrivacyAgreed provides agreed, content = content)

    if (promptVisible) {
        AlertDialog(
            onDismissRequest = { promptVisible = false },
            title = { Text("启用真实地图") },
            text = {
                Text(
                    "路线页将使用高德地图 SDK 加载在线地图并请求步行路径。定位默认关闭；" +
                        "只有点击“开始园内指引”后，应用才会申请位置权限并在指引期间处理当前位置。" +
                        "参与者编号不会发送给高德。",
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        preferences.edit().putBoolean(PRIVACY_AGREED, true).apply()
                        agreed = true
                        promptVisible = false
                    },
                ) { Text("同意并启用") }
            },
            dismissButton = {
                TextButton(onClick = { uriHandler.openUri(AMAP_PRIVACY_URL) }) {
                    Text("查看高德隐私说明")
                }
            },
        )
    }
}

/** 高德真实步行路线、定位和轻量园内指引的界面状态。 */
private data class ParkRouteUiState(
    val status: String = "正在请求高德步行路线…",
    val summary: String = "",
    val instruction: String = "",
    val isGuiding: Boolean = false,
    val canRecenter: Boolean = false,
    val isError: Boolean = false,
)

private data class PlannedStep(
    val instruction: String,
    val points: List<LatLng>,
)

private data class PlannedPath(
    val points: List<LatLng>,
    val distanceMeters: Float,
    val durationSeconds: Long,
    val steps: List<PlannedStep>,
)

/** 固定园内起终点的结果只在进程内缓存，避免每次切换实验工具都重复消耗路线请求。 */
private object ParkRouteCache {
    var previewPaths: List<PlannedPath>? = null
}

/**
 * 八家郊野公园的高德真实地图。
 *
 * 路线由高德步行路径规划返回，并使用地图原生 [Polyline] 绘制，因此拖动、缩放地图时会和道路一起移动。
 * [guidanceControls] 只用于探索主界面：参与者可主动开启定位并获得剩余距离与下一步文字指引。
 */
@Composable
fun AmapParkMap(
    contentDescription: String?,
    modifier: Modifier = Modifier,
    selectedAlternative: Boolean = false,
    routeEnabled: Boolean = true,
    showRouteSummary: Boolean = false,
    guidanceControls: Boolean = false,
    guidanceBottomInset: Dp = 0.dp,
    gesturesEnabled: Boolean = true,
    showAlternativeRoutes: Boolean = true,
    journeyPhotoUris: List<String> = emptyList(),
    journeyVoiceCount: Int = 0,
    journeyReplanCount: Int = 0,
    selectedJourneyPhotoIndex: Int = -1,
    onJourneyPhotoSelected: (Int) -> Unit = {},
) {
    val inspection = LocalInspectionMode.current
    val privacyAgreed = LocalAmapPrivacyAgreed.current
    if (inspection || !BuildConfig.AMAP_API_KEY_CONFIGURED || !privacyAgreed) {
        Box(modifier) {
            Image(
                painter = painterResource(R.drawable.park_map_background),
                contentDescription = contentDescription,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize(),
            )
            if (routeEnabled) {
                PreviewFieldRouteOverlay(
                    selectedAlternative = selectedAlternative,
                    showAlternativeRoutes = showAlternativeRoutes,
                    modifier = Modifier.fillMaxSize(),
                )
            }
            if (inspection && (showRouteSummary || guidanceControls)) {
                ParkRouteStatusCard(
                    state = ParkRouteUiState(
                        status = "正在按高德路线指引",
                        summary = "剩余约 2.7 公里",
                        instruction = "向西南步行 13 米后左转",
                        isGuiding = guidanceControls,
                        canRecenter = guidanceControls,
                    ),
                    guidanceControls = guidanceControls,
                    onToggleGuidance = {},
                    onRecenter = {},
                    onReplan = {},
                    modifier = if (guidanceControls) {
                        Modifier
                            .align(Alignment.BottomCenter)
                            .fillMaxWidth()
                            .padding(start = 14.dp, end = 14.dp, bottom = guidanceBottomInset + 10.dp)
                    } else {
                        Modifier
                            .align(Alignment.CenterEnd)
                            .padding(end = 12.dp)
                            .widthIn(max = 218.dp)
                    },
                )
            }
        }
        return
    }

    val context = LocalContext.current
    val applicationContext = context.applicationContext
    val lifecycleOwner = LocalLifecycleOwner.current

    MapsInitializer.updatePrivacyShow(applicationContext, true, true)
    MapsInitializer.updatePrivacyAgree(applicationContext, true)
    AMapLocationClient.updatePrivacyShow(applicationContext, true, true)
    AMapLocationClient.updatePrivacyAgree(applicationContext, true)

    val mapView = remember(context, gesturesEnabled) {
        MapView(context).apply {
            onCreate(null)
            configureBaseMap(context, map, gesturesEnabled)
        }
    }
    val controller = remember(mapView) {
        ParkRouteController(applicationContext, mapView.map)
    }
    val routeState = controller.uiState

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions(),
    ) { result ->
        val granted = result[Manifest.permission.ACCESS_FINE_LOCATION] == true
        if (granted) controller.startGuidance() else controller.reportPermissionDenied()
    }

    fun requestGuidance() {
        val hasFine = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.ACCESS_FINE_LOCATION,
        ) == PackageManager.PERMISSION_GRANTED
        if (hasFine) {
            controller.startGuidance()
        } else {
            permissionLauncher.launch(
                arrayOf(
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.ACCESS_COARSE_LOCATION,
                ),
            )
        }
    }

    LaunchedEffect(controller, routeEnabled) {
        if (routeEnabled) controller.planPreviewRoute() else controller.clearRoute()
    }
    LaunchedEffect(controller, selectedAlternative) {
        controller.selectAlternative(selectedAlternative)
    }
    LaunchedEffect(
        controller,
        showAlternativeRoutes,
        journeyPhotoUris,
        journeyVoiceCount,
        journeyReplanCount,
        selectedJourneyPhotoIndex,
    ) {
        controller.updateJourneyPresentation(
            showAlternatives = showAlternativeRoutes,
            photoUris = journeyPhotoUris,
            voiceCount = journeyVoiceCount,
            replanCount = journeyReplanCount,
            selectedPhotoIndex = selectedJourneyPhotoIndex,
            onPhotoSelected = onJourneyPhotoSelected,
        )
    }

    DisposableEffect(mapView, controller, lifecycleOwner) {
        var destroyed = false
        fun destroyOnce() {
            if (!destroyed) {
                destroyed = true
                controller.destroy()
                mapView.onDestroy()
            }
        }
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_RESUME -> {
                    mapView.onResume()
                    controller.resumeLocationIfNeeded()
                }

                Lifecycle.Event.ON_PAUSE -> {
                    controller.pauseLocation()
                    mapView.onPause()
                }

                Lifecycle.Event.ON_DESTROY -> destroyOnce()
                else -> Unit
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        if (lifecycleOwner.lifecycle.currentState.isAtLeast(Lifecycle.State.RESUMED)) mapView.onResume()
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
            controller.pauseLocation()
            mapView.onPause()
            destroyOnce()
        }
    }

    Box(modifier) {
        AndroidView(
            factory = { mapView },
            modifier = Modifier
                .fillMaxSize()
                .semantics { contentDescription?.let { this.contentDescription = it } },
        )
        if (showRouteSummary || guidanceControls) {
            ParkRouteStatusCard(
                state = routeState,
                guidanceControls = guidanceControls,
                onToggleGuidance = {
                    if (routeState.isGuiding) controller.stopGuidance() else requestGuidance()
                },
                onRecenter = controller::recenter,
                onReplan = controller::replanFromCurrentLocation,
                modifier = if (guidanceControls) {
                    Modifier
                        .align(Alignment.BottomCenter)
                        .fillMaxWidth()
                        .padding(start = 14.dp, end = 14.dp, bottom = guidanceBottomInset + 10.dp)
                } else {
                    Modifier
                        .align(Alignment.CenterEnd)
                        .padding(end = 12.dp)
                        .widthIn(max = 218.dp)
                },
            )
        }
    }
}

/**
 * Preview 与无地图密钥状态下的确定性路线叠层。
 * 真机联网时仍由高德原生 Polyline 绘制；这里让截图回归和离线演示保留相同的路线语义。
 */
@Composable
private fun PreviewFieldRouteOverlay(
    selectedAlternative: Boolean,
    showAlternativeRoutes: Boolean,
    modifier: Modifier = Modifier,
) {
    Canvas(modifier) {
        val recommended = Path().apply {
            moveTo(size.width * .47f, size.height * .86f)
            cubicTo(size.width * .34f, size.height * .76f, size.width * .29f, size.height * .66f, size.width * .36f, size.height * .56f)
            cubicTo(size.width * .44f, size.height * .45f, size.width * .39f, size.height * .34f, size.width * .54f, size.height * .25f)
            cubicTo(size.width * .62f, size.height * .20f, size.width * .65f, size.height * .16f, size.width * .72f, size.height * .13f)
        }
        val alternative = Path().apply {
            moveTo(size.width * .47f, size.height * .86f)
            cubicTo(size.width * .62f, size.height * .76f, size.width * .72f, size.height * .67f, size.width * .70f, size.height * .57f)
            cubicTo(size.width * .68f, size.height * .45f, size.width * .80f, size.height * .34f, size.width * .73f, size.height * .24f)
            cubicTo(size.width * .69f, size.height * .19f, size.width * .71f, size.height * .16f, size.width * .72f, size.height * .13f)
        }
        val selected = if (selectedAlternative) alternative else recommended
        val compared = if (selectedAlternative) recommended else alternative
        val selectedColor = if (selectedAlternative) SageSignalCoral else SageSignalLime
        val comparedColor = if (selectedAlternative) SageSignalLime else SageSignalCoral
        if (showAlternativeRoutes) {
            drawPath(
                compared,
                color = Color.White.copy(alpha = .90f),
                style = Stroke(width = 5.5.dp.toPx(), cap = StrokeCap.Round),
            )
            drawPath(
                compared,
                color = comparedColor.copy(alpha = .82f),
                style = Stroke(
                    width = 2.4.dp.toPx(),
                    cap = StrokeCap.Round,
                    pathEffect = PathEffect.dashPathEffect(floatArrayOf(7.dp.toPx(), 6.dp.toPx())),
                ),
            )
        }
        drawPath(
            selected,
            color = Color.White.copy(alpha = .94f),
            style = Stroke(width = 7.dp.toPx(), cap = StrokeCap.Round),
        )
        drawPath(
            selected,
            color = selectedColor.copy(alpha = .94f),
            style = Stroke(
                width = 3.dp.toPx(),
                cap = StrokeCap.Round,
                pathEffect = PathEffect.dashPathEffect(floatArrayOf(8.dp.toPx(), 4.dp.toPx())),
            ),
        )
        val recommendedNodes = listOf(
            androidx.compose.ui.geometry.Offset(size.width * .47f, size.height * .86f),
            androidx.compose.ui.geometry.Offset(size.width * .38f, size.height * .56f),
            androidx.compose.ui.geometry.Offset(size.width * .54f, size.height * .25f),
            androidx.compose.ui.geometry.Offset(size.width * .72f, size.height * .13f),
        )
        val alternativeNodes = listOf(
            androidx.compose.ui.geometry.Offset(size.width * .47f, size.height * .86f),
            androidx.compose.ui.geometry.Offset(size.width * .70f, size.height * .57f),
            androidx.compose.ui.geometry.Offset(size.width * .73f, size.height * .24f),
            androidx.compose.ui.geometry.Offset(size.width * .72f, size.height * .13f),
        )
        (if (selectedAlternative) alternativeNodes else recommendedNodes).forEachIndexed { index, point ->
            drawCircle(Color.White.copy(alpha = .96f), radius = if (index in 1..2) 7.dp.toPx() else 8.dp.toPx(), center = point)
            drawCircle(selectedColor, radius = if (index in 1..2) 4.dp.toPx() else 5.dp.toPx(), center = point)
        }
    }
}

@Composable
private fun ParkRouteStatusCard(
    state: ParkRouteUiState,
    guidanceControls: Boolean,
    onToggleGuidance: () -> Unit,
    onRecenter: () -> Unit,
    onReplan: () -> Unit,
    modifier: Modifier = Modifier,
) {
    if (guidanceControls) {
        var expanded by rememberSaveable { mutableStateOf(false) }
        val primaryText = state.instruction.ifBlank { state.status }
        val secondaryText = state.summary.ifBlank { state.status }

        FrostedGlassSurface(
            shape = RoundedCornerShape(18.dp),
            modifier = modifier,
        ) {
            Column(Modifier.padding(horizontal = 10.dp, vertical = 7.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("↗", color = Color(0xFF315E4B), fontSize = 22.sp, fontWeight = FontWeight.Bold)
                    Column(
                        Modifier
                            .weight(1f)
                            .padding(start = 8.dp, end = 3.dp),
                    ) {
                        Text(
                            text = primaryText,
                            color = if (state.isError) Color(0xFFB35D2E) else Color(0xFF1F3028),
                            fontSize = 13.sp,
                            fontWeight = FontWeight.SemiBold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                        if (secondaryText != primaryText) {
                            Text(
                                text = secondaryText,
                                color = Color(0xFF607068),
                                fontSize = 10.sp,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                        }
                    }
                    TextButton(onClick = { expanded = !expanded }) {
                        Text(if (expanded) "收起" else "详情", fontSize = 10.sp)
                    }
                    Button(onClick = onToggleGuidance) {
                        Text(if (state.isGuiding) "结束" else "开始", fontSize = 10.sp)
                    }
                }
                if (expanded) {
                    if (state.status != primaryText && state.status != secondaryText) {
                        Text(
                            text = state.status,
                            color = if (state.isError) Color(0xFFB35D2E) else Color(0xFF34423B),
                            fontSize = 10.sp,
                            modifier = Modifier.padding(top = 3.dp),
                        )
                    }
                    if (state.isGuiding && state.canRecenter) {
                        Row(
                            Modifier.fillMaxWidth().padding(start = 32.dp),
                            horizontalArrangement = Arrangement.End,
                        ) {
                            TextButton(onClick = onRecenter) { Text("回到位置", fontSize = 10.sp) }
                            TextButton(onClick = onReplan) { Text("重新计算", fontSize = 10.sp) }
                        }
                    }
                }
            }
        }
        return
    }

    FrostedGlassSurface(
        modifier = modifier,
    ) {
        Column(Modifier.padding(horizontal = 12.dp, vertical = 10.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("↗", color = Color(0xFF315E4B), fontSize = 24.sp, fontWeight = FontWeight.Bold)
                Text(
                    text = if (state.isGuiding) "银小叶 · 园内指引" else "银小叶 · 高德路线",
                    color = Color(0xFF315E4B),
                    fontSize = 13.sp,
                    modifier = Modifier.padding(start = 6.dp),
                )
            }
            Text(
                text = state.status,
                color = if (state.isError) Color(0xFFB35D2E) else Color(0xFF34423B),
                fontSize = 11.sp,
                modifier = Modifier.padding(top = 3.dp),
            )
            if (state.summary.isNotBlank()) {
                Text(state.summary, color = Color(0xFF607068), fontSize = 10.sp, modifier = Modifier.padding(top = 2.dp))
            }
            if (state.instruction.isNotBlank()) {
                Text(state.instruction, color = Color(0xFF1F3028), fontSize = 11.sp, modifier = Modifier.padding(top = 5.dp))
            }
        }
    }
}

private class ParkRouteController(
    private val context: Context,
    private val map: AMap,
) : RouteSearchV2.OnRouteSearchListener {
    private val mainHandler = Handler(Looper.getMainLooper())
    private val routeSearch = runCatching { RouteSearchV2(context) }.getOrNull()
    private val routePolylines = mutableListOf<Polyline>()
    private val endpointMarkers = mutableListOf<Marker>()
    private val journeyMarkers = mutableListOf<Marker>()
    private val photoMarkerIndices = mutableMapOf<String, Int>()
    private var locationMarker: Marker? = null
    private var locationClient: AMapLocationClient? = null
    private var plannedPaths: List<PlannedPath> = emptyList()
    private var selectedAlternative = false
    private var showAlternativeRoutes = true
    private var journeyPhotoUris: List<String> = emptyList()
    private var journeyVoiceCount = 0
    private var journeyReplanCount = 0
    private var selectedJourneyPhotoIndex = -1
    private var onJourneyPhotoSelected: (Int) -> Unit = {}
    private var latestLocation: LatLng? = null
    private var guidanceRequested = false
    private var pendingGuidanceRoute = false

    var uiState by mutableStateOf(ParkRouteUiState())
        private set

    init {
        routeSearch?.setRouteSearchListener(this)
        map.setOnMarkerClickListener { marker ->
            photoMarkerIndices[marker.id]?.let { index ->
                onJourneyPhotoSelected(index)
                true
            } ?: false
        }
        if (routeSearch == null) {
            uiState = ParkRouteUiState(status = "路线服务初始化失败", isError = true)
        }
    }

    fun planPreviewRoute() {
        ParkRouteCache.previewPaths?.takeIf { it.isNotEmpty() }?.let {
            applyPaths(it, fitCamera = true)
            return
        }
        requestRoute(PARK_START_GCJ02, PARK_DESTINATION_GCJ02, guidanceRoute = false)
    }

    fun clearRoute() {
        plannedPaths = emptyList()
        routePolylines.forEach(Polyline::remove)
        endpointMarkers.forEach(Marker::remove)
        journeyMarkers.forEach(Marker::remove)
        routePolylines.clear()
        endpointMarkers.clear()
        journeyMarkers.clear()
        photoMarkerIndices.clear()
        uiState = ParkRouteUiState(status = "等待开始路线规划")
    }

    fun selectAlternative(alternative: Boolean) {
        selectedAlternative = alternative
        if (plannedPaths.isNotEmpty()) drawPlannedPaths(fitCamera = false)
    }

    fun updateJourneyPresentation(
        showAlternatives: Boolean,
        photoUris: List<String>,
        voiceCount: Int,
        replanCount: Int,
        selectedPhotoIndex: Int,
        onPhotoSelected: (Int) -> Unit,
    ) {
        showAlternativeRoutes = showAlternatives
        journeyPhotoUris = photoUris.take(8)
        journeyVoiceCount = voiceCount.coerceIn(0, 6)
        journeyReplanCount = replanCount.coerceAtLeast(0)
        selectedJourneyPhotoIndex = selectedPhotoIndex
        this.onJourneyPhotoSelected = onPhotoSelected
        if (plannedPaths.isNotEmpty()) drawPlannedPaths(fitCamera = false)
    }

    fun startGuidance() {
        if (guidanceRequested) return
        guidanceRequested = true
        uiState = uiState.copy(
            status = "正在获取当前位置…",
            instruction = "请在园区开阔处稍候",
            isGuiding = true,
            isError = false,
        )
        ensureLocationClient()?.startLocation()
    }

    fun stopGuidance() {
        guidanceRequested = false
        pendingGuidanceRoute = false
        locationClient?.stopLocation()
        uiState = uiState.copy(
            status = "已结束定位指引",
            instruction = "地图仍保留高德规划的园内预览路线",
            isGuiding = false,
            canRecenter = latestLocation != null,
            isError = false,
        )
        planPreviewRoute()
    }

    fun reportPermissionDenied() {
        guidanceRequested = false
        uiState = uiState.copy(
            status = "未获得精确位置权限",
            instruction = "可继续查看路线；园内指引需要在系统弹窗中选择“精确”位置",
            isGuiding = false,
            isError = true,
        )
    }

    fun recenter() {
        latestLocation?.let {
            map.animateCamera(
                CameraUpdateFactory.newCameraPosition(CameraPosition(it, 18f, 48f, 0f)),
            )
        }
    }

    fun replanFromCurrentLocation() {
        val location = latestLocation ?: return
        pendingGuidanceRoute = true
        requestRoute(location, PARK_DESTINATION_GCJ02, guidanceRoute = true)
    }

    fun pauseLocation() {
        locationClient?.stopLocation()
    }

    fun resumeLocationIfNeeded() {
        if (guidanceRequested) ensureLocationClient()?.startLocation()
    }

    fun destroy() {
        guidanceRequested = false
        locationClient?.stopLocation()
        locationClient?.onDestroy()
        locationClient = null
        routePolylines.forEach(Polyline::remove)
        endpointMarkers.forEach(Marker::remove)
        journeyMarkers.forEach(Marker::remove)
        locationMarker?.remove()
    }

    private fun ensureLocationClient(): AMapLocationClient? {
        locationClient?.let { return it }
        return runCatching {
            AMapLocationClient(context).also { client ->
                client.setLocationOption(
                    AMapLocationClientOption()
                        .setLocationMode(AMapLocationClientOption.AMapLocationMode.Hight_Accuracy)
                        .setInterval(2_000L)
                        .setNeedAddress(false)
                        .setOffset(true)
                        .setMockEnable(false)
                        .setLocationCacheEnable(false),
                )
                client.setLocationListener(::onLocationChanged)
                locationClient = client
            }
        }.getOrElse { error ->
            guidanceRequested = false
            uiState = uiState.copy(
                status = "定位服务启动失败",
                instruction = error.message.orEmpty(),
                isGuiding = false,
                isError = true,
            )
            null
        }
    }

    private fun onLocationChanged(location: AMapLocation?) {
        if (location == null) return
        mainHandler.post {
            if (location.errorCode != AMapLocation.LOCATION_SUCCESS) {
                uiState = uiState.copy(
                    status = "定位失败（${location.errorCode}）",
                    instruction = "请检查定位权限、GPS 与网络后重试",
                    isError = true,
                )
                return@post
            }
            val point = LatLng(location.latitude, location.longitude)
            latestLocation = point
            updateLocationMarker(point)

            val distanceToPark = AMapUtils.calculateLineDistance(point, PARK_CENTER_GCJ02)
            if (distanceToPark > PARK_GUIDANCE_RADIUS_METERS) {
                guidanceRequested = false
                locationClient?.stopLocation()
                uiState = uiState.copy(
                    status = "当前位置不在八家郊野公园附近",
                    instruction = "园内指引仅在公园约 2.5 公里范围内启用",
                    isGuiding = false,
                    canRecenter = true,
                    isError = true,
                )
                return@post
            }

            if (!pendingGuidanceRoute && uiState.status == "正在获取当前位置…") {
                pendingGuidanceRoute = true
                requestRoute(point, PARK_DESTINATION_GCJ02, guidanceRoute = true)
            } else if (uiState.isGuiding && plannedPaths.isNotEmpty()) {
                updateGuidance(point)
            }
        }
    }

    private fun updateLocationMarker(point: LatLng) {
        val marker = locationMarker
        if (marker == null) {
            locationMarker = map.addMarker(
                MarkerOptions()
                    .position(point)
                    .title("当前位置")
                    .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_AZURE))
                    .zIndex(20f),
            )
        } else {
            marker.position = point
        }
    }

    private fun requestRoute(from: LatLng, to: LatLng, guidanceRoute: Boolean) {
        val search = routeSearch ?: return
        pendingGuidanceRoute = guidanceRoute
        if (guidanceRoute) {
            uiState = uiState.copy(status = "正在从当前位置重新规划…", instruction = "", isError = false)
        } else {
            uiState = uiState.copy(status = "正在请求高德步行路线…", instruction = "", isError = false)
        }
        val fromAndTo = RouteSearchV2.FromAndTo(from.toLatLonPoint(), to.toLatLonPoint())
        val query = RouteSearchV2.WalkRouteQuery(fromAndTo).apply {
            alternativeRoute = RouteSearchV2.AlternativeRoute.ALTERNATIVE_ROUTE_TWO
            showFields = RouteSearchV2.ShowFields.ALL
        }
        search.calculateWalkRouteAsyn(query)
    }

    override fun onWalkRouteSearched(result: WalkRouteResultV2?, resultCode: Int) {
        mainHandler.post {
            if (resultCode != AMapException.CODE_AMAP_SUCCESS || result == null) {
                showRouteFailure(resultCode)
                return@post
            }
            val parsed = result.paths.orEmpty().mapNotNull { path ->
                val steps = path.steps.orEmpty().mapNotNull { step ->
                    val points = step.polyline.orEmpty().map(LatLonPoint::toLatLng)
                    if (points.isEmpty()) null else PlannedStep(
                        instruction = step.instruction?.takeIf(String::isNotBlank)
                            ?: step.action?.takeIf(String::isNotBlank)
                            ?: "沿园路继续前行",
                        points = points,
                    )
                }
                val points = path.polyline.orEmpty().map(LatLonPoint::toLatLng)
                    .ifEmpty { steps.flatMap(PlannedStep::points) }
                if (points.size < 2) null else PlannedPath(
                    points = points,
                    distanceMeters = path.distance,
                    durationSeconds = path.duration,
                    steps = steps,
                )
            }.take(2)
            if (parsed.isEmpty()) {
                showRouteFailure(resultCode)
                return@post
            }
            if (!pendingGuidanceRoute) ParkRouteCache.previewPaths = parsed
            applyPaths(parsed, fitCamera = true)
            if (guidanceRequested) latestLocation?.let(::updateGuidance)
            pendingGuidanceRoute = false
        }
    }

    private fun applyPaths(paths: List<PlannedPath>, fitCamera: Boolean) {
        plannedPaths = paths
        drawPlannedPaths(fitCamera)
    }

    private fun drawPlannedPaths(fitCamera: Boolean) {
        routePolylines.forEach(Polyline::remove)
        endpointMarkers.forEach(Marker::remove)
        journeyMarkers.forEach(Marker::remove)
        routePolylines.clear()
        endpointMarkers.clear()
        journeyMarkers.clear()
        photoMarkerIndices.clear()
        if (plannedPaths.isEmpty()) return

        val selectedIndex = if (selectedAlternative && plannedPaths.size > 1) 1 else 0
        plannedPaths.forEachIndexed { index, path ->
            val selected = index == selectedIndex
            if (!showAlternativeRoutes && !selected) return@forEachIndexed
            routePolylines += map.addPolyline(
                PolylineOptions()
                    .addAll(path.points)
                    .width(if (selected) 14f else 8f)
                    .color(
                        when {
                            selected && (index == 0 || !showAlternativeRoutes) -> AndroidColor.rgb(216, 255, 47)
                            selected -> AndroidColor.rgb(255, 116, 102)
                            index == 0 -> AndroidColor.argb(165, 177, 206, 59)
                            else -> AndroidColor.argb(165, 211, 104, 94)
                        },
                    )
                    .zIndex(if (selected) 8f else 5f)
                    .geodesic(false),
            )
        }
        val selectedPath = plannedPaths[selectedIndex]
        endpointMarkers += map.addMarker(
            MarkerOptions()
                .position(selectedPath.points.first())
                .title(if (guidanceRequested) "当前位置附近" else "园内路线起点")
                .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_GREEN)),
        )
        drawJourneyMarkers(selectedPath)
        endpointMarkers += map.addMarker(
            MarkerOptions()
                .position(selectedPath.points.last())
                .title("儿童活动区外环")
                .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_RED)),
        )

        val firstInstruction = selectedPath.steps.firstOrNull()?.instruction.orEmpty()
        uiState = uiState.copy(
            status = if (guidanceRequested) "正在按高德路线指引" else "路线来自高德实时步行规划",
            summary = routeSummary(selectedPath),
            instruction = if (guidanceRequested) firstInstruction else "拖动或缩放地图，路线会与道路同步移动",
            isGuiding = guidanceRequested,
            canRecenter = latestLocation != null,
            isError = false,
        )
        if (fitCamera) fitRoute(selectedPath.points)
    }

    private data class JourneyMarkerSpec(
        val kind: String,
        val photoUri: String? = null,
        val photoIndex: Int = -1,
    )

    private fun drawJourneyMarkers(path: PlannedPath) {
        if (path.points.size < 2) return
        val specs = buildList {
            journeyPhotoUris.forEachIndexed { index, uri ->
                add(JourneyMarkerSpec(kind = "photo", photoUri = uri, photoIndex = index))
            }
            repeat(journeyVoiceCount) { add(JourneyMarkerSpec(kind = "voice")) }
            if (journeyReplanCount > 0) add(JourneyMarkerSpec(kind = "replan"))
        }
        specs.forEachIndexed { index, spec ->
            val fraction = if (specs.size == 1) .50f else .12f + .76f * index / (specs.size - 1f)
            val marker = map.addMarker(
                MarkerOptions()
                    .position(pointAtFraction(path.points, fraction))
                    .anchor(.5f, .5f)
                    .zIndex(16f + index)
                    .title(
                        when (spec.kind) {
                            "photo" -> "第 ${spec.photoIndex + 1} 个照片发现"
                            "voice" -> "沿途语音发现"
                            else -> "路线调整节点"
                        },
                    )
                    .icon(
                        when (spec.kind) {
                            "photo" -> photoMarkerIcon(
                                spec.photoUri.orEmpty(),
                                spec.photoIndex,
                                selected = spec.photoIndex == selectedJourneyPhotoIndex,
                            )
                            "voice" -> textMarkerIcon("语", AndroidColor.rgb(78, 113, 139))
                            else -> textMarkerIcon("改", AndroidColor.rgb(184, 107, 44))
                        },
                    ),
            )
            journeyMarkers += marker
            if (spec.kind == "photo") photoMarkerIndices[marker.id] = spec.photoIndex
        }
    }

    private fun pointAtFraction(points: List<LatLng>, fraction: Float): LatLng {
        if (points.size < 2) return points.first()
        val segmentLengths = points.zipWithNext { a, b -> AMapUtils.calculateLineDistance(a, b) }
        val target = segmentLengths.sum() * fraction.coerceIn(0f, 1f)
        var walked = 0f
        segmentLengths.forEachIndexed { index, length ->
            if (walked + length >= target && length > 0f) {
                val local = ((target - walked) / length).coerceIn(0f, 1f)
                val start = points[index]
                val end = points[index + 1]
                return LatLng(
                    start.latitude + (end.latitude - start.latitude) * local,
                    start.longitude + (end.longitude - start.longitude) * local,
                )
            }
            walked += length
        }
        return points.last()
    }

    private fun photoMarkerIcon(rawUri: String, index: Int, selected: Boolean) = runCatching {
        val source = decodeMarkerBitmap(rawUri) ?: error("photo unavailable")
        val density = context.resources.displayMetrics.density
        val size = ((if (selected) 58f else 50f) * density).roundToInt().coerceAtLeast(64)
        val output = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
        val canvas = AndroidCanvas(output)
        val center = size / 2f
        val radius = size * .39f
        canvas.drawCircle(center, center, size * .48f, Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = if (selected) AndroidColor.rgb(49, 94, 75) else AndroidColor.WHITE
        })
        val shader = BitmapShader(source, Shader.TileMode.CLAMP, Shader.TileMode.CLAMP)
        val scale = maxOf(size * .78f / source.width, size * .78f / source.height)
        shader.setLocalMatrix(Matrix().apply {
            setScale(scale, scale)
            postTranslate((size - source.width * scale) / 2f, (size - source.height * scale) / 2f)
        })
        canvas.drawCircle(center, center, radius, Paint(Paint.ANTI_ALIAS_FLAG).apply { this.shader = shader })
        canvas.drawCircle(center, center, radius, Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = AndroidColor.WHITE
            style = Paint.Style.STROKE
            strokeWidth = density * 2.2f
        })
        val badgeRadius = density * 8f
        val badgeX = size - badgeRadius * 1.05f
        val badgeY = badgeRadius * 1.05f
        canvas.drawCircle(badgeX, badgeY, badgeRadius, Paint(Paint.ANTI_ALIAS_FLAG).apply { color = AndroidColor.rgb(184, 107, 44) })
        canvas.drawText(
            (index + 1).toString(),
            badgeX,
            badgeY + density * 3.4f,
            Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = AndroidColor.WHITE
                textAlign = Paint.Align.CENTER
                textSize = density * 9f
                typeface = Typeface.DEFAULT_BOLD
            },
        )
        BitmapDescriptorFactory.fromBitmap(output)
    }.getOrElse { BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_GREEN) }

    private fun decodeMarkerBitmap(rawUri: String): Bitmap? {
        val uri = Uri.parse(rawUri)
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        context.contentResolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, bounds) }
        var sample = 1
        while (bounds.outWidth / sample > 320 || bounds.outHeight / sample > 320) sample *= 2
        val options = BitmapFactory.Options().apply { inSampleSize = sample }
        return context.contentResolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, options) }
    }

    private fun textMarkerIcon(label: String, backgroundColor: Int) = run {
        val density = context.resources.displayMetrics.density
        val size = (40f * density).roundToInt().coerceAtLeast(56)
        val output = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
        val canvas = AndroidCanvas(output)
        val center = size / 2f
        canvas.drawCircle(center, center, size * .43f, Paint(Paint.ANTI_ALIAS_FLAG).apply { color = AndroidColor.WHITE })
        canvas.drawCircle(center, center, size * .36f, Paint(Paint.ANTI_ALIAS_FLAG).apply { color = backgroundColor })
        canvas.drawText(
            label,
            center,
            center + density * 5f,
            Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = AndroidColor.WHITE
                textAlign = Paint.Align.CENTER
                textSize = density * 14f
                typeface = Typeface.DEFAULT_BOLD
            },
        )
        BitmapDescriptorFactory.fromBitmap(output)
    }

    private fun updateGuidance(location: LatLng) {
        val selectedIndex = if (selectedAlternative && plannedPaths.size > 1) 1 else 0
        val path = plannedPaths.getOrNull(selectedIndex) ?: return
        val nearestIndex = path.points.indices.minByOrNull { index ->
            AMapUtils.calculateLineDistance(location, path.points[index])
        } ?: 0
        val offRouteDistance = AMapUtils.calculateLineDistance(location, path.points[nearestIndex])
        val destinationDistance = AMapUtils.calculateLineDistance(location, path.points.last())
        if (destinationDistance <= 25f) {
            uiState = uiState.copy(
                status = "已到达路线终点",
                summary = "距离终点约 ${destinationDistance.toInt()} 米",
                instruction = "请留意现场道路和公园标识",
                canRecenter = true,
                isError = false,
            )
            return
        }

        val remaining = remainingDistance(location, path.points, nearestIndex)
        val nearestStep = path.steps.minByOrNull { step ->
            step.points.minOfOrNull { point -> AMapUtils.calculateLineDistance(location, point) }
                ?: Float.MAX_VALUE
        }
        uiState = uiState.copy(
            status = if (offRouteDistance > 45f) "可能偏离路线，请核对现场道路" else "正在按高德路线指引",
            summary = "剩余约 ${formatDistance(remaining)}",
            instruction = nearestStep?.instruction ?: "沿园内步行路线继续前行",
            canRecenter = true,
            isError = offRouteDistance > 45f,
        )
    }

    private fun showRouteFailure(code: Int) {
        pendingGuidanceRoute = false
        val fallback = fallbackPath()
        plannedPaths = listOf(fallback)
        drawFallbackPath(fallback)
        uiState = uiState.copy(
            status = "高德步行规划失败（$code）",
            summary = routeSummary(fallback),
            instruction = "当前仅显示地理坐标预设线，请检查网络或 Key 服务权限后重试",
            isGuiding = guidanceRequested,
            canRecenter = latestLocation != null,
            isError = true,
        )
    }

    private fun drawFallbackPath(path: PlannedPath) {
        routePolylines.forEach(Polyline::remove)
        routePolylines.clear()
        routePolylines += map.addPolyline(
            PolylineOptions()
                .addAll(path.points)
                .width(10f)
                .color(AndroidColor.rgb(158, 167, 162))
                .setDottedLine(true),
        )
        fitRoute(path.points)
    }

    private fun fallbackPath(): PlannedPath {
        val converter = CoordinateConverter(context).from(CoordinateConverter.CoordType.GPS)
        val points = PARK_ROUTE_WGS84.map { converter.coord(it).convert() }
        val distance = points.zipWithNext().sumOf { (from, to) ->
            AMapUtils.calculateLineDistance(from, to).toDouble()
        }.toFloat()
        return PlannedPath(points, distance, (distance / 1.2f).toLong(), emptyList())
    }

    private fun fitRoute(points: List<LatLng>) {
        if (points.isEmpty()) return
        val bounds = LatLngBounds.Builder().apply { points.forEach(::include) }.build()
        runCatching {
            map.animateCamera(
                CameraUpdateFactory.newLatLngBounds(bounds, 90),
                object : AMap.CancelableCallback {
                    override fun onFinish() {
                        val current = map.cameraPosition
                        map.animateCamera(
                            CameraUpdateFactory.newCameraPosition(
                                CameraPosition(current.target, current.zoom, 46f, 12f),
                            ),
                        )
                    }

                    override fun onCancel() = Unit
                },
            )
        }
    }

    override fun onBusRouteSearched(result: BusRouteResultV2?, resultCode: Int) = Unit
    override fun onDriveRouteSearched(result: DriveRouteResultV2?, resultCode: Int) = Unit
    override fun onRideRouteSearched(result: RideRouteResultV2?, resultCode: Int) = Unit
}

private fun configureBaseMap(context: Context, map: AMap, gesturesEnabled: Boolean) {
    map.uiSettings.apply {
        isZoomControlsEnabled = false
        isCompassEnabled = false
        isScaleControlsEnabled = true
        isMyLocationButtonEnabled = false
        isScrollGesturesEnabled = gesturesEnabled
        isZoomGesturesEnabled = gesturesEnabled
        isRotateGesturesEnabled = gesturesEnabled
        isTiltGesturesEnabled = gesturesEnabled
    }
    map.mapType = AMap.MAP_TYPE_NORMAL
    map.showBuildings(true)
    map.showMapText(true)
    map.setRoadArrowEnable(true)
    // 免费 GeoHUB 方案：离线样式打包进 APK 即可，不依赖付费的 Style ID 在线调用。
    // 若离线文件不存在，则回退到可选的在线 Style ID，再回退到高德默认底图。
    val offlineStyle = context.readAssetOrNull(OFFLINE_STYLE_DATA)
    val customStyle = when {
        offlineStyle != null -> CustomMapStyleOptions()
            .setEnable(true)
            .setStyleData(offlineStyle)
            .apply {
                context.readAssetOrNull(OFFLINE_STYLE_EXTRA)?.let(::setStyleExtraData)
                context.readAssetOrNull(OFFLINE_STYLE_TEXTURES)?.let(::setStyleTextureData)
            }

        BuildConfig.AMAP_STYLE_ID.isNotBlank() -> CustomMapStyleOptions()
            .setEnable(true)
            .setStyleId(BuildConfig.AMAP_STYLE_ID)

        else -> null
    }
    customStyle?.let(map::setCustomMapStyle)
    map.moveCamera(
        CameraUpdateFactory.newCameraPosition(
            CameraPosition(PARK_CENTER_GCJ02, 17.1f, 46f, 12f),
        ),
    )
}

private fun Context.readAssetOrNull(path: String): ByteArray? = runCatching {
    assets.open(path).use { it.readBytes() }
}.getOrNull()

private fun LatLng.toLatLonPoint(): LatLonPoint = LatLonPoint(latitude, longitude)
private fun LatLonPoint.toLatLng(): LatLng = LatLng(latitude, longitude)

private fun routeSummary(path: PlannedPath): String {
    val minutes = ceil(path.durationSeconds.coerceAtLeast(1L) / 60.0).toInt()
    return "${formatDistance(path.distanceMeters)} · 约 $minutes 分钟"
}

private fun formatDistance(meters: Float): String =
    if (meters < 1_000f) "${meters.toInt()} 米" else String.format(Locale.CHINA, "%.1f 公里", meters / 1_000f)

private fun remainingDistance(location: LatLng, points: List<LatLng>, nearestIndex: Int): Float {
    var distance = AMapUtils.calculateLineDistance(location, points[nearestIndex])
    for (index in nearestIndex until points.lastIndex) {
        distance += AMapUtils.calculateLineDistance(points[index], points[index + 1])
    }
    return distance
}

/**
 * 高德坐标系中的园内起点、终点和中心点。路线几何不再由这些常量决定；它们仅作为真实算路端点。
 * 参与者开启指引后，起点会替换为设备的高德定位结果。
 */
private val PARK_START_GCJ02 = LatLng(40.01332, 116.33224)
private val PARK_DESTINATION_GCJ02 = LatLng(40.01896, 116.33537)
private val PARK_CENTER_GCJ02 = LatLng(40.01614, 116.33381)

/** 仅在在线算路失败时使用的经纬度降级线；显示前转换为 GCJ-02。 */
private val PARK_ROUTE_WGS84 = listOf(
    LatLng(40.01192, 116.32606),
    LatLng(40.01312, 116.32662),
    LatLng(40.01428, 116.32755),
    LatLng(40.01546, 116.32724),
    LatLng(40.01662, 116.32815),
    LatLng(40.01758, 116.32920),
)
