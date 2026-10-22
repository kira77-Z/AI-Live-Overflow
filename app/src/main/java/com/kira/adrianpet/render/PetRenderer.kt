package com.kira.adrianpet.render

import android.annotation.SuppressLint
import android.graphics.Color
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient

@SuppressLint("SetJavaScriptEnabled")
class PetRenderer(
    private val webView: WebView,
    private val onReady: () -> Unit = {},
) {
    private var ready = false

    init {
        webView.setBackgroundColor(Color.TRANSPARENT)
        webView.isVerticalScrollBarEnabled = false
        webView.isHorizontalScrollBarEnabled = false
        webView.settings.apply {
            // JavaScript drives only our bundled animation engine. Network access and
            // Android bridges stay disabled, and all dynamic text is JSON + textContent.
            javaScriptEnabled = true
            allowFileAccess = true
            allowContentAccess = false
            blockNetworkLoads = true
            cacheMode = WebSettings.LOAD_NO_CACHE
        }
        webView.webViewClient = object : WebViewClient() {
            override fun onPageFinished(view: WebView?, url: String?) {
                if (url != ASSET_URL) return
                ready = true
                onReady()
            }
        }
        webView.loadUrl(ASSET_URL)
    }

    fun render(event: RenderEvent): Boolean {
        if (!ready) return false
        webView.evaluateJavascript("window.petEngine.render(${event.toJson()})", null)
        return true
    }

    companion object {
        const val ASSET_URL = "file:///android_asset/pet/index.html"
    }
}
