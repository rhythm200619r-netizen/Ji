package app.ji.ui.theme

import androidx.compose.ui.graphics.Color
import app.ji.data.model.Reminder

/**
 * Category colors for Ji (SPEC.md):
 * - Professional = Blue
 * - Personal = Green
 * - Parcel = Amber
 * - Urgent = Red
 * Supports both light and dark mode.
 */
data class CategoryColor(
    val color: Color,
    val containerColor: Color,
    val onContainerColor: Color
)

object CategoryTheme {
    val ProfessionalLight = CategoryColor(
        color = Color(0xFF1976D2),
        containerColor = Color(0xFFE3F2FD),
        onContainerColor = Color(0xFF0D47A1)
    )
    val ProfessionalDark = CategoryColor(
        color = Color(0xFF64B5F6),
        containerColor = Color(0xFF0D47A1).copy(alpha = 0.4f),
        onContainerColor = Color(0xFFBBDEFB)
    )

    val PersonalLight = CategoryColor(
        color = Color(0xFF388E3C),
        containerColor = Color(0xFFE8F5E9),
        onContainerColor = Color(0xFF1B5E20)
    )
    val PersonalDark = CategoryColor(
        color = Color(0xFF81C784),
        containerColor = Color(0xFF1B5E20).copy(alpha = 0.4f),
        onContainerColor = Color(0xFFC8E6C9)
    )

    val ParcelLight = CategoryColor(
        color = Color(0xFFFFA000),
        containerColor = Color(0xFFFFF8E1),
        onContainerColor = Color(0xFFE65100)
    )
    val ParcelDark = CategoryColor(
        color = Color(0xFFFFD54F),
        containerColor = Color(0xFFE65100).copy(alpha = 0.35f),
        onContainerColor = Color(0xFFFFECB3)
    )

    val UrgentLight = CategoryColor(
        color = Color(0xFFD32F2F),
        containerColor = Color(0xFFFFEBEE),
        onContainerColor = Color(0xFFB71C1C)
    )
    val UrgentDark = CategoryColor(
        color = Color(0xFFE57373),
        containerColor = Color(0xFFB71C1C).copy(alpha = 0.4f),
        onContainerColor = Color(0xFFFFCDD2)
    )

    fun getColor(category: String, isDark: Boolean = false): CategoryColor {
        return when (category) {
            Reminder.CATEGORY_PROFESSIONAL -> if (isDark) ProfessionalDark else ProfessionalLight
            Reminder.CATEGORY_PARCEL -> if (isDark) ParcelDark else ParcelLight
            Reminder.CATEGORY_URGENT -> if (isDark) UrgentDark else UrgentLight
            else -> if (isDark) PersonalDark else PersonalLight
        }
    }
}

