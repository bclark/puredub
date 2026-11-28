package com.puredub.android

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.lifecycle.viewmodel.compose.viewModel
import android.view.WindowManager
import android.content.Intent
import android.net.Uri
import com.puredub.android.ui.theme.PureDubTheme

class MainActivity : ComponentActivity() {
  private var model: PureDubViewModel? = null
  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)

    enableEdgeToEdge()
    setContent {
      val vm: PureDubViewModel = viewModel()
      model = vm
      val importWav = androidx.activity.compose.rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri -> uri?.let(vm::importWav) }
      val openProject = androidx.activity.compose.rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri -> uri?.let(vm::openProject) }
      val saveProject = androidx.activity.compose.rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/json")) { uri -> uri?.let(vm::saveProject) }
      val export = androidx.activity.compose.rememberLauncherForActivityResult(ActivityResultContracts.OpenDocumentTree()) { uri -> uri?.let(vm::exportTo) }
      DisposableEffect(vm.state.busy) {
        if (vm.state.busy != null) window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        onDispose { window.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON) }
      }
      PureDubTheme {
        Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
          PureDubScreen(vm,
            onImport = { importWav.launch(arrayOf("audio/*", "application/octet-stream")) },
            onOpen = { openProject.launch(arrayOf("application/json", "text/plain", "application/octet-stream")) },
            onSave = { saveProject.launch("side-${vm.state.project.side.lowercase()}.puredub.json") },
            onExport = { export.launch(null) },
            onOpenExport = { runCatching { startActivity(Intent(Intent.ACTION_VIEW).setDataAndType(Uri.parse(vm.state.exportFolder), "vnd.android.document/directory").addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)) } }
          )
        }
      }
    }
  }
  override fun onStop() { model?.stopPlayback(); super.onStop() }
}
