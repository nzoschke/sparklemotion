package baaahs.ui.gridlayout

import baaahs.replaceAll
import baaahs.util.Logger
import kotlin.math.max
import kotlin.math.min

data class Layout(val items: List<LayoutItem> = emptyList()) {
    val size: Int get() = items.size

    operator fun get(index: Int): LayoutItem = items[index]

    /**
     * Return the bottom coordinate of the layout.
     *
     * @param  {Array} layout Layout array.
     * @return {Number}       Bottom coordinate.
     */
    fun bottom(): Int {
        var max = 0
        var bottomY: Int
        val len = items.size
        for (i in 0 until len) {
            bottomY = items[i].y + items[i].h
            if (bottomY > max) max = bottomY
        }
        return max
    }

    private fun updateLayout(updateItem: LayoutItem): Layout =
        Layout(items.map { if (it.i == updateItem.i) updateItem else it })

    /**
     * Given a layout, compact it. This involves going down each y coordinate and removing gaps
     * between items.
     *
     * Does not modify layout items (clones). Creates a new layout array.
     *
     * @param  {Array} layout Layout.
     * @param  {Boolean} verticalCompact Whether or not to compact the layout
     *   vertically.
     * @return {Array}       Compacted Layout.
     */
    fun compact(compactType: CompactType, cols: Int): Layout {
        // Statics go in the compareWith array right away so items flow around them.
        val compareWith = getStatics().toMutableList()
        // We go through the items by row and column.
        val sorted = sortLayoutItems(compactType)
        // Holding for new items.
        val out = MutableList<LayoutItem?>(items.size) { null }

        val len = sorted.size
        for (i in 0 until len) {
            var l = sorted[i].copy()

            // Don't move static elements
            if (!l.isStatic) {
                l = compactItem(Layout(compareWith), l, compactType, cols, sorted)

                // Add to comparison array. We only collide with items before this one.
                // Statics are already in this array.
                compareWith.add(l)
            }

            // Add to output array to make sure they still come out in the right order.
            out[items.indexOf(sorted[i])] = l

            // Clear moved flag, if it exists.
            if (l.moved) {
                out.replaceAll { if (it?.i == l.i) l.copy(moved = false) else it }
            }
        }

        return Layout(out.filterNotNull())
    }

    /**
     * Before moving item down, it will check if the movement will cause collisions and move those items down before.
     */
    private fun resolveCompactionCollision(
        item: LayoutItem,
        moveToCoord: Int,
        axis: Axis
    ): LayoutItem {
        var newItem = axis.incr(item)

        val sizeProp = heightWidth[axis]!!
        val itemIndex = items.indexOfFirst { layoutItem -> layoutItem.i == newItem.i }

        // Go through each item we collide with.
        for (i in itemIndex + 1 until items.size) {
            val otherItem = items[i]
            // Ignore static items
            if (otherItem.isStatic) continue

            // Optimization: we can break early if we know we're past this el
            // We can do this b/c it's a sorted layout
            if (otherItem.y > newItem.y + newItem.h) break

            if (collides(newItem, otherItem)) {
                newItem =
                    resolveCompactionCollision(
                        otherItem,
                        moveToCoord + sizeProp.invoke(newItem),
                        axis
                    )
            }
        }

        return axis.set(newItem, moveToCoord)
    }

    /**
     * Given a layout, make sure all elements fit within its bounds.
     *
     * Modifies layout items.
     *
     * @param  {Array} layout Layout array.
     * @param  {Number} cols Number of columns.
     */
    fun correctBounds(cols: Int): Layout {
        val collidesWith = getStatics().toMutableList()
        val newLayoutItems = ArrayList(items)
        newLayoutItems.replaceAll { l ->
            var newItem = l

            // Overflows right
            if (l.x + l.w > cols) newItem = newItem.copy(x = cols - l.w)
            // Overflows left
            if (l.x < 0) {
                newItem = newItem.copy(x = 0, w = cols)
            }
            if (!l.isStatic) collidesWith.add(l) else {
                // If this is static and collides with other statics, we must move it down.
                // We have to do something nicer than just letting them overlap.
                while (Layout(collidesWith).getFirstCollision(l) != null) {
                    newItem = newItem.copy(y = newItem.y + 1)
                }
            }
            newItem
        }
        return Layout(newLayoutItems)
    }

    /**
     * Get a layout item by ID. Used so we can override later on if necessary.
     *
     * @param  {Array}  layout Layout array.
     * @param  {String} id     ID
     * @return {LayoutItem}    Item at ID.
     */
    fun find(id: String): LayoutItem? =
        items.firstOrNull { it.i == id }

    /**
     * Returns the first item this layout collides with.
     * It doesn't appear to matter which order we approach this from, although
     * perhaps that is the wrong thing to do.
     *
     * @param  {Object} layoutItem Layout item.
     * @return {Object|undefined}  A colliding layout item, or undefined.
     */
    private fun getFirstCollision(layoutItem: LayoutItem): LayoutItem? =
        items.firstOrNull { collides(it, layoutItem) }

