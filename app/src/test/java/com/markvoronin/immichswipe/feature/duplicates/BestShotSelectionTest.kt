package com.markvoronin.immichswipe.feature.duplicates

import android.content.ContextWrapper
import com.markvoronin.immichswipe.feature.duplicates.BestShotAnalyzer.AssetScore
import com.markvoronin.immichswipe.feature.duplicates.BestShotAnalyzer.BestShotMode
import org.junit.Assert.assertEquals
import org.junit.Test

class BestShotSelectionTest {
    // Selection is pure ranking logic; the context is only used by the image analysis.
    private val analyzer = BestShotAnalyzer(ContextWrapper(null))

    private fun score(id: String, total: Float) = AssetScore(id, total, false, 0f, null, null)

    private fun select(mode: BestShotMode, vararg scores: AssetScore) =
        analyzer.selectKeptAssetIds(scores.toList(), mode)

    @Test
    fun `nothing is kept when there are no scores`() {
        BestShotMode.entries.forEach { assertEquals(emptySet<String>(), select(it)) }
    }

    @Test
    fun `single best keeps the highest score whatever the input order`() {
        assertEquals(setOf("b"), select(BestShotMode.SINGLE_BEST, score("a", 0.2f), score("b", 0.9f), score("c", 0.5f)))
    }

    @Test
    fun `top 2 and top 3 keep that many of the best`() {
        val scores = arrayOf(score("a", 0.2f), score("b", 0.9f), score("c", 0.5f), score("d", 0.7f))
        assertEquals(setOf("b", "d"), select(BestShotMode.TOP_2, *scores))
        assertEquals(setOf("b", "d", "c"), select(BestShotMode.TOP_3, *scores))
    }

    @Test
    fun `top n always leaves one shot of the cluster to delete`() {
        assertEquals(setOf("b"), select(BestShotMode.TOP_2, score("a", 0.2f), score("b", 0.9f)))
        assertEquals(setOf("b"), select(BestShotMode.TOP_3, score("a", 0.2f), score("b", 0.9f)))
        assertEquals(
            setOf("b", "c"),
            select(BestShotMode.TOP_3, score("a", 0.2f), score("b", 0.9f), score("c", 0.5f))
        )
    }

    @Test
    fun `auto keeps only the shots close to the best one`() {
        val kept = select(
            BestShotMode.AUTO,
            score("a", 0.90f), score("b", 0.88f), score("c", 0.50f), score("d", 0.40f)
        )
        assertEquals(setOf("a", "b"), kept)
    }

    @Test
    fun `auto never keeps the whole cluster of three or more`() {
        val kept = select(BestShotMode.AUTO, score("a", 0.90f), score("b", 0.90f), score("c", 0.90f))
        assertEquals(2, kept.size)
    }

    @Test
    fun `auto keeps only one shot of a tied pair`() {
        assertEquals(1, select(BestShotMode.AUTO, score("a", 0.9f), score("b", 0.9f)).size)
    }

    @Test
    fun `ties are broken by sharpness`() {
        val kept = select(
            BestShotMode.SINGLE_BEST,
            AssetScore("soft", 0.9f, false, 0.4f, null, null), AssetScore("sharp", 0.9f, false, 0.8f, null, null)
        )
        assertEquals(setOf("sharp"), kept)
    }

    @Test
    fun `assets that could not be analyzed are always kept`() {
        val failed = AssetScore("failed", 0f, false, 0f, null, null, analyzed = false)
        assertEquals(
            setOf("a", "failed"),
            select(BestShotMode.SINGLE_BEST, score("a", 0.9f), score("b", 0.2f), failed)
        )
        assertEquals(setOf("failed"), select(BestShotMode.AUTO, failed))
    }

    @Test
    fun `auto keeps a lone asset`() {
        assertEquals(setOf("a"), select(BestShotMode.AUTO, score("a", 0f)))
    }
}
