package com.example.reddittube.utils

import android.content.ContentValues
import android.content.Context
import android.media.MediaCodec
import android.media.MediaExtractor
import android.media.MediaFormat
import android.media.MediaMuxer
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.BufferedInputStream
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.net.HttpURLConnection
import java.net.URL
import java.nio.ByteBuffer

// ponytail: Simplified media downloader and muxer using entirely native platform APIs.
object DownloadHelper {

    suspend fun downloadRedditVideo(
        context: Context,
        fallbackUrl: String,
        dashUrl: String,
        title: String,
        onProgress: (String) -> Unit,
        onComplete: (Boolean, String?) -> Unit
    ) {
        withContext(Dispatchers.IO) {
            try {
                onProgress("Downloading video...")
                val tempVideoFile = File(context.cacheDir, "temp_video_${System.currentTimeMillis()}.mp4")
                downloadFile(fallbackUrl, tempVideoFile)

                var tempAudioFile: File? = null
                if (dashUrl.isNotEmpty()) {
                    onProgress("Checking audio...")
                    try {
                        val dashXml = fetchText(dashUrl)
                        val audioFileMatch = Regex("<BaseURL>([^<]*audio[^<]*\\.mp4)</BaseURL>", RegexOption.IGNORE_CASE).find(dashXml)
                        val audioFilename = audioFileMatch?.groupValues?.get(1)
                        if (audioFilename != null) {
                            val baseUrl = fallbackUrl.substringBeforeLast("/") + "/"
                            val audioUrl = baseUrl + audioFilename
                            
                            onProgress("Downloading audio...")
                            val audioFile = File(context.cacheDir, "temp_audio_${System.currentTimeMillis()}.mp4")
                            downloadFile(audioUrl, audioFile)
                            tempAudioFile = audioFile
                        }
                    } catch (e: Exception) {
                        Log.e("DownloadHelper", "Audio check failed, falling back to video-only: ${e.message}")
                    }
                }

                onProgress("Saving video...")
                val cleanTitle = title.replace(Regex("[^a-zA-Z0-9_-]"), "_").take(50)
                val displayName = "RedditTube_${cleanTitle}_${System.currentTimeMillis()}.mp4"

                val contentValues = ContentValues().apply {
                    put(MediaStore.MediaColumns.DISPLAY_NAME, displayName)
                    put(MediaStore.MediaColumns.MIME_TYPE, "video/mp4")
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                        put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS + "/RedditTube")
                        put(MediaStore.MediaColumns.IS_PENDING, 1)
                    }
                }

                val resolver = context.contentResolver
                val collection = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    MediaStore.Downloads.EXTERNAL_CONTENT_URI
                } else {
                    @Suppress("DEPRECATION")
                    MediaStore.Video.Media.EXTERNAL_CONTENT_URI
                }

                val uri = resolver.insert(collection, contentValues)
                if (uri != null) {
                    val tempOutputFile = File(context.cacheDir, "temp_output_${System.currentTimeMillis()}.mp4")
                    
                    muxVideoAndAudio(tempVideoFile, tempAudioFile, tempOutputFile)

                    resolver.openFileDescriptor(uri, "w")?.use { pfd ->
                        FileInputStream(tempOutputFile).use { input ->
                            FileOutputStream(pfd.fileDescriptor).use { output ->
                                input.copyTo(output)
                            }
                        }
                    }

                    if (tempOutputFile.exists()) tempOutputFile.delete()

                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                        contentValues.clear()
                        contentValues.put(MediaStore.MediaColumns.IS_PENDING, 0)
                        resolver.update(uri, contentValues, null, null)
                    }

                    if (tempVideoFile.exists()) tempVideoFile.delete()
                    tempAudioFile?.let { if (it.exists()) it.delete() }

