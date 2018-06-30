package com.prisyazhnuy.videoeditor

import android.app.Activity
import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import android.provider.MediaStore
import android.content.Intent
import android.net.Uri
import android.support.v7.app.AlertDialog
import android.util.Log
import android.view.Menu
import com.github.hiteshsondhi88.libffmpeg.FFmpeg
import com.github.hiteshsondhi88.libffmpeg.FFmpegExecuteResponseHandler
import com.github.hiteshsondhi88.libffmpeg.FFmpegLoadBinaryResponseHandler
import com.tbruyelle.rxpermissions2.RxPermissions
import kotlinx.android.synthetic.main.activity_main.*
import android.view.MenuItem
import java.io.File


class MainActivity : AppCompatActivity() {

    companion object {
        private val REQUEST_VIDEO_CAPTURE = 1
        private val REQUEST_VIDEO_GALLERY = 2
        private val REQUEST_AUDIO_PICKER = 3
    }

    private var selectedVideoPath: String? = null
    private var audioFilePath: String? = null

    private val ffmpeg by lazy {
        FFmpeg.getInstance(this).apply {
            loadBinary(object : FFmpegLoadBinaryResponseHandler {
                override fun onFinish() {
                    Log.d("TAG", "onFinish")
                }

                override fun onSuccess() {
                    Log.d("TAG", "onSuccess")
                }

                override fun onFailure() {
                    Log.d("TAG", "onFailure")

                }

                override fun onStart() {
                    Log.d("TAG", "onStart")

                }

            })
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        btnRecord.setOnClickListener { dispatchTakeVideoIntent() }
        btnOpenGallery.setOnClickListener { openGalleryForVideo() }
        btnPlayStop.setOnClickListener { playStopVideo() }
        with(RxPermissions(this)) {
            request(android.Manifest.permission.WRITE_EXTERNAL_STORAGE)
                    .subscribe { granted ->
                        if (!granted) {
                            finish()
                        }
                    }
        }
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.main_menu, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem?): Boolean {
        when (item?.itemId) {
            R.id.action_mute -> muteVideo()
            R.id.action_cut -> cutVideo()
            R.id.action_load_audio -> openAudioPicker()
            R.id.action_add_music -> changeAudio()
            R.id.action_details -> viewVideoInformation()
            R.id.action_custom -> scaleVideo()
        }
        return super.onOptionsItemSelected(item)
    }

    private fun dispatchTakeVideoIntent() {
        val takeVideoIntent = Intent(MediaStore.ACTION_VIDEO_CAPTURE)
        if (takeVideoIntent.resolveActivity(packageManager) != null) {
            startActivityForResult(takeVideoIntent, REQUEST_VIDEO_CAPTURE)
        }
    }

    private fun openGalleryForVideo() {
        val i = Intent(Intent.ACTION_PICK, android.provider.MediaStore.Video.Media.EXTERNAL_CONTENT_URI).apply {
            type = "video/*"
        }
        startActivityForResult(i, REQUEST_VIDEO_GALLERY)
    }

    private fun openAudioPicker() {
        Intent(Intent.ACTION_PICK, android.provider.MediaStore.Audio.Media.EXTERNAL_CONTENT_URI).apply {
            startActivityForResult(this, REQUEST_AUDIO_PICKER)
        }
    }

    fun getVideoPathFromURI(contentUri: Uri): String? {
        var res: String? = null
        val proj = arrayOf(MediaStore.Video.Media.DATA)
        val cursor = contentResolver.query(contentUri, proj, null, null, null)
        if (cursor!!.moveToFirst()) {
            val column_index = cursor.getColumnIndexOrThrow(MediaStore.Video.Media.DATA)
            res = cursor.getString(column_index)
        }
        cursor.close()
        return res
    }

    fun getAudioPathFromURI(contentUri: Uri): String? {
        var res: String? = null
        val proj = arrayOf(MediaStore.Audio.Media.DATA)
        val cursor = contentResolver.query(contentUri, proj, null, null, null)
        if (cursor!!.moveToFirst()) {
            val column_index = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DATA)
            res = cursor.getString(column_index)
        }
        cursor.close()
        return res
    }

    private fun executeCmd(cmd: Array<String?>) {
        ffmpeg.killRunningProcesses()
        ffmpeg.execute(cmd, object : FFmpegExecuteResponseHandler {
            override fun onFinish() {
                Log.d("TAG", "onFinish")
                AlertDialog.Builder(this@MainActivity).apply {
                    setMessage("Finished")
                    setCancelable(true)
                    show()
                }
            }

            override fun onSuccess(message: String?) {
                Log.d("TAG", "onSuccess $message")
                AlertDialog.Builder(this@MainActivity).apply {
                    setMessage(message)
                    setCancelable(true)
                    show()
                }
            }

            override fun onFailure(message: String?) {
                Log.d("TAG", "onFailure $message")
                AlertDialog.Builder(this@MainActivity).apply {
                    setMessage(message)
                    setCancelable(true)
                    show()
                }
            }

            override fun onProgress(message: String?) {
                Log.d("TAG", "onProgress $message")
                tvProgress.text = message
            }

            override fun onStart() {
                Log.d("TAG", "onStart")

            }
        })
    }

    private fun cutVideo() {
        val startTime = "00:00:00.0"
        val duration = "00:00:05.0"
        val output = selectedVideoPath?.replace(".avi", "(1).avi")?.replace(".mp4", "(1).mp4")
        File(output).delete()
        val cmd = arrayOf("-i", selectedVideoPath, "-qscale", "0", "-ss", startTime, "-t", duration, output)
        executeCmd(cmd)
    }

    private fun muteVideo() {
        val output = selectedVideoPath?.replace(".avi", "(1).avi")?.replace(".mp4", "(1).mp4")
        File(output).delete()
        val cmd = arrayOf("-i", selectedVideoPath, "-c", "copy", "-an", output)
        executeCmd(cmd)
    }

    private fun changeAudio() {
        val output = selectedVideoPath?.replace(".avi", "(1).avi")?.replace(".mp4", "(1).mp4")
        File(output).delete()
        val cmd = arrayOf("-i", selectedVideoPath, "-i", audioFilePath, "-c:v", "copy", "-map", "0:v:0", "-map", "1:a:0", "-shortest", output)
        executeCmd(cmd)
    }

    private fun viewVideoInformation() {
        val cmd = arrayOf("-i", selectedVideoPath, "-hide_banner")
        executeCmd(cmd)
    }

    private fun scaleVideo() {
        val output = selectedVideoPath?.replace(".avi", "(1).avi")?.replace(".mp4", "(1).mp4")
        File(output).delete()
        val cmd = arrayOf("-i", selectedVideoPath, "-vf", "scale=1280:720", output)
        executeCmd(cmd)
    }

    private fun playStopVideo() {
        with(videoView) {
            if (isPlaying) {
                pause()
                btnPlayStop.setText(R.string.play)
            } else {
                start()
                btnPlayStop.setText(R.string.stop)
            }
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, intent: Intent?) {
        if (resultCode == Activity.RESULT_OK) {
            when (requestCode) {
                REQUEST_VIDEO_CAPTURE,
                REQUEST_VIDEO_GALLERY -> {
                    intent?.data?.let {
                        selectedVideoPath = getVideoPathFromURI(it)
                        with(videoView) {
                            setVideoURI(it)
                            start()
                        }
                    }
                }
                REQUEST_AUDIO_PICKER -> {
                    intent?.data?.let {
                        audioFilePath = getAudioPathFromURI(it)
                    }
                }
            }
        }
    }
}
