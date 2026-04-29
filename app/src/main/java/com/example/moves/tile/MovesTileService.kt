package com.example.moves.tile

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import androidx.wear.protolayout.ActionBuilders
import androidx.wear.protolayout.ColorBuilders
import androidx.wear.protolayout.DimensionBuilders
import androidx.wear.protolayout.LayoutElementBuilders
import androidx.wear.protolayout.ModifiersBuilders
import androidx.wear.protolayout.ResourceBuilders
import androidx.wear.protolayout.TimelineBuilders
import androidx.wear.protolayout.material.Colors
import androidx.wear.protolayout.material.Text
import androidx.wear.protolayout.material.Typography
import androidx.wear.tiles.RequestBuilders
import androidx.wear.tiles.TileBuilders
import androidx.wear.tiles.TileService
import com.example.moves.data.SettingsRepository
import com.example.moves.presentation.MainActivity
import androidx.concurrent.futures.ResolvableFuture
import com.google.common.util.concurrent.ListenableFuture
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

private const val RESOURCES_VERSION = "1"
private const val FRESHNESS_MS = 60_000L

class MovesTileService : TileService() {

    override fun onTileRequest(
        requestParams: RequestBuilders.TileRequest,
    ): ListenableFuture<TileBuilders.Tile> {
        val future = ResolvableFuture.create<TileBuilders.Tile>()
        val ctx = applicationContext
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val repo = SettingsRepository(ctx)
                val settings = repo.currentSettings()
                val state = repo.currentScheduleState()
                val now = System.currentTimeMillis()
                val remainingMs = (state.nextScheduledAtEpochMillis - now).coerceAtLeast(0L)
                val nextType = com.example.moves.domain.ReminderType
                    .fromString(state.lastFiredType).next().name

                val (line1, line2) = if (!settings.enabled) {
                    "Paused" to ""
                } else {
                    formatRemaining(remainingMs) to nextType
                }

                val tile = TileBuilders.Tile.Builder()
                    .setResourcesVersion(RESOURCES_VERSION)
                    .setFreshnessIntervalMillis(FRESHNESS_MS)
                    .setTileTimeline(
                        TimelineBuilders.Timeline.Builder()
                            .addTimelineEntry(
                                TimelineBuilders.TimelineEntry.Builder()
                                    .setLayout(
                                        LayoutElementBuilders.Layout.Builder()
                                            .setRoot(rootLayout(ctx, line1, line2))
                                            .build(),
                                    )
                                    .build(),
                            )
                            .build(),
                    )
                    .build()
                future.set(tile)
            } catch (t: Throwable) {
                future.setException(t)
            }
        }
        return future
    }

    override fun onTileResourcesRequest(
        requestParams: RequestBuilders.ResourcesRequest,
    ): ListenableFuture<ResourceBuilders.Resources> {
        val resources = ResourceBuilders.Resources.Builder()
            .setVersion(RESOURCES_VERSION)
            .build()
        return ResolvableFuture.create<ResourceBuilders.Resources>().also { it.set(resources) }
    }

    private fun rootLayout(
        ctx: Context,
        line1: String,
        line2: String,
    ): LayoutElementBuilders.LayoutElement {
        val openApp = ActionBuilders.LaunchAction.Builder()
            .setAndroidActivity(
                ActionBuilders.AndroidActivity.Builder()
                    .setPackageName(ctx.packageName)
                    .setClassName(MainActivity::class.java.name)
                    .build(),
            )
            .build()

        val column = LayoutElementBuilders.Column.Builder()
            .setHorizontalAlignment(LayoutElementBuilders.HORIZONTAL_ALIGN_CENTER)
            .setWidth(DimensionBuilders.expand())
            .setHeight(DimensionBuilders.expand())
            .addContent(
                Text.Builder(ctx, "Moves")
                    .setTypography(Typography.TYPOGRAPHY_CAPTION1)
                    .setColor(ColorBuilders.argb(Colors.DEFAULT.primary))
                    .build(),
            )
            .addContent(spacer(8))
            .addContent(
                Text.Builder(ctx, line1)
                    .setTypography(Typography.TYPOGRAPHY_DISPLAY3)
                    .setColor(ColorBuilders.argb(Colors.DEFAULT.onSurface))
                    .build(),
            )
            .also { col ->
                if (line2.isNotEmpty()) {
                    col.addContent(spacer(4))
                    col.addContent(
                        Text.Builder(ctx, line2)
                            .setTypography(Typography.TYPOGRAPHY_CAPTION2)
                            .setColor(ColorBuilders.argb(Colors.DEFAULT.onSurface))
                            .build(),
                    )
                }
            }
            .build()

        return LayoutElementBuilders.Box.Builder()
            .setWidth(DimensionBuilders.expand())
            .setHeight(DimensionBuilders.expand())
            .setModifiers(
                ModifiersBuilders.Modifiers.Builder()
                    .setClickable(
                        ModifiersBuilders.Clickable.Builder()
                            .setId("open_app")
                            .setOnClick(openApp)
                            .build(),
                    )
                    .build(),
            )
            .addContent(column)
            .build()
    }

    private fun spacer(dp: Int): LayoutElementBuilders.Spacer =
        LayoutElementBuilders.Spacer.Builder()
            .setHeight(DimensionBuilders.dp(dp.toFloat()))
            .build()

    private fun formatRemaining(ms: Long): String {
        if (ms <= 0L) return "now"
        val totalSec = ms / 1000
        val m = totalSec / 60
        val s = totalSec % 60
        return if (m == 0L) "${s}s" else "${m}m"
    }

    companion object {
        fun requestRefresh(context: Context) {
            try {
                getUpdater(context).requestUpdate(MovesTileService::class.java)
            } catch (_: Throwable) {
                // Tile not added yet — refresh is a no-op in that case.
            }
        }
    }
}
