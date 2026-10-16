package dev.milkei.audiofocusmixer.data

import android.content.Context
import android.util.LruCache
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.core.graphics.drawable.toBitmap
import dev.milkei.audiofocusmixer.core.ConcurrencyGate
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlin.math.roundToInt

/**
 * Bounded cache of downscaled installed-app icons. Never decode icons during composition.
 * Two concurrent loads prevent a fast fling from starting many bitmap conversions at once.
 */
internal object AppIconRepository {
    private const val MAX_CACHE_BYTES = 2 * 1024 * 1024
    private const val ICON_DP = 40
    private val gate = ConcurrencyGate(maxParallel = 2)

    private val cache = object : LruCache<String, ImageBitmap>(MAX_CACHE_BYTES) {
        override fun sizeOf(key: String, value: ImageBitmap): Int =
            (value.width * value.height * 4).coerceAtLeast(1)
    }

    suspend fun load(context: Context, packageName: String): ImageBitmap? {
        val sizePx = (ICON_DP * context.resources.displayMetrics.density)
            .roundToInt()
            .coerceIn(40, 160)
        val key = "$packageName@$sizePx"
        cache.get(key)?.let { return it }

        return gate.withSlot {
            cache.get(key) ?: withContext(Dispatchers.IO) {
                runCatching {
                    context.applicationContext.packageManager
                        .getApplicationIcon(packageName)
                        .toBitmap(width = sizePx, height = sizePx)
                        .asImageBitmap()
                }.getOrNull()
            }?.also { bitmap ->
                cache.put(key, bitmap)
            }
        }
    }
}
