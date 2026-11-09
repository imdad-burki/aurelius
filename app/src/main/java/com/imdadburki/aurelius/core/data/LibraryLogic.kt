package com.imdadburki.aurelius.core.data

/** Pure toggle decision for favorites (unit-tested): add when not already a favorite. */
fun shouldAddFavorite(currentFavoriteIds: Collection<String>, mediaId: String): Boolean =
    !currentFavoriteIds.contains(mediaId)

/** Positions for appending [count] songs after [maxPosition] (-1 when empty). */
fun appendPositions(maxPosition: Int, count: Int): List<Int> =
    (1..count).map { maxPosition + it }