                    withContext(Dispatchers.Main) {
                        onComplete(true, displayName)
                    }
                } else {
                    throw Exception("MediaStore insert failed")
                }
            } catch (e: Exception) {
                Log.e("DownloadHelper", "Download failed: ${e.message}", e)
                withContext(Dispatchers.Main) {
                    onComplete(false, e.message)
                }
            }
        }
    }

    private fun openConnection(urlString: String): HttpURLConnection {
        val url = URL(urlString)
        val connection = url.openConnection() as HttpURLConnection
        connection.setRequestProperty("User-Agent", "RedditTube/1.0 (by /u/reddittube_app)")
        connection.connectTimeout = 15000
        connection.readTimeout = 15000
        connection.connect()
        if (connection.responseCode >= 400) {
            throw Exception("HTTP ${connection.responseCode} for $urlString")
        }
        return connection
    }

    private fun downloadFile(urlString: String, outputFile: File) {
        val connection = openConnection(urlString)
        BufferedInputStream(connection.inputStream).use { input ->
            FileOutputStream(outputFile).use { output ->
                input.copyTo(output)
            }
        }
    }

    private fun fetchText(urlString: String): String {
        return openConnection(urlString).inputStream.bufferedReader().use { it.readText() }
    }

    private fun muxVideoAndAudio(videoFile: File, audioFile: File?, outputFile: File) {
        if (audioFile == null || !audioFile.exists() || audioFile.length() == 0L) {
            videoFile.copyTo(outputFile, overwrite = true)
            return
        }

        val videoExtractor = MediaExtractor()
        videoExtractor.setDataSource(videoFile.absolutePath)

        val audioExtractor = MediaExtractor()
        audioExtractor.setDataSource(audioFile.absolutePath)

        val muxer = MediaMuxer(outputFile.absolutePath, MediaMuxer.OutputFormat.MUXER_OUTPUT_MPEG_4)

        var videoTrackIndex = -1
        for (i in 0 until videoExtractor.trackCount) {
            val format = videoExtractor.getTrackFormat(i)
            val mime = format.getString(MediaFormat.KEY_MIME) ?: ""
            if (mime.startsWith("video/")) {
                videoExtractor.selectTrack(i)
                videoTrackIndex = muxer.addTrack(format)
                break
            }
        }

        var audioTrackIndex = -1
        for (i in 0 until audioExtractor.trackCount) {
            val format = audioExtractor.getTrackFormat(i)
            val mime = format.getString(MediaFormat.KEY_MIME) ?: ""
            if (mime.startsWith("audio/")) {
                audioExtractor.selectTrack(i)
                audioTrackIndex = muxer.addTrack(format)
                break
            }
        }

        muxer.start()

        val videoBuffer = ByteBuffer.allocate(1024 * 1024)
        val videoBufferInfo = MediaCodec.BufferInfo()
        while (true) {
            videoBufferInfo.offset = 0
            videoBufferInfo.size = videoExtractor.readSampleData(videoBuffer, 0)
            if (videoBufferInfo.size < 0) break
            videoBufferInfo.presentationTimeUs = videoExtractor.sampleTime
            videoBufferInfo.flags = videoExtractor.sampleFlags
            muxer.writeSampleData(videoTrackIndex, videoBuffer, videoBufferInfo)
            videoExtractor.advance()
        }

        if (audioTrackIndex != -1) {
            val audioBuffer = ByteBuffer.allocate(1024 * 1024)
            val audioBufferInfo = MediaCodec.BufferInfo()
            while (true) {
                audioBufferInfo.offset = 0
                audioBufferInfo.size = audioExtractor.readSampleData(audioBuffer, 0)
                if (audioBufferInfo.size < 0) break
                audioBufferInfo.presentationTimeUs = audioExtractor.sampleTime
                audioBufferInfo.flags = audioExtractor.sampleFlags
                muxer.writeSampleData(audioTrackIndex, audioBuffer, audioBufferInfo)
                audioExtractor.advance()
            }
        }

        try {
            muxer.stop()
        } catch (e: Exception) {
            Log.e("DownloadHelper", "Muxer stop failed", e)
        } finally {
            muxer.release()
            videoExtractor.release()
            audioExtractor.release()
        }
    }
}
