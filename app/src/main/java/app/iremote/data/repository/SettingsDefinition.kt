package app.iremote.data.repository

import io.github.mlmgames.settings.core.annotations.CategoryDefinition
import io.github.mlmgames.settings.core.annotations.Persisted
import io.github.mlmgames.settings.core.annotations.Setting
import io.github.mlmgames.settings.core.types.Dropdown
import io.github.mlmgames.settings.core.types.Toggle

@CategoryDefinition(order = 0)
object General

@CategoryDefinition(order = 1)
object Appearance

@CategoryDefinition(order = 2)
object System

data class AppSettings(
    @Setting(
        title = "Default Sort",
        description = "Default sorting for lists",
        category = General::class,
        type = Dropdown::class,
        options = ["Name", "Recently Updated", "Recently Added"]
    )
    val defaultSort: Int = 1,

    @Setting(
        title = "Theme",
        category = Appearance::class,
        type = Dropdown::class,
        options = ["System", "Light", "Dark"]
    )
    val themeMode: Int = 2,

    @Persisted
    val useAuroraTheme: Boolean = true,

    @Setting(
        title = "Haptic feedback",
        description = "Vibrate lightly on key press",
        category = General::class,
        type = Toggle::class
    )
    val hapticFeedback: Boolean = true,

    @Setting(
        title = "Keep screen on",
        description = "Prevent the screen from sleeping while a remote is open",
        category = General::class,
        type = Toggle::class
    )
    val keepScreenOn: Boolean = true
)