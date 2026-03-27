package com.sr.fixit106.data.posts

object PostStatus {
    const val NEW = "NEW"
    const val IN_PROGRESS = "IN_PROGRESS"
    const val RESOLVED = "RESOLVED"

    val all: List<String> = listOf(NEW, IN_PROGRESS, RESOLVED)
}