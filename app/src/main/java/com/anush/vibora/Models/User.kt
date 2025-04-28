package com.anush.vibora.Models

data class User(
    var userId: String,
    var name: String,
    var email: String,
    var password: String,
    var fcmToken: String,
    var number: String,
    var countryCode: String
)
