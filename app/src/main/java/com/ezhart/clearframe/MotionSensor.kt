package com.kitesystems.nix.frame

import android.util.Log
import kotlinx.coroutines.delay
import org.greenrobot.eventbus.EventBus

private const val TAG = "MotionSensor"

data object MotionDetectedEvent

class MotionSensor {

    var noActivityCounter: Long = 0

    var isAlreadyInSleepMode: Boolean = false

    external fun readMotionSensor(): Int

    external fun readMotionSensorPower(): Boolean

    external fun setMotionSensorPower(b: Boolean)

    external fun setWakeOnMotion(b: Boolean): Int

    var isWatchingForMotion = false

    suspend fun start() {
        if (!sensorEnabled) {
            Log.d(TAG, "Enabling sensor")
            sensorEnabled = true
           // setWakeOnMotion(true)
        }

        isWatchingForMotion = true

        while(isWatchingForMotion) {

            if(isMotionDetected){
                EventBus.getDefault().post(MotionDetectedEvent)
            }

            delay(1000)
        }
    }

    fun stop(){
        isWatchingForMotion = false
    }

    @get:Synchronized
    private val isMotionDetected: Boolean
        get() {
            if (HAVE_GPIO) {
                return readMotionSensor() > 0
            }
            return false
        }

    @get:Synchronized
    @set:Synchronized
    private var sensorEnabled: Boolean
        get() {
            if (HAVE_GPIO) {
                return readMotionSensorPower()
            }
            return false
        }
        set(enabled) {
            if (HAVE_GPIO) {
                setMotionSensorPower(enabled)
            }
        }

    companion object {
        private var HAVE_GPIO = false
        private const val LIBRARY_NAME = "gpio_jni"

        init {
            HAVE_GPIO = false
            try {
                System.loadLibrary(LIBRARY_NAME)
                HAVE_GPIO = true
                Log.d(TAG, "Motion Sensor initialized")
            } catch (e: UnsatisfiedLinkError) {
                Log.d(TAG, "Could not load library ${LIBRARY_NAME}: ${e.message}")
            }
        }
    }
}