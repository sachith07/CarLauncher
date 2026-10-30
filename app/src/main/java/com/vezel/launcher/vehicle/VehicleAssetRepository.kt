package com.vezel.launcher.vehicle

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.os.Handler
import android.os.Looper
import android.util.LruCache
import java.util.concurrent.Executors
import java.util.concurrent.atomic.AtomicInteger

/**
 * Loads and caches vehicle WebP bitmaps off the UI thread.
 * Callers keep the previous image visible until the callback delivers a new bitmap
 * (avoids white flash on color switch).
 */
class VehicleAssetRepository(
    context: Context
) {

    private val appContext = context.applicationContext
    private val paintColors = PaintColorRepository()
    private val mainHandler = Handler(Looper.getMainLooper())
    private val decodeExecutor = Executors.newSingleThreadExecutor { r ->
        Thread(r, "vezel-vehicle-decode").apply { isDaemon = true }
    }

    private val cache: LruCache<String, Bitmap> = object : LruCache<String, Bitmap>(
        // ~6 × ~720×360 ARGB ≈ ~6 MiB; allow headroom for decoded copies
        ((Runtime.getRuntime().maxMemory() / 1024L) / 16L).toInt().coerceIn(4 * 1024, 12 * 1024)
    ) {
        override fun sizeOf(key: String, value: Bitmap): Int =
            value.byteCount / 1024
    }

    private val generation = AtomicInteger(0)

    fun paintFor(key: String): PaintColor = paintColors.find(key)

    fun availableColors(): List<PaintColor> = paintColors.all()

    fun getCached(key: String): Bitmap? {
        val normalized = PaintColor.normalizeKey(key)
        synchronized(cache) {
            return cache.get(normalized)
        }
    }

    /**
     * Decode [key] off the UI thread. Invokes [onReady] on the main thread only when
     * the request is still current (latest [requestId] wins).
     *
     * @return request id for optional cancellation tracking
     */
    fun loadAsync(key: String, onReady: (PaintColor, Bitmap) -> Unit): Int {
        val paint = paintColors.find(key)
        val requestId = generation.incrementAndGet()
        val cached = getCached(paint.key)
        if (cached != null && !cached.isRecycled) {
            mainHandler.post {
                if (requestId == generation.get()) {
                    onReady(paint, cached)
                }
            }
            return requestId
        }

        decodeExecutor.execute {
            val bitmap = decodeDrawable(paint.drawableRes) ?: return@execute
            synchronized(cache) {
                cache.put(paint.key, bitmap)
            }
            mainHandler.post {
                if (requestId == generation.get()) {
                    onReady(paint, bitmap)
                }
            }
        }
        return requestId
    }

    /** Warm the cache for every paint so first switches stay off the critical path. */
    fun preloadAll() {
        decodeExecutor.execute {
            for (paint in paintColors.all()) {
                if (getCached(paint.key) != null) continue
                val bitmap = decodeDrawable(paint.drawableRes) ?: continue
                synchronized(cache) {
                    cache.put(paint.key, bitmap)
                }
            }
        }
    }

    private fun decodeDrawable(drawableRes: Int): Bitmap? {
        val opts = BitmapFactory.Options().apply {
            inPreferredConfig = Bitmap.Config.ARGB_8888
            inScaled = false
        }
        return try {
            BitmapFactory.decodeResource(appContext.resources, drawableRes, opts)
        } catch (_: OutOfMemoryError) {
            null
        }
    }

    fun shutdown() {
        decodeExecutor.shutdownNow()
    }
}