    fun getAllCollisions(layoutItem: LayoutItem): List<LayoutItem> =
        items.filter { l -> collides(l, layoutItem) }

    /**
     * Get all static elements.
     * @param  {Array} layout Array of layout objects.
     * @return {Array}        Array of static layout items..
     */
    private fun getStatics(): List<LayoutItem> =
        items.filter { l -> l.isStatic }

    /**
     * Move an element. Responsible for doing cascading movements of other elements.
     *
     * Modifies layout items.
     *
     * @param  {Array}      layout            Full layout to modify.
     * @param  {LayoutItem} l                 element to move.
     * @param  {Number}     [x]               X position in grid units.
     * @param  {Number}     [y]               Y position in grid units.
     */
    fun moveElement(
        l: LayoutItem,
        x: Int?,
        y: Int?,
        isUserAction: Boolean,
        preventCollision: Boolean,
        compactType: CompactType,
        cols: Int,
        allowOverlap: Boolean = false
    ): Layout {
        // If this is static and not explicitly enabled as draggable,
        // no move is possible, so we can short-circuit this immediately.
        if (l.isStatic && !l.isDraggable) return this

        // Short-circuit if nothing to do.
        if (l.y == y && l.x == x) return this

        logger.debug {
            "Moving element ${l.i} to [$x,$y] from [${l.x},${l.y}]"
        }
        val oldX = l.x
        val oldY = l.y

        val newItem = l.copy(
            x = x ?: l.x,
            y = y ?: l.y,
            moved = true
        )
        var updatedLayout = updateLayout(newItem)

        // If this collides with anything, move it.
        // When doing this comparison, we have to sort the items we compare with
        // to ensure, in the case of multiple collisions, that we're getting the
        // nearest collision.
        var sorted = updatedLayout.sortLayoutItems(compactType)
        val movingUp =
            if (compactType == CompactType.vertical && y != null)
                oldY >= y
            else if (compactType == CompactType.horizontal && x != null)
                oldX >= x
            else false
        // $FlowIgnore acceptable modification of read-only array as it was recently cloned
        if (movingUp) sorted = Layout(sorted.items.reversed())
        val collisions = sorted.getAllCollisions(newItem)
        val hasCollisions = collisions.isNotEmpty()

        // We may have collisions. We can short-circuit if we've turned off collisions or
        // allowed overlap.
        if (hasCollisions && allowOverlap) {
            // Easy, we don't need to resolve collisions. But we *did* change the layout,
            // so clone it on the way out.
            return updatedLayout
        } else if (hasCollisions && preventCollision) {
            // If we are preventing collision but not allowing overlap, we need to
            // revert the position of this element so it goes to where it came from, rather
            // than the user's desired location.
            logger.info { "Collision prevented on ${l.i}, reverting." }
            return this // did not change so don't clone
        }

        // Move each item that collides away from this element.
        for (collision in collisions) {
            logger.info {
                "Resolving collision between ${newItem.i} at [${newItem.x},${newItem.y}] and ${collision.i} at [${collision.x},${collision.y}]"
            }

            // Short circuit so we can't infinitely loop
            if (collision.moved) continue

            val movingAxis = if (newItem.x != x) Axis.x else Axis.y
            // Don't move static items - we have to move *this* element away
            updatedLayout =
                if (collision.isStatic) {
                    updatedLayout.moveElementAwayFromCollision(collision, newItem, isUserAction, compactType, cols, movingAxis)
                } else {
                    updatedLayout.moveElementAwayFromCollision(newItem, collision, isUserAction, compactType, cols, movingAxis)
                }
        }

        return updatedLayout
    }

    fun removeItem(id: String): Layout =
        Layout(items.filter { it.i != id })

    /**
     * This is where the magic needs to happen - given a collision, move an element away from the collision.
     * We attempt to move it up if there's room, otherwise it goes below.
     *
     * @param  {Array} layout            Full layout to modify.
     * @param  {LayoutItem} collidesWith Layout item we're colliding with.
     * @param  {LayoutItem} itemToMove   Layout item we're moving.
     */
    private fun moveElementAwayFromCollision(
        collidesWith: LayoutItem,
        itemToMove: LayoutItem,
        isUserAction: Boolean,
        compactType: CompactType,
        cols: Int,
        movingAxis: Axis
    ): Layout {
        val compactH = compactType == CompactType.horizontal
        val compactV = compactType == CompactType.vertical

        // If there is enough space above the collision to put this element, move it there.
        // We only do this on the main collision as this can get funky in cascades and cause
        // unwanted swapping behavior.
        if (isUserAction) {
            // Make a mock item so we don't modify the item here, only modify in moveElement.
            val fakeItem = LayoutItem(
                if (movingAxis == Axis.x) collidesWith.x + collidesWith.w else itemToMove.x,
                if (movingAxis == Axis.y) collidesWith.y + collidesWith.y else itemToMove.y,
                itemToMove.w,
                itemToMove.h,
                i = "-1"
            )

            // No collision? If so, we can go up there; otherwise, we'll end up moving down as normal
            if (getFirstCollision(fakeItem) != null) {
                logger.debug {
                    "Doing reverse collision on ${itemToMove.i} up to [${fakeItem.x},${fakeItem.y}]."
                }
                return moveElement(
                    itemToMove,
                    if (compactH) fakeItem.x else null,
                    if (compactV) fakeItem.y else null,
                    false, // Reset isUserAction flag because we're not in the main collision anymore.
                    collidesWith.isStatic, // we're already colliding (not for static items)
                    compactType,
                    cols
                )
            }
        }

        return moveElement(
            itemToMove,
            if (compactH || movingAxis == Axis.x) itemToMove.x + 1 else null,
            if (compactV || movingAxis == Axis.y) itemToMove.y + 1 else null,
            isUserAction,
            collidesWith.isStatic, // we're already colliding (not for static items)
            compactType,
            cols
        )
    }

