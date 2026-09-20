package cn.tsinghua.sagemotion.data

import cn.tsinghua.sagemotion.model.*
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Test

class JourneyDataTest {
    private val points = listOf(GeoPoint(40.0, 116.0), GeoPoint(40.002, 116.003))
    private fun route(id: String, meters: Float) = ParkRoutePlan(id, "路线 $id", points, meters, 120)
    private val result = AiTaskResult("old", "old", null, "采用", listOf("unverified shade"))

    @Test fun routeChoiceBindsToGeometryInsteadOfReturnedArrayOrder() {
        val bound = bindRouteResult(result, listOf(route("long", 900f), route("short", 200f)), null, false)
        assertEquals("short", bound.recommendedRouteId)
        assertEquals("long", bound.alternativeRouteId)
        assertEquals("200 米", bound.metrics[1].title)
        assertFalse(bound.evidence.contains("unverified shade"))
    }

    @Test fun singleRouteNeverPretendsToHaveAnAlternativeOrReplan() {
        val only = route("same", 250f)
        assertNull(bindRouteResult(result, listOf(only, only), null, false).alternativeRouteId)
        val adjust = bindRouteResult(result, listOf(only), "same", true)
        assertNull(adjust.recommendedRouteId)
        assertTrue(adjust.metrics.isEmpty())
    }

    @Test fun replanKeepsTheAdoptedRouteAsTheExplicitAlternative() {
        val bound = bindRouteResult(result, listOf(route("current", 100f), route("new", 500f)), "current", true)
        assertEquals("new", bound.recommendedRouteId)
        assertEquals("current", bound.alternativeRouteId)
    }

    @Test fun geometryIdIsStableButChangesWithPath() {
        assertEquals(ParkRoutePlan.stableId(points), ParkRoutePlan.stableId(points.toList()))
        assertNotEquals(ParkRoutePlan.stableId(points), ParkRoutePlan.stableId(points.reversed()))
    }

    @Test fun circleUsesCenterCropImageCoordinates() {
        val region = ImageRegionMapping.fromCircle(100f, 200f, 50f, 200f, 400f, 800, 400)!!
        assertEquals(.4375f, region.left, .0001f)
        assertEquals(.5625f, region.right, .0001f)
        assertEquals(.375f, region.top, .0001f)
        assertEquals(.625f, region.bottom, .0001f)
    }

    @Test fun circleClipsAtImageEdgeAndRejectsInvalidInput() {
        val region = ImageRegionMapping.fromCircle(0f, 0f, 50f, 200f, 400f, 200, 400)!!
        assertEquals(0f, region.left)
        assertEquals(.25f, region.right)
        assertNull(ImageRegionMapping.fromCircle(Float.NaN, 0f, 50f, 200f, 400f, 200, 400))
        assertNull(ImageRegionMapping.fromCircle(-200f, -200f, 10f, 200f, 400f, 200, 400))
    }

    @Test fun trackRejectsStaleInaccurateAndDuplicateFixes() {
        val now = 100_000L
        val fix = GeoFix(points[0], now, 8f)
        val state = JourneySpatialState().record(fix, now)
        assertEquals(1, state.track.size)
        assertEquals(state, state.record(fix.copy(timestamp = now - 70_000), now))
        assertEquals(state, state.record(fix.copy(accuracyMeters = 100f), now))
        assertEquals(state, state.record(fix.copy(timestamp = now + 1000), now + 1000))
        assertEquals(2, state.record(fix.copy(point = points[1], timestamp = now + 1000), now + 1000).track.size)
    }

    @Test fun realPhotoWithoutLabelsDoesNotReturnTheSampleFlower() = runBlocking {
        val events = MockAiDemoApi(waitFor = {}).runTask(AiTaskRequest(
            ExperimentScenario.VISUAL, "这是什么？", hasCapturedPhoto = true,
        )).toList()
        val output = events.filterIsInstance<AiTaskEvent.Completed>().single().result
        assertFalse(output.summary.contains("月季"))
        assertTrue(output.summary.contains("没有") || output.summary.contains("未"))
    }

    @Test fun gpsGapsAreNotDrawnAsTravelledSegments() {
        val p = points[0]
        val fixes = listOf(GeoFix(p, 1000, 5f), GeoFix(p, 2000, 5f),
            GeoFix(p, 200_000, 5f), GeoFix(points[1], 201_000, 5f))
        assertEquals(listOf(2, 1, 1), fixes.continuousSegments().map { it.size })
    }

    @Test fun onlineRouteNameRequiresAnAdoptedSdkPlan() {
        assertEquals("尚未采纳路线", ExperimentUiState().activeRouteName)
        assertEquals("路线 real", ExperimentUiState(spatial = JourneySpatialState(
            routes = listOf(route("real", 200f)), activeRouteId = "real",
        )).activeRouteName)
    }
}
