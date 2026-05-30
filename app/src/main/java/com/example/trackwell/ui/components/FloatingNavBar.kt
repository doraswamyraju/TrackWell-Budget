package com.example.trackwell.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Alarm
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.PhotoCamera
import androidx.compose.material.icons.filled.PieChart
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation3.runtime.NavKey
import com.example.trackwell.*
import com.example.trackwell.theme.*

sealed class NavItem(val key: NavKey, val title: String, val icon: ImageVector) {
    object Dashboard : NavItem(Main, "Home", Icons.Default.Home)
    object Transactions : NavItem(TransactionHistory, "Activity", Icons.Default.History)
    object Budgets : NavItem(BudgetPlanner, "Budgets", Icons.Default.PieChart)
    object Reminders : NavItem(ReminderList, "Reminders", Icons.Default.Alarm)
}

@Composable
fun FloatingNavBar(
    currentDestination: NavKey,
    onTabSelected: (NavKey) -> Unit,
    modifier: Modifier = Modifier
) {
    val leftItems = listOf(NavItem.Dashboard, NavItem.Transactions)
    val rightItems = listOf(NavItem.Budgets, NavItem.Reminders)

    Box(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp),
        contentAlignment = Alignment.BottomCenter
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(72.dp)
                .clip(RoundedCornerShape(28.dp))
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            CardDark.copy(alpha = 0.92f),
                            CardDarkSecondary.copy(alpha = 0.96f)
                        )
                    )
                )
                .padding(horizontal = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Left tabs
            Row(
                modifier = Modifier.weight(2f),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                leftItems.forEach { item ->
                    NavBarTabItem(
                        item = item,
                        isSelected = currentDestination == item.key,
                        onClick = { onTabSelected(item.key) }
                    )
                }
            }

            // Center FAB spacer
            Box(
                modifier = Modifier
                    .weight(1.2f)
                    .fillMaxHeight(),
                contentAlignment = Alignment.Center
            ) {
                Box(
                    modifier = Modifier
                        .offset(y = (-16).dp)
                        .size(58.dp)
                        .background(
                            Brush.linearGradient(
                                colors = listOf(NeonCyan, ElectricViolet)
                            ),
                            CircleShape
                        )
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null
                        ) {
                            onTabSelected(OcrScanner)
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.PhotoCamera,
                        contentDescription = "Scan Receipt",
                        tint = DeepObsidian,
                        modifier = Modifier.size(28.dp)
                    )
                }
            }

            // Right tabs
            Row(
                modifier = Modifier.weight(2f),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                rightItems.forEach { item ->
                    NavBarTabItem(
                        item = item,
                        isSelected = currentDestination == item.key,
                        onClick = { onTabSelected(item.key) }
                    )
                }
            }
        }
    }
}

@Composable
fun RowScope.NavBarTabItem(
    item: NavItem,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    val tintColor by animateColorAsState(
        targetValue = if (isSelected) NeonCyan else TextSecondaryDark,
        animationSpec = spring(stiffness = Spring.StiffnessLow),
        label = "tintColor"
    )

    val scaleValue by animateFloatAsState(
        targetValue = if (isSelected) 1.15f else 1.0f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessLow),
        label = "scale"
    )

    Column(
        modifier = Modifier
            .weight(1f)
            .fillMaxHeight()
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null
            ) {
                onClick()
            },
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = item.icon,
            contentDescription = item.title,
            tint = tintColor,
            modifier = Modifier
                .size(24.dp)
                .scale(scaleValue)
        )
        Spacer(modifier = Modifier.height(2.dp))
        Text(
            text = item.title,
            fontSize = 9.sp,
            color = tintColor,
            maxLines = 1
        )
        Spacer(modifier = Modifier.height(2.dp))
        // Active dot
        Box(
            modifier = Modifier
                .height(3.dp)
                .width(if (isSelected) 12.dp else 0.dp)
                .background(color = NeonCyan, shape = CircleShape)
        )
    }
}
