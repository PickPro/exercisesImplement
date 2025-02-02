package com.ooplab.exercises_fitfuel

import android.app.Activity
import android.media.MediaPlayer
import kotlinx.coroutines.Job
import kotlin.random.Random

class SoundManager(var activity: Activity) {
    private var noActivityJob: Job? = null

    fun stopAllSounds() {
        // Logic to stop all playing sounds
        noActivityJob?.cancel()
    }

    fun setNoActivityJob(job: Job) {
        noActivityJob = job
    }
    // List of sound resources for "up" and "down" stages
    private val upSounds = listOf(
        R.raw.up_1, R.raw.up_2, R.raw.up_3, R.raw.up_4, R.raw.up_5,
        R.raw.up_6, R.raw.up_7, R.raw.up_8, R.raw.up_10
    )
    private val downSounds = listOf(
        R.raw.down_1, R.raw.down_2, R.raw.down_3, R.raw.down_4, R.raw.down_5,
        R.raw.down_6, R.raw.down_7, R.raw.down_8, R.raw.down_9, R.raw.down_10
    )
    private val motivationalSounds = listOf(
        R.raw.motivational1, R.raw.motivational2, R.raw.motivational3, R.raw.motivational4, R.raw.motivational5, R.raw.motivational6,R.raw.motivational7,R.raw.motivational8,R.raw.motivational9,R.raw.motivational10,
    )
    private val noActivitySounds = listOf(
        R.raw.no_activity_1, R.raw.no_activity_2, R.raw.no_activity_3, R.raw.no_activity_4, R.raw.no_activity_5, R.raw.no_activity_6,R.raw.no_activity_7,R.raw.no_activity_8,R.raw.no_activity_9,R.raw.no_activity_10
    )
    //plank sounds
    private val plankSounds = listOf(
        R.raw.plank_1, R.raw.plank_2, R.raw.plank_3, R.raw.plank_4, R.raw.plank_5, R.raw.plank_6,R.raw.plank_7,R.raw.plank_8,R.raw.plank_9,R.raw.plank_10, R.raw.plank_11, R.raw.plank_12, R.raw.plank_13, R.raw.plank_14, R.raw.plank_15
    )
    private val notPlankSounds = listOf(
        R.raw.not_plank_1, R.raw.not_plank_2, R.raw.not_plank_3, R.raw.not_plank_4, R.raw.not_plank_5, R.raw.not_plank_6,R.raw.not_plank_7,R.raw.not_plank_8,R.raw.not_plank_9,R.raw.not_plank_10, R.raw.not_plank_11, R.raw.not_plank_12, R.raw.not_plank_13, R.raw.not_plank_14, R.raw.not_plank_15
    )
    private val completeSounds = listOf(
        R.raw.not_plank_1, R.raw.not_plank_2, R.raw.not_plank_3, R.raw.not_plank_4, R.raw.not_plank_5, R.raw.not_plank_6,R.raw.not_plank_7,R.raw.not_plank_8,R.raw.not_plank_9,R.raw.not_plank_10, R.raw.not_plank_11, R.raw.not_plank_12, R.raw.not_plank_13, R.raw.not_plank_14, R.raw.not_plank_15
    )

    private val jumpingJackInSounds = listOf(
        R.raw.not_plank_1, R.raw.not_plank_2, R.raw.not_plank_3, R.raw.not_plank_4, R.raw.not_plank_5, R.raw.not_plank_6,R.raw.not_plank_7,R.raw.not_plank_8,R.raw.not_plank_9,R.raw.not_plank_10, R.raw.not_plank_11, R.raw.not_plank_12, R.raw.not_plank_13, R.raw.not_plank_14, R.raw.not_plank_15

    )

    private val jumpingJackOutSounds = listOf(
        R.raw.not_plank_1, R.raw.not_plank_2, R.raw.not_plank_3, R.raw.not_plank_4, R.raw.not_plank_5, R.raw.not_plank_6,R.raw.not_plank_7,R.raw.not_plank_8,R.raw.not_plank_9,R.raw.not_plank_10, R.raw.not_plank_11, R.raw.not_plank_12, R.raw.not_plank_13, R.raw.not_plank_14, R.raw.not_plank_15
    )

//    private val completeSounds = listOf(
//        R.raw.complete_1, R.raw.complete_2, R.raw.complete_3, R.raw.complete_4, R.raw.complete_5, R.raw.complete_6,R.raw.complete_7,R.raw.complete_8,R.raw.complete_9,R.raw.complete_10, R.raw.complete_11, R.raw.complete_12, R.raw.complete_13
//    )
//
//    private val jumpingJackInSounds = listOf(
//        R.raw.jumping_jack_in_1, R.raw.jumping_jack_in_2, R.raw.jumping_jack_in_3, R.raw.jumping_jack_in_4, R.raw.jumping_jack_in_5, R.raw.jumping_jack_in_6,R.raw.jumping_jack_in_7,R.raw.jumping_jack_in_8
//
//    )
//
//    private val jumpingJackOutSounds = listOf(
//        R.raw.jumping_jack_out_1, R.raw.jumping_jack_out_2, R.raw.jumping_jack_out_3, R.raw.jumping_jack_out_4, R.raw.jumping_jack_out_5
//    )


    private lateinit var mediaPlayer: MediaPlayer

    // Function to play a random sound from a given list
    private fun playRandomSound( soundList: List<Int>) {
        if (::mediaPlayer.isInitialized && mediaPlayer.isPlaying) {
            // If a sound is already playing, do not start a new one
            return
        }
        val randomSound = soundList[Random.nextInt(soundList.size)]
        mediaPlayer = MediaPlayer.create(activity, randomSound)
        mediaPlayer.start()
    }

    // Function to play "up" sound
    fun playUpSound() {
        playRandomSound(upSounds)
    }

    // Function to play "down" sound
    fun playDownSound() {
        val combinedSounds = motivationalSounds + downSounds
        playRandomSound(combinedSounds)
    }

    fun playNoActivitySound() {
        playRandomSound(noActivitySounds)
    }

    fun playPlankSound() {
        playRandomSound(plankSounds)
    }
    fun playNotPlankSound() {
        playRandomSound(notPlankSounds)
    }
    fun playCompleteSound() {
        playRandomSound(completeSounds)
    }

    fun playJumpingJackInSound() {
        playRandomSound(jumpingJackInSounds)
    }
    fun playJumpingJackOutSound() {
        playRandomSound(jumpingJackOutSounds)
    }


}