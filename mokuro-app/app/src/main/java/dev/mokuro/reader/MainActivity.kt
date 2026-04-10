package dev.mokuro.reader

import android.net.Uri
import android.os.Bundle
import android.webkit.ValueCallback
import android.webkit.WebChromeClient
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.activity.result.contract.ActivityResultContracts.GetMultipleContents
import androidx.activity.result.contract.ActivityResultContracts.GetContent
import androidx.activity.result.contract.ActivityResultContracts.OpenDocumentTree
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.documentfile.provider.DocumentFile

/**
 * Single-Activity shell that hosts mokuro-reader.html in a full-screen WebView.
 *
 * File-chooser routing:
 *  - Single-file inputs (e.g. JMdict JSON)   → system file picker (GetContent)
 *  - Multi-file / webkitdirectory inputs      → SAF folder picker (OpenDocumentTree)
 *    The folder tree is walked one level deep and all child URIs are returned
 *    so the JS FileReader API can read them directly.
 */
class MainActivity : AppCompatActivity() {

    private lateinit var webView: WebView
    private var fileCallback: ValueCallback<Array<Uri>>? = null

    // Single-file picker (JMdict JSON etc.)
    private val singlePicker = registerForActivityResult(GetContent()) { uri ->
        fileCallback?.onReceiveValue(if (uri != null) arrayOf(uri) else emptyArray())
        fileCallback = null
    }

    // Multi-file picker used as fallback when folder selection fails
    private val multiPicker = registerForActivityResult(GetMultipleContents()) { uris ->
        fileCallback?.onReceiveValue(uris.toTypedArray())
        fileCallback = null
    }

    // Folder picker (webkitdirectory) — returns all direct children of the chosen dir
    private val folderPicker = registerForActivityResult(OpenDocumentTree()) { treeUri ->
        if (treeUri == null) {
            fileCallback?.onReceiveValue(emptyArray())
            fileCallback = null
            return@registerForActivityResult
        }
        // Persist permission so the JS FileReader can access the URIs
        contentResolver.takePersistableUriPermission(
            treeUri,
            android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION,
        )
        val tree = DocumentFile.fromTreeUri(this, treeUri)
        val uris = tree?.listFiles()
            ?.filter { it.isFile && it.canRead() }
            ?.map { it.uri }
            ?.toTypedArray()
            ?: emptyArray()
        fileCallback?.onReceiveValue(uris)
        fileCallback = null
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Edge-to-edge, hide system bars so the WebView fills the screen
        WindowCompat.setDecorFitsSystemWindows(window, false)

        webView = WebView(this).also { wv ->
            wv.settings.apply {
                javaScriptEnabled = true
                domStorageEnabled = true
                databaseEnabled = true
                allowFileAccess = true
                allowContentAccess = true
                mixedContentMode = WebSettings.MIXED_CONTENT_ALWAYS_ALLOW
                useWideViewPort = true
                loadWithOverviewMode = true
                setSupportZoom(false)
                builtInZoomControls = false
            }

            wv.webViewClient = WebViewClient()

            wv.webChromeClient = object : WebChromeClient() {
                override fun onShowFileChooser(
                    webView: WebView,
                    filePathCallback: ValueCallback<Array<Uri>>,
                    fileChooserParams: FileChooserParams,
                ): Boolean {
                    // Cancel any in-progress chooser
                    fileCallback?.onReceiveValue(emptyArray())
                    fileCallback = filePathCallback

                    val isMultiple = fileChooserParams.mode == FileChooserParams.MODE_OPEN_MULTIPLE
                    val accept = fileChooserParams.acceptTypes.joinToString(",")

                    when {
                        // webkitdirectory or generic multi-select → folder picker
                        isMultiple && (accept.isBlank() || accept == "*/*") ->
                            folderPicker.launch(null)

                        // Multiple files with specific type (rare, fallback)
                        isMultiple ->
                            multiPicker.launch("*/*")

                        // Single file (JMdict JSON etc.)
                        else ->
                            singlePicker.launch(if (accept.isNotBlank()) accept else "*/*")
                    }
                    return true
                }
            }

            wv.loadUrl("file:///android_asset/mokuro-reader.html")
        }

        setContentView(webView)

        // Hide status bar and nav bar for immersive reading
        WindowInsetsControllerCompat(window, webView).apply {
            hide(WindowInsetsCompat.Type.systemBars())
            systemBarsBehavior =
                WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        }
    }

    @Deprecated("Use OnBackPressedDispatcher")
    override fun onBackPressed() {
        if (webView.canGoBack()) webView.goBack() else super.onBackPressed()
    }
}
