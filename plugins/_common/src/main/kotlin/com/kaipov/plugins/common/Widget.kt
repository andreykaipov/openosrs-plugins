package com.kaipov.plugins.common

import javax.inject.Inject
import net.runelite.api.Client
import net.runelite.api.widgets.Widget
import org.slf4j.LoggerFactory

// Additional widgets s

class WidgetUtility {
    private val log = LoggerFactory.getLogger(this::class.java)

    @Inject
    private lateinit var client: Client

    fun get(w: WidgetInfo): Widget? {
        log.info("hinooo in my wi ${System.identityHashCode(client)}")
        return client.getWidget(w.groupId, w.childId)
    }

    fun get(w: net.runelite.api.widgets.WidgetInfo): Widget? {
        log.info("nwow in rl wi ${System.identityHashCode(client)}")
        return client.getWidget(w.groupId, w.childId)
    }
}

// WidgetInfo mimics the WidgetInfo in the net.runelite.api.widgets package
enum class WidgetInfo(val groupId: Int, val childId: Int) {
    INTRO_HEAD_LEFT_BUTTON(269, 106);

    val id: Int get() = groupId shl 16 or childId
    val packedId: Int get() = groupId shl 16 or childId

    companion object {
        fun TO_GROUP(id: Int): Int {
            return id ushr 16
        }

        fun TO_CHILD(id: Int): Int {
            return id and '\uffff'.toInt()
        }

        fun PACK(groupId: Int, childId: Int): Int {
            return groupId shl 16 or childId
        }
    }
}