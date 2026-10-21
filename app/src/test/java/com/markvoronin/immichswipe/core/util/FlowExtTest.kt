package com.markvoronin.immichswipe.core.util

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Test

class FlowExtTest {

    @Test
    fun `combine6 passes each flow's value in the right position and keeps the types`() = runBlocking {
        val result = combine6(
            flowOf(1), flowOf("two"), flowOf(3.0), flowOf('d'), flowOf(true), flowOf(6L)
        ) { a, b, c, d, e, f -> listOf(a, b, c, d, e, f) }.first()
        assertEquals(listOf<Any>(1, "two", 3.0, 'd', true, 6L), result)
    }

    @Test
    fun `combine6 re-emits when any input changes`() = runBlocking {
        val changing = MutableStateFlow("a")
        val flow = combine6(flowOf(1), changing, flowOf(3), flowOf(4), flowOf(5), flowOf(6)) { _, b, _, _, _, _ -> b }
        assertEquals("a", flow.first())
        changing.value = "b"
        assertEquals("b", flow.first())
    }

    @Test
    fun `combine6 completes after all inputs complete`() = runBlocking {
        val out = combine6(flowOf(1, 2), flowOf(10), flowOf(0), flowOf(0), flowOf(0), flowOf(0)) { a, b, _, _, _, _ -> a + b }
            .toList()
        assertEquals(listOf(12), out.takeLast(1))
    }
}
