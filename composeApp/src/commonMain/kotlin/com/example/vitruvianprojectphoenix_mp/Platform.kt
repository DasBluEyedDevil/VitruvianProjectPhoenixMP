package com.example.vitruvianprojectphoenix_mp

interface Platform {
    val name: String
}

expect fun getPlatform(): Platform