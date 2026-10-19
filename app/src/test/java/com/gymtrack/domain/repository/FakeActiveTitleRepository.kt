package com.gymtrack.domain.repository

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

class FakeActiveTitleRepository(
    initial: String? = null,
) : ActiveTitleRepository {

    private val preferred = MutableStateFlow(initial)
    val setCalls = mutableListOf<String?>()

    override val activeTitleId: Flow<String?> = preferred.asStateFlow()

    override suspend fun setActiveTitleId(titleId: String?) {
        setCalls.add(titleId)
        preferred.value = titleId
    }

    fun peek(): String? = preferred.value
}
