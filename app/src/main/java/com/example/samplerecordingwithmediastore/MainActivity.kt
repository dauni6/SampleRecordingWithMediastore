package com.example.samplerecordingwithmediastore

import android.Manifest
import android.content.ContentValues
import android.content.pm.PackageManager
import android.media.MediaPlayer
import android.media.MediaRecorder
import android.os.Build
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.os.Environment
import android.provider.MediaStore
import android.util.Log
import androidx.annotation.RequiresApi
import androidx.core.content.ContextCompat
import com.example.samplerecordingwithmediastore.databinding.ActivityMainBinding
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.*

class MainActivity : AppCompatActivity() {

    private val binding by lazy {
        ActivityMainBinding.inflate(layoutInflater)
    }

    /** state */
    private var state = RecordState.BEFORE_RECORDING
        set(value) {
            field = value
            binding.resetBtn.isEnabled = (value == RecordState.AFTER_RECORDING) || (value == RecordState.ON_PLAYING)
            binding.recordBtn.updateIconWithState(value)
        }

    /** recorder */
    private var recorder: MediaRecorder? = null
    private val recordingFilePath: String by lazy {
        "${externalCacheDir?.absolutePath}/recording.mp4" // 녹음한것을 저장할 path
    }

    /** player */
    private var player: MediaPlayer? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(binding.root)
        requestAudioPermission()
        initViews()
        bindViews()
        initVariables()
    }

    private fun requestAudioPermission() {
        requestPermissions(permissions, REQUEST_RECORD_AUDIO_PERMISSION)
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        val audioRecordPermissionGranted = requestCode == REQUEST_RECORD_AUDIO_PERMISSION &&
                grantResults.firstOrNull() == PackageManager.PERMISSION_GRANTED
        if (!audioRecordPermissionGranted) {
            finish()
        }

    }

    private fun initViews() {
        binding.recordBtn.updateIconWithState(state)
    }

    private fun bindViews() = with(binding) {
        recordBtn.setOnClickListener {
            when(state) {
                RecordState.BEFORE_RECORDING -> {
                    startRecording()
                }
                RecordState.ON_RECORDING -> {
                    stopRecording()
                }
                RecordState.AFTER_RECORDING -> {
                    startPlaying()
                }
                RecordState.ON_PLAYING -> {
                    stopPlaying()
                }
            }
        }

        resetBtn.setOnClickListener {
            stopPlaying() // 재생중일 때 리셋할 수도 있으므로
            binding.visualizerView.clearVisualization()
            binding.recordTimeTextView.clearCountTime()
            state = RecordState.BEFORE_RECORDING
        }

        visualizerView.onRequestCurrentAmplitude = {
            recorder?.maxAmplitude ?: 0
        }

        listBtn.setOnClickListener {
            // 목록 불러오기
            startActivity(ListActivity.newIntent(this@MainActivity))
        }

    }

    private fun initVariables() {
        state = RecordState.BEFORE_RECORDING
    }

    private fun startRecording() {
        // MediaRecorder 공식문서를 참조하여 각각의 상태를 확인
        recorder = MediaRecorder()
            .apply {
                setAudioSource(MediaRecorder.AudioSource.MIC)
                setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
                setAudioEncoder(MediaRecorder.AudioEncoder.AAC)
                setOutputFile(recordingFilePath)
                prepare()
            }
        recorder?.start()
        binding.visualizerView.startVisualizing(false)
        binding.recordTimeTextView.startCountUp()
        state = RecordState.ON_RECORDING
    }

    private fun stopRecording() {
        recorder?.run {
            stop()
            release() // 메모리 해제
            saveToMediaStore()
        }
        recorder = null
        binding.visualizerView.stopVisualizing()
        binding.recordTimeTextView.stopCountUp()
        state = RecordState.AFTER_RECORDING
    }

    private fun startPlaying() {
        // MediaPlayer 공식문서를 참조하여 각각의 상태를 확인
        player = MediaPlayer()
            .apply {
                setDataSource(recordingFilePath)
                prepare()
            }
        player?.setOnCompletionListener {
            // 전달된 파일이 전부 다 재생되었을 때 동작하는 리스너
            stopPlaying()
            state = RecordState.AFTER_RECORDING
        }
        player?.start()
        binding.visualizerView.startVisualizing(true)
        binding.recordTimeTextView.startCountUp()
        state = RecordState.ON_PLAYING
    }

    private fun stopPlaying() {
        player?.release() // MediaRecorder와 달리 release() 를 하면 바로 end 상태로 가기 때문에 release만 해준다.
        player = null
        binding.visualizerView.stopVisualizing()
        binding.recordTimeTextView.stopCountUp()
        state = RecordState.AFTER_RECORDING
    }

    private fun saveToMediaStore() {
        Log.d("TEST", "filePath = $recordingFilePath")
        val recordingFile = File(recordingFilePath)

        val sdf = SimpleDateFormat("yyyyMMddHHmmss", Locale.KOREA)
        val tempFileName = "monkey_${sdf.format(Date())}.mp3"

        val values = ContentValues().apply {
            put(MediaStore.Audio.Media.DISPLAY_NAME, tempFileName)
            put(MediaStore.Audio.Media.MIME_TYPE, "audio/mp3")
            put(MediaStore.Audio.Media.DATE_ADDED, Date().time)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                put(MediaStore.Audio.Media.IS_PENDING, 1) // 1로 설정할 경우 보류중인 파일로 구분되어 다른 앱에 바로 노출되지 않아 다른 앱의 요청을 무시할 수 있음
                put(MediaStore.Audio.Media.RELATIVE_PATH, Environment.DIRECTORY_MUSIC + "/MonkeysPhone/")
            }
        }

        // MediaStore에 파일을 등록하고, 등록된 Uri를 item 변수에 저장
        val uri = contentResolver.insert(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, values)

        // 파일디스크럽터를 열고 descriptor 변수에 저장하기. 파일디스크럽터로 파일을 읽거나 쓸 수 있다
        if (uri != null) {
            contentResolver.openFileDescriptor(uri, "w" ,null).use {  descriptor ->
                descriptor?.let {
                    val fos = FileOutputStream(descriptor.fileDescriptor)
                    val bytes = recordingFile.readBytes()
                    fos.write(bytes)
                    fos.close()

                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                        values.clear()
                        values.put(MediaStore.Audio.Media.IS_PENDING, 0)
                        contentResolver.update(uri, values, null, null)
                    }
                }
            }
        }

        Log.d("TEST", "check path = ${recordingFile.path}")
        Log.d("TEST", "file exist? = ${recordingFile.exists()}")




    }

    companion object {
        private const val REQUEST_RECORD_AUDIO_PERMISSION = 201

        val permissions = arrayOf(
            Manifest.permission.READ_EXTERNAL_STORAGE,
            Manifest.permission.WRITE_EXTERNAL_STORAGE,
            Manifest.permission.RECORD_AUDIO
        )


    }

}
