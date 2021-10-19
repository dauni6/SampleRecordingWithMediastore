package com.example.samplerecordingwithmediastore

import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.provider.MediaStore
import android.util.Log
import androidx.annotation.RequiresApi
import com.example.samplerecordingwithmediastore.databinding.ActivityListBinding

class ListActivity : AppCompatActivity() {

    private val binding by lazy {
        ActivityListBinding.inflate(layoutInflater)
    }

    private val adapter by lazy {
        RecordListAdapter()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(binding.root)

        initRecyclerView()

    }

    private fun initRecyclerView() = with(binding) {
        listRecyclerView.adapter = adapter
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            adapter.setNewRecordList(getAllList())
        }
    }

    @RequiresApi(Build.VERSION_CODES.Q)
    private fun getAllList(): List<Record> {
        val listUrl = MediaStore.Audio.Media.EXTERNAL_CONTENT_URI // 녹음파일 주소
        val proj = arrayOf(
            MediaStore.Audio.Media._ID,
            MediaStore.Audio.Media.DISPLAY_NAME,
            MediaStore.Audio.Media.DURATION,
            MediaStore.Audio.Media.DATE_ADDED
        )
        val selection = "${MediaStore.Audio.Media.DISPLAY_NAME} >= ?" // SQL where 절
        val selectionArgs = arrayOf(
            "monkey_" // where의 조건칼럼
        )

        val cursor = contentResolver.query(listUrl, proj, selection, selectionArgs, null) // 뒤에 3개의 인자에 null을하면 모두 가져옴
        val recordList = mutableListOf<Record>()

        while (cursor?.moveToNext() == true) {
            val id = cursor.getString(0)
            val displayName = cursor.getString(1)
            val duration = cursor.getLong(2)
            val createAt = cursor.getLong(3)


            val record = Record(id, displayName, duration, createAt)
            Log.d("TEST", "Record = $record")
            recordList.add(record)
        }

        cursor?.close()

        return recordList
    }

    companion object {

        fun newIntent(context: Context) = Intent(context, ListActivity::class.java)

    }

}
