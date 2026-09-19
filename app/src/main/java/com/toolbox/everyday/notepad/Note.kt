package com.toolbox.everyday.notepad

import kotlinx.serialization.Serializable

@Serializable
data class Note(
    val id: String,
    val body: String,
    val updatedAt: Long,
) {
    /** First non-blank line, used as the list title. */
    val title: String
        get() = body.lineSequence().firstOrNull { it.isNotBlank() }?.trim().orEmpty()
}
