package com.anush.vibora.Utils

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class FireBaseUtils {

    private val auth: FirebaseAuth = FirebaseAuth.getInstance()
    private val db: FirebaseFirestore = FirebaseFirestore.getInstance()
    private val TAG: String = "FirebaseUtils"

    fun getCurrentUserId(): String? {
        return if (auth.currentUser != null) auth.currentUser!!.uid else null
    }

    fun getChatId(user1: String, user2: String): String {
        return if (user1 < user2) user1 + "_" + user2 else user2 + "_" + user1
    }

}