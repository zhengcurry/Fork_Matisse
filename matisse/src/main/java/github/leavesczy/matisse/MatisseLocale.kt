package github.leavesczy.matisse

import java.util.Locale

/**
 * 全局 Locale 配置，宿主 App 在启动或切换语言时设置，
 * Matisse 内部 Activity 在 attachBaseContext 中读取并应用。
 *
 * 用法示例：
 * ```
 * // 宿主 App 启动时 / 切换语言时
 * MatisseLocale.locale = Locale.ENGLISH
 * ```
 *
 * 若未设置（为 null），则沿用系统默认行为（AppCompatDelegate / 系统 Locale）。
 */
object MatisseLocale {

    /**
     * 宿主 App 期望 Matisse 使用的 Locale。
     * 设为 null 表示不干预，使用系统 / AppCompatDelegate 默认行为。
     */
    @Volatile
    var locale: Locale? = null

}

