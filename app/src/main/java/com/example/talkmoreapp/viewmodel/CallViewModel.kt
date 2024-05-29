package com.example.talkmoreapp.viewmodel

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import org.linphone.core.AudioDevice
import org.linphone.core.Call
import org.linphone.core.Core
import org.linphone.core.CoreListenerStub


class CallViewModel: ViewModel() {

    var core: Core? = null
    val callState = MutableLiveData<Call.State?>()

    var currentCall: Call? = null
    var coreListener: CoreListenerStub? = null

    fun initCore(core: Core){
        this.core = core
        setupCoreListener()
    }

    private fun setupCoreListener() {
        coreListener = object : CoreListenerStub() {
            override fun onCallStateChanged(
                core: Core,
                call: Call,
                state: Call.State?,
                message: String
            ) {
                currentCall = call
                callState.postValue(state)
            }
        }
        core?.addListener(coreListener)
    }

    fun answerCall() {
        currentCall?.accept()
    }

    fun isMuted(): MutableLiveData<Boolean> {
        return MutableLiveData<Boolean> (currentCall?.microphoneMuted)
    }

    fun isSpeaker(): MutableLiveData<Boolean> {
        return MutableLiveData<Boolean>(currentCall?.speakerMuted)
    }

    fun mute(){
        core?.enableMic(!core?.micEnabled()!!)
    }

    fun toggleSpeaker(){
            // Get the currently used audio device
            val currentAudioDevice = core?.currentCall?.outputAudioDevice
            val speakerEnabled = currentAudioDevice?.type == AudioDevice.Type.Speaker

            // We can get a list of all available audio devices using
            // Note that on tablets for example, there may be no Earpiece device
            for (audioDevice in core?.audioDevices!!) {
                if (speakerEnabled && audioDevice.type == AudioDevice.Type.Earpiece) {
                    core?.currentCall?.outputAudioDevice = audioDevice
                    return
                } else if (!speakerEnabled && audioDevice.type == AudioDevice.Type.Speaker) {
                    core?.currentCall?.outputAudioDevice = audioDevice
                    return
                }/* If we wanted to route the audio to a bluetooth headset
            else if (audioDevice.type == AudioDevice.Type.Bluetooth) {
                core.currentCall?.outputAudioDevice = audioDevice
            }*/
            }
    }

    fun declineCall() {
        currentCall?.terminate()
    }

    override fun onCleared() {
        super.onCleared()
    }

}