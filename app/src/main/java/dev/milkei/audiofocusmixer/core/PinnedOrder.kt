package dev.milkei.audiofocusmixer.core

/**
 * Stable partition: tracked/verified enabled apps come first.
 * Relative ordering within both groups stays exactly as supplied.
 * This function is called only when the UI's pinned snapshot changes,
 * not when an individual switch updates.
 */
internal fun <T> pinnedFirstStable(
    items: List<T>,
    pinned: Set<String>,
    key: (T) -> String,
): List<T> {
    if (items.isEmpty() || pinned.isEmpty()) return items
    val enabled = ArrayList<T>()
    val remaining = ArrayList<T>()
    for (item in items) {
        if (key(item) in pinned) enabled.add(item) else remaining.add(item)
    }
    return enabled + remaining
}
