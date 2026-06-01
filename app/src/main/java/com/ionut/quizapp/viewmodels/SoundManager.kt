package com.ionut.quizapp.viewmodels

import android.content.Context
import android.content.SharedPreferences
import android.media.AudioAttributes
import android.media.MediaPlayer
import android.media.SoundPool
import com.ionut.quizapp.R

class SoundManager(private val context: Context) {

    private var soundPool: SoundPool

    // Playere pentru sunete lungi
    private var bgMediaPlayer: MediaPlayer? = null
    private var timerMediaPlayer: MediaPlayer? = null

    // ID-urile pentru efectele sonore scurte (SFX)
    private var correctSoundId = 0
    private var wrongSoundId = 0
    private var finishSoundId = 0

    // Setări pentru utilizator
    private val prefs: SharedPreferences = context.getSharedPreferences("quiz_settings", Context.MODE_PRIVATE)

    var isMusicEnabled: Boolean
        get() = prefs.getBoolean("music_enabled", true)
        set(value) {
            prefs.edit().putBoolean("music_enabled", value).apply()
        }

    var isSoundEnabled: Boolean
        get() = prefs.getBoolean("sfx_enabled", true)
        set(value) {
            prefs.edit().putBoolean("sfx_enabled", value).apply()
        }

    var isVibrationEnabled: Boolean
        get() = prefs.getBoolean("vibration_enabled", true)
        set(value) {
            prefs.edit().putBoolean("vibration_enabled", value).apply()
        }

    init {
        // Configurăm SoundPool pentru efectele scurte
        val audioAttributes = AudioAttributes.Builder()
            .setUsage(AudioAttributes.USAGE_GAME)
            .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
            .build()

        soundPool = SoundPool.Builder()
            .setMaxStreams(5)
            .setAudioAttributes(audioAttributes)
            .build()

        // Încărcăm fișierele scurte (Asigură-te că fișierele .mp3/.ogg există în res/raw)
        correctSoundId = soundPool.load(context, R.raw.correct, 1)
        wrongSoundId = soundPool.load(context, R.raw.wrong, 1)
        finishSoundId = soundPool.load(context, R.raw.quiz_finish, 1)
    }

    // ==========================================
    // CONTROALE PENTRU EFECTE SONORE (SFX)
    // ==========================================

    fun playCorrect() {
        if (isSoundEnabled) soundPool.play(correctSoundId, 0.15f, 0.15f, 0, 0, 1f)
    }

    fun playWrong() {
        if (isSoundEnabled) soundPool.play(wrongSoundId, 0.15f, 0.15f, 0, 0, 1f)
    }

    fun playFinish() {
        if (isSoundEnabled) soundPool.play(finishSoundId, 0.6f, 0.6f, 0, 0, 1f)
    }

    fun stopAllSFX() {
        soundPool.autoPause()
    }

    // ==========================================
    // CONTROALE PENTRU TIMER
    // ==========================================

    fun playTimerWarning() {
        if (!isMusicEnabled) return

        if (timerMediaPlayer == null) {
            timerMediaPlayer = MediaPlayer.create(context, R.raw.timer_tick)
            timerMediaPlayer?.setVolume(0.4f, 0.4f)
        }

        if (timerMediaPlayer?.isPlaying == false) {
            timerMediaPlayer?.start()
        }
    }

    fun stopTimerWarning() {
        timerMediaPlayer?.stop()
        timerMediaPlayer?.release()
        timerMediaPlayer = null
    }

    // ==========================================
    // CONTROALE PENTRU MUZICĂ (BGM)
    // ==========================================

    fun playBGM() {
        if (!isMusicEnabled) return

        if (bgMediaPlayer == null) {
            bgMediaPlayer = MediaPlayer.create(context, R.raw.bg_music)
            bgMediaPlayer?.isLooping = true
            bgMediaPlayer?.setVolume(0.1f, 0.1f)

            bgMediaPlayer?.setOnErrorListener { mp, _, _ ->
                mp.reset()
                bgMediaPlayer = null
                true
            }
        }

        if (bgMediaPlayer?.isPlaying == false) {
            bgMediaPlayer?.start()
        }
    }

    fun pauseBGM() {
        if (bgMediaPlayer?.isPlaying == true) {
            bgMediaPlayer?.pause()
        }
    }

    fun stopBGM() {
        bgMediaPlayer?.stop()
        bgMediaPlayer?.release()
        bgMediaPlayer = null
    }

    fun releaseAll() {
        soundPool.release()
        stopTimerWarning()
        stopBGM()
    }
}