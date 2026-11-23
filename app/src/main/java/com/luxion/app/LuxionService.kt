package com.luxion.app

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Intent
import android.content.pm.ServiceInfo
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import android.widget.Toast
import java.util.Locale

class LuxionService : Service(), TextToSpeech.OnInitListener {

    private val channelId = "luxion_channel"
    private val handler = Handler(Looper.getMainLooper())
    private var recognizer: SpeechRecognizer? = null
    private var tts: TextToSpeech? = null
    private var running = false
    private var awake = false
    private var awakeAt = 0L

    private val wakeWords = listOf(
        "lux", "luck", "lex", "lak", "lack", "look", "wake up", "wakeup"
    )

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        createChannel()
        val notification = Notification.Builder(this, channelId)
            .setContentTitle("Luxion")
            .setContentText("Luxion sun raha hai: Wake up Luxion")
            .setSmallIcon(android.R.drawable.ic_btn_speak_now)
            .setOngoing(true)
            .build()

        if (Build.VERSION.SDK_INT >= 29) {
            startForeground(1, notification, ServiceInfo.FOREGROUND_SERVICE_TYPE_MICROPHONE)
        } else {
            startForeground(1, notification)
        }

        if (!running) {
            running = true
            tts = TextToSpeech(this, this)
        }
        return START_STICKY
    }

    override fun onInit(status: Int) {
        val engine = tts ?: return
        if (status != TextToSpeech.SUCCESS) {
            toast("TTS shuru nahi hua")
            return
        }

        engine.language = Locale("en", "IN")
        engine.setPitch(0.7f)
        engine.setSpeechRate(0.95f)
        engine.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
            override fun onStart(utteranceId: String?) {}
            override fun onDone(utteranceId: String?) {
                handler.post { listen() }
            }
            @Deprecated("Deprecated in Java")
            override fun onError(utteranceId: String?) {
                handler.post { listen() }
            }
        })

        if (!SpeechRecognizer.isRecognitionAvailable(this)) {
            toast("Is phone mein speech recognition nahi mila")
            engine.speak(
                "Is phone mein speech recognition nahi mil raha",
                TextToSpeech.QUEUE_FLUSH, null, "nospeech"
            )
            return
        }

        recognizer = SpeechRecognizer.createSpeechRecognizer(this)
        recognizer?.setRecognitionListener(object : RecognitionListener {
            override fun onReadyForSpeech(params: Bundle?) {}
            override fun onBeginningOfSpeech() {}
            override fun onRmsChanged(rmsdB: Float) {}
            override fun onBufferReceived(buffer: ByteArray?) {}
            override fun onEndOfSpeech() {}
            override fun onPartialResults(partialResults: Bundle?) {}
            override fun onEvent(eventType: Int, params: Bundle?) {}

            override fun onError(error: Int) {
                // 6 = kuch bola nahi, 7 = samajh nahi aaya. Ye normal hain.
                if (error != 6 && error != 7) {
                    toast("Speech error number: $error")
                }
                if (awake && System.currentTimeMillis() - awakeAt > 10000) {
                    awake = false
                }
                handler.postDelayed({ listen() }, 600)
            }

            override fun onResults(results: Bundle?) {
                val list = results
                    ?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                    ?: emptyList<String>()
                val heard = list.joinToString(" ").lowercase(Locale.ROOT)
                if (heard.isNotBlank()) {
                    toast("Suna: $heard")
                }
                handleSpeech(heard)
            }
        })

        toast("Luxion sun raha hai")
        listen()
    }

    private fun listen() {
        if (!running) return
        try {
            val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                putExtra(
                    RecognizerIntent.EXTRA_LANGUAGE_MODEL,
                    RecognizerIntent.LANGUAGE_MODEL_FREE_FORM
                )
                putExtra(RecognizerIntent.EXTRA_LANGUAGE, "en-IN")
                putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 5)
                putExtra(
                    RecognizerIntent.EXTRA_SPEECH_INPUT_COMPLETE_SILENCE_LENGTH_MILLIS,
                    1500L
                )
            }
            recognizer?.cancel()
            recognizer?.startListening(intent)
        } catch (e: Exception) {
            toast("Listen error: ${e.message}")
            handler.postDelayed({ listen() }, 1500)
        }
    }

    private fun handleSpeech(heard: String) {
        if (!awake) {
            if (wakeWords.any { heard.contains(it) }) {
                awake = true
                awakeAt = System.currentTimeMillis()
                speak("Haan bolo, main sun raha hu")
            } else {
                listen()
            }
            return
        }

        awake = false
        if (heard.contains("youtube") || heard.contains("you tube")) {
            speak("Theek hai, YouTube khol raha hu")
            openUrl("https://www.youtube.com")
        } else {
            speak("Tumne kaha: $heard")
        }
    }

    private fun openUrl(url: String) {
        try {
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            startActivity(intent)
        } catch (e: Exception) {
            speak("App khol nahi paaya")
        }
    }

    private fun speak(text: String) {
        recognizer?.cancel()
        tts?.speak(text, TextToSpeech.QUEUE_FLUSH, null, "luxion")
    }

    private fun toast(message: String) {
        handler.post {
            Toast.makeText(applicationContext, message, Toast.LENGTH_SHORT).show()
        }
    }

    private fun createChannel() {
        val manager = getSystemService(NotificationManager::class.java)
        val channel = NotificationChannel(
            channelId, "Luxion", NotificationManager.IMPORTANCE_LOW
        )
        manager.createNotificationChannel(channel)
    }

    override fun onDestroy() {
        running = false
        handler.removeCallbacksAndMessages(null)
        recognizer?.destroy()
        tts?.stop()
        tts?.shutdown()
        super.onDestroy()
    }
}
