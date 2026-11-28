package com.puredub.android

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.performScrollToNode
import androidx.compose.ui.test.hasContentDescription
import androidx.lifecycle.ViewModelProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.json.JSONObject
import java.io.File
import android.net.Uri

@RunWith(AndroidJUnit4::class)
class WorkbenchTest {
    @get:Rule val compose = createAndroidComposeRule<MainActivity>()

    @Test fun syntheticProjectCrossesJniEditsRecreatesAndExportsVerifiedFlacs() {
        val vm = ViewModelProvider(compose.activity)[PureDubViewModel::class.java]
        compose.waitUntil(15000) { vm.state.busy == null }
        compose.runOnUiThread { vm.demo() }
        compose.waitUntil(20000) { vm.state.busy == null && vm.state.audio != null }
        assertEquals(2, vm.state.audio!!.channels)
        assertEquals(24, vm.state.audio!!.bits)
        compose.onNodeWithText("Tracks", useUnmergedTree = true).performClick()
        compose.onNodeWithText("Side workbench").assertIsDisplayed()
        compose.onNodeWithTag("tracks-list").performScrollToNode(hasContentDescription("Find nearby quiet within eight seconds"))
        compose.onNodeWithContentDescription("Find nearby quiet within eight seconds").assertIsDisplayed().performClick()
        assertFalse(vm.state.quietSnap)
        compose.onNodeWithContentDescription("Find nearby quiet within eight seconds").performClick()
        assertTrue(vm.state.quietSnap)
        compose.runOnUiThread { vm.seek(1.25); vm.setStart() }
        assertEquals("0:01.250", vm.state.project.tracks[0].start)
        compose.runOnUiThread { vm.suggest() }
        compose.waitUntil(20000) { vm.state.busy == null }
        assertEquals("0:00.000", vm.state.project.tracks[0].start)
        assertTrue(parseTime(vm.state.project.tracks[1].start)!! in 3.5..4.5)
        val original = vm.state.project
        compose.activityRule.scenario.recreate()
        compose.waitForIdle()
        val recreated = ViewModelProvider(compose.activity)[PureDubViewModel::class.java]
        assertEquals(original, recreated.state.project)
        val saved = File(compose.activity.cacheDir, "instrumented-project.json")
        compose.runOnUiThread { recreated.saveProject(Uri.fromFile(saved)) }
        compose.waitUntil(20000) { recreated.state.busy == null && saved.exists() }
        compose.runOnUiThread { recreated.newProject() }
        compose.waitUntil(20000) { recreated.state.busy == null && recreated.state.audio == null }
        compose.runOnUiThread { recreated.openProject(Uri.fromFile(saved)) }
        compose.waitUntil(20000) { recreated.state.busy == null && recreated.state.audio != null }
        assertEquals(original, recreated.state.project)
        assertEquals("Synthetic stereo demo.wav", recreated.state.sourceName)
        saved.delete()
        compose.onNodeWithText("Export", useUnmergedTree = true).performClick()
        compose.onNodeWithText("Ready for your library").assertIsDisplayed()
        val root = File(compose.activity.cacheDir, "instrumented-export-${System.nanoTime()}").also { it.mkdirs() }
        try {
            val result = NativeCore.call("export", "project" to recreated.state.project.json(), "parent" to root.absolutePath) as JSONObject
            assertEquals(2, result.getInt("tracks"))
            assertEquals(3, File(result.getString("folder")).listFiles()!!.size)
            assertTrue(File(original.source).exists())
        } finally { root.deleteRecursively() }
    }
}
