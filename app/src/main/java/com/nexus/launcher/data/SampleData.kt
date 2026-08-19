package com.nexus.launcher.data

import com.nexus.launcher.domain.BudgetCategory
import com.nexus.launcher.domain.BudgetState
import com.nexus.launcher.domain.ClaudeStatus
import com.nexus.launcher.domain.ClaudeStep
import com.nexus.launcher.domain.ClaudeTask
import com.nexus.launcher.domain.Transaction

/**
 * Seed content for the hubs that depend on an account the launcher does not have
 * yet (Plex, Twitch, the Claude relay) plus a starter budget. Everything here is
 * replaced by live data as soon as the matching integration is configured in
 * Nexus Settings — see [com.nexus.launcher.integration] for the wiring.
 */
object SampleData {

    fun budget(): BudgetState {
        val now = System.currentTimeMillis()
        val hour = 3_600_000L
        return BudgetState(
            totalLimitCents = 250_000,
            periodEndEpochDay = 11,
            categories = listOf(
                BudgetCategory("Food", 42_000, 60_000),
                BudgetCategory("Transport", 18_500, 25_000),
                BudgetCategory("Fun", 31_000, 28_000),
            ),
            transactions = listOf(
                Transaction("t1", "Grocery run", "Food", -4_215, now - 2 * hour),
                Transaction("t2", "Metro pass", "Transport", -3_000, now - 26 * hour),
                Transaction("t3", "Salary", "Income", 180_000, now - 52 * hour),
                Transaction("t4", "Cinema", "Fun", -1_800, now - 74 * hour),
                Transaction("t5", "Coffee", "Food", -450, now - 96 * hour),
                Transaction("t6", "Streaming", "Fun", -1_299, now - 120 * hour),
            ),
        )
    }

    fun claudeTasks(): List<ClaudeTask> = listOf(
        ClaudeTask(
            id = "c1",
            title = "Refactoring auth flow",
            status = ClaudeStatus.Running,
            detail = "nexus-api · feature/auth-v2",
            progress = 0.64f,
            device = "Mac Studio",
            steps = listOf(
                ClaudeStep("Read session handling", true),
                ClaudeStep("Extract token store", true),
                ClaudeStep("Migrate call sites", false),
                ClaudeStep("Run test suite", false),
            ),
        ),
        ClaudeTask(
            id = "c2",
            title = "Migrate billing tests",
            status = ClaudeStatus.NeedsYou,
            detail = "billing-svc · main",
            device = "Mac Studio",
            question = "Should the legacy proration test keep its old fixtures, or regenerate them?",
        ),
        ClaudeTask(
            id = "c3",
            title = "Bump dependencies",
            status = ClaudeStatus.Done,
            detail = "12 packages · all green",
            progress = 1f,
            device = "MacBook Pro",
        ),
    )
}
