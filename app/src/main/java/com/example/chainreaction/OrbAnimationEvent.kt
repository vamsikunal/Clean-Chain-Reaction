package com.example.chainreaction

data class OrbAnimationEvent(
    val fromRow: Int,
    val fromCol: Int,
    val toRow: Int,
    val toCol: Int,
    val playerOwner: Int
)
