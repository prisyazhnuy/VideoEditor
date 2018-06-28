package com.prisyazhnuy.videoeditor

import android.app.Activity
import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import android.provider.MediaStore
import android.content.Intent
import android.net.Uri
import com.github.hiteshsondhi88.libffmpeg.ExecuteBinaryResponseHandler
import com.github.hiteshsondhi88.libffmpeg.FFmpeg
import com.github.hiteshsondhi88.libffmpeg.FFmpegExecuteResponseHandler
import kotlinx.android.synthetic.main.activity_main.*

class MainActivity : AppCompatActivity() {

    companion object {
        private val REQUEST_VIDEO_CAPTURE = 1
        private val REQUEST_VIDEO_GALLERY = 2
    }

    private var selectedVideoPath: String? = null

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

    fun getRealPathFromURI(contentUri: Uri): String? {
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

    private fun muteVideo() {
        with(FFmpeg.getInstance(this)) {
            execute(arrayOf("-version"), object : FFmpegExecuteResponseHandler{
                override fun onFinish() {

                }

                override fun onSuccess(message: String?) {
                }

                override fun onFailure(message: String?) {
                }

                override fun onProgress(message: String?) {
                }

                override fun onStart() {
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
                        selectedVideoPath = getRealPathFromURI(it)
                        with(videoView) {
                            setVideoURI(it)
                            start()
                        }
                    }
                }
            }
        }
    }
}
