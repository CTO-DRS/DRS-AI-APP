package com.drs.ai.features.onboarding

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.Memory
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.drs.ai.R
import com.drs.ai.ui.components.PrimaryAction
import com.drs.ai.ui.components.SoftAction
import kotlinx.coroutines.launch

/**
 * v1.4 — first-run welcome wizard: three honest, animated pages.
 * Shown exactly once (persisted flag), skippable, zero data collection.
 */
@Composable
fun OnboardingScreen(onFinish: () -> Unit) {
    val scope = rememberCoroutineScope()
    val pager = rememberPagerState(initialPage = 0, pageCount = { 3 })

    val pages = listOf(
        OnboardingPage(
            icon = Icons.Filled.Memory,
            titleRes = R.string.onboarding_1_title,
            bodyRes = R.string.onboarding_1_body
        ),
        OnboardingPage(
            icon = Icons.Filled.Shield,
            titleRes = R.string.onboarding_2_title,
            bodyRes = R.string.onboarding_2_body
        ),
        OnboardingPage(
            icon = Icons.Filled.Bolt,
            titleRes = R.string.onboarding_3_title,
            bodyRes = R.string.onboarding_3_body
        )
    )

    Column(
        Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(24.dp)
    ) {
        // Skip — always available, top trailing edge
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
            TextButton(onClick = onFinish) { Text(stringResource(R.string.onboarding_skip)) }
        }

        HorizontalPager(
            state = pager,
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
        ) { page ->
            val p = pages[page]
            Column(
                Modifier.fillMaxSize(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                // Glowing gradient orb with the page icon — spring entrance per page
                var pulse by remember(page) { mutableStateOf(false) }
                LaunchedEffect(page) { pulse = true }
                val orbScale by animateFloatAsState(
                    targetValue = if (pulse) 1f else 0.7f,
                    animationSpec = spring(dampingRatio = 0.5f, stiffness = 120f),
                    label = "orb"
                )
                Box(
                    Modifier
                        .size(150.dp)
                        .scale(orbScale)
                        .background(
                            Brush.radialGradient(
                                colors = listOf(
                                    Color(0xFF7C4DFF).copy(alpha = 0.5f),
                                    Color(0xFF3A45A8).copy(alpha = 0.28f),
                                    Color.Transparent
                                ),
                                radius = 320f
                            ),
                            CircleShape
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Box(
                        Modifier
                            .size(96.dp)
                            .background(MaterialTheme.colorScheme.primaryContainer, CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            p.icon, contentDescription = null,
                            tint = MaterialTheme.colorScheme.onPrimaryContainer,
                            modifier = Modifier.size(46.dp)
                        )
                    }
                }

                Spacer(Modifier.height(34.dp))
                Text(
                    stringResource(p.titleRes),
                    style = MaterialTheme.typography.headlineMedium,
                    textAlign = TextAlign.Center,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.alpha(if (pager.currentPage == page) 1f else 0.6f)
                )
                Spacer(Modifier.height(12.dp))
                Text(
                    stringResource(p.bodyRes),
                    style = MaterialTheme.typography.bodyLarge,
                    textAlign = TextAlign.Center,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(horizontal = 8.dp)
                )
            }
        }

        // Animated page dots
        Row(
            Modifier
                .fillMaxWidth()
                .padding(vertical = 18.dp),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            repeat(3) { i ->
                val active = pager.currentPage == i
                val width by animateFloatAsState(
                    targetValue = if (active) 26f else 8f,
                    animationSpec = spring(dampingRatio = 0.7f, stiffness = 300f),
                    label = "dot$i"
                )
                Box(
                    Modifier
                        .padding(horizontal = 4.dp)
                        .size(width = width.dp, height = 8.dp)
                        .background(
                            if (active) MaterialTheme.colorScheme.primary
                            else MaterialTheme.colorScheme.outlineVariant,
                            RoundedCornerShape(50)
                        )
                )
            }
        }

        // Actions
        Row(
            Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            if (pager.currentPage == 2) {
                PrimaryAction(
                    label = stringResource(R.string.onboarding_start),
                    icon = Icons.AutoMirrored.Filled.ArrowForward,
                    modifier = Modifier.weight(1f),
                    onClick = onFinish
                )
            } else {
                SoftAction(
                    label = stringResource(R.string.onboarding_next),
                    icon = Icons.AutoMirrored.Filled.ArrowForward,
                    modifier = Modifier.weight(1f),
                    onClick = {
                        scope.launch {
                            pager.animateScrollToPage(pager.currentPage + 1)
                        }
                    }
                )
            }
        }
        Spacer(Modifier.height(14.dp))
    }
}

private data class OnboardingPage(
    val icon: ImageVector,
    val titleRes: Int,
    val bodyRes: Int
)
