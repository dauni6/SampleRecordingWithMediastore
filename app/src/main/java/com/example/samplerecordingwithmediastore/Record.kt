package com.example.samplerecordingwithmediastore

import android.net.Uri
import android.provider.MediaStore

data class Record(
    val id: String,
    val displayName: String?,
    val duration: Long?,
    val createAt: Long?
) {

    fun getRecordUri(): Uri = Uri.withAppendedPath(
        MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, id
    )

}
