package com.getresponse.mobile_sdk

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import androidx.core.app.NotificationCompat
import io.jsonwebtoken.Jwts
import io.jsonwebtoken.SignatureAlgorithm
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import java.io.IOException
import java.net.URL
import java.util.Date

fun applyImageUrl(
    builder: NotificationCompat.Builder,
    imageUrl: String
) = runBlocking {
    val url = URL(imageUrl)
    withContext(Dispatchers.IO) {
        try {
            val input = url.openStream()
            BitmapFactory.decodeStream(input)
        } catch (e: IOException) {
            null
        }
    }?.let { bitmap ->
        builder.setLargeIcon(bitmap)
        val bitmapNull: Bitmap? = null
        builder.setStyle(NotificationCompat.BigPictureStyle().bigPicture(bitmap).bigLargeIcon(bitmapNull))
    }
}

fun createJWTToken(applicationId: String, secretKey: String, installationUUID: String) = Jwts.builder()
    .setHeaderParam("typ","JWT")
    .issuer(applicationId)
    .issuedAt(Date())
    .expiration(Date().apply { time += 70000 })
    .setAudience(installationUUID)
    .signWith(
        SignatureAlgorithm.HS256,
        secretKey.toByteArray()
    )
    .compact()