    /**
     * Compact an item in the layout.
     *
     * Returns modified item.
     *
     */
    private fun compactItem(
        compareWith: Layout,
        l: LayoutItem,
        compactType: CompactType,
        cols: Int,
        fullLayout: Layout
    ): LayoutItem {
        val compactV = compactType == CompactType.vertical
        val compactH = compactType == CompactType.horizontal
        var newItem = l
        if (compactV) {
            // Bottom 'y' possible is the bottom of the layout.
            // This allows you to do nice stuff like specify {y: Infinity}
            // This is here because the layout must be sorted in order to get the correct bottom `y`.
            newItem = newItem.copy(y = min(compareWith.bottom(), newItem.y))
            // Move the element up as far as it can go without colliding.
            while (newItem.y > 0 && compareWith.getFirstCollision(newItem) == null) {
                newItem = newItem.copy(y = newItem.y - 1)
            }
        } else if (compactH) {
            // Move the element left as far as it can go without colliding.
            while (newItem.x > 0 && compareWith.getFirstCollision(newItem) == null) {
                newItem = newItem.copy(x = newItem.x - 1)
            }
        }

        // Move it down, and keep moving it down if it's colliding.
        var collides: LayoutItem? = compareWith.getFirstCollision(newItem)
        while (collides != null) {
            newItem = if (compactH) {
                fullLayout.resolveCompactionCollision(newItem, collides.x + collides.w, Axis.x)
            } else {
                fullLayout.resolveCompactionCollision(newItem, collides.y + collides.h, Axis.y)
            }
            // Since we can't grow without bounds horizontally, if we've overflown, var's move it down and try again.
            if (compactH && newItem.x + newItem.w > cols) {
                newItem = newItem.copy(
                    x = cols - newItem.w,
                    y = newItem.y + 1
                )
            }

            collides = compareWith.getFirstCollision(newItem)
        }

        // Ensure that there are no negative positions
        return newItem.copy(
            x = max(newItem.x, 0),
            y = max(newItem.y, 0)
        )
    }

    /**
     * Get layout items sorted from top left to right and down.
     *
     * @return {Array} Array of layout objects.
     * @return {Array}        Layout, sorted static items first.
     */
    private fun sortLayoutItems(compactType: CompactType): Layout =
        when (compactType) {
            CompactType.horizontal -> sortLayoutItemsByColRow()
            CompactType.vertical -> sortLayoutItemsByRowCol()
            else -> this
        }

    /**
     * Sort layout items by row ascending and column ascending.
     *
     * Does not modify Layout.
     */
    private fun sortLayoutItemsByRowCol(): Layout =
        Layout(items.sortedWith { a, b ->
            if (a.y > b.y || (a.y == b.y && a.x > b.x)) {
                1
            } else if (a.y == b.y && a.x == b.x) {
                // Without this, we can get different sort results in IE vs. Chrome/FF
                0
            } else -1
        })

    /**
     * Sort layout items by column ascending then row ascending.
     *
     * Does not modify Layout.
     */
    private fun sortLayoutItemsByColRow(): Layout =
        Layout(items.sortedWith { a, b ->
            if (a.x > b.x || (a.x == b.x && a.y > b.y)) 1 else -1
        })

    fun canonicalize(): Layout =
        Layout(items.sortedWith { a, b ->
            if (a.y > b.y ||
                (a.y == b.y && a.x > b.x) ||
                (a.y == b.y && a.x == b.x && a.i > b.i)
            ) 1 else -1
        })

    /**
     * Given two layoutitems, check if they collide.
     */
    private fun collides(l1: LayoutItem, l2: LayoutItem): Boolean {
        if (l1.i === l2.i) return false // same element
        if (l1.x + l1.w <= l2.x) return false // l1 is left of l2
        if (l1.x >= l2.x + l2.w) return false // l1 is right of l2
        if (l1.y + l1.h <= l2.y) return false // l1 is above l2
        if (l1.y >= l2.y + l2.h) return false // l1 is below l2
        return true // boxes overlap
    }

    companion object {
        private val logger = Logger<Layout>()

        private val heightWidth = mapOf(
            Axis.x to { layoutItem: LayoutItem -> layoutItem.w },
            Axis.y to { layoutItem: LayoutItem -> layoutItem.h }
        )
    }
}