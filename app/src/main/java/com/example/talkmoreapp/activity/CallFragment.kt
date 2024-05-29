package com.example.talkmoreapp.activity

import android.os.Bundle
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import com.example.talkmoreapp.R
import com.example.talkmoreapp.databinding.FragmentCallBinding
import com.example.talkmoreapp.utils.Constants
import com.example.talkmoreapp.viewmodel.CallViewModel
import org.linphone.core.Call

// TODO: Rename parameter arguments, choose names that match
// the fragment initialization parameters, e.g. ARG_ITEM_NUMBER
private const val ARG_PARAM1 = "param1"
private const val ARG_PARAM2 = "param2"

/**
 * A simple [Fragment] subclass.
 * Use the [CallFragment.newInstance] factory method to
 * create an instance of this fragment.
 */

private const val TAG = "CallFragment"

class CallFragment : DialogFragment() {
    // TODO: Rename and change types of parameters
    private var param1: String? = null
    private var callDirection: String? = null

    private val callViewModel:CallViewModel by activityViewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setStyle(DialogFragment.STYLE_NORMAL, R.style.FullScreenDialogStyle);
        arguments?.let {
            param1 = it.getString(Constants.REMOTE_FROM)
            callDirection = it.getString(Constants.CALL_DIRECTION)
        }
    }

    private lateinit var binding:FragmentCallBinding

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        binding = FragmentCallBinding.inflate(inflater, container, false)
        val view = binding.root
        binding.callName.text = param1
        return view
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        if (callDirection == "outgoing"){
            binding.callPanel.visibility = View.GONE
            binding.optionsButton.visibility = View.VISIBLE
        }else{
            binding.callPanel.visibility = View.VISIBLE
            binding.optionsButton.visibility = View.GONE
        }

        callViewModel.callState.observe(viewLifecycleOwner) {state->

            Log.e(TAG, "onViewCreated: $state", )
            
            when(state){
                Call.State.Connected -> {

                }
                Call.State.Released -> {
                    Log.e(TAG, "Call released: true ", )
                }
                Call.State.End -> {
                    Log.e(TAG, "Call end: true ", )
                }
                Call.State.OutgoingInit -> {
                    Log.e(TAG, "onViewCreated: OutgoingInit")
                    binding.callStats.text = "Ringing"
                }
                Call.State.OutgoingRinging-> {
                    Log.e(TAG, "CallFragment: OutgoingRinging")
                    binding.callStats.text = "Ringing"
                }
                Call.State.OutgoingProgress-> {
                    Log.e(TAG, "CallFragment: OutgoingProgress" )
                    binding.optionsButton.visibility = View.VISIBLE
                    binding.callPanel.visibility = View.GONE
                    binding.callStats.text = "Ringing"
                }
                Call.State.StreamsRunning -> {

                    Log.e(TAG, "call duration: ${callViewModel.currentCall?.duration}")

                    if (callViewModel.currentCall?.dir == Call.Dir.Incoming){
                        binding.callStats.text = displayStats(callViewModel.currentCall)
                        Log.e(TAG, "Call connected: streamsRunning")
                        binding.callPanel.visibility = View.GONE
                        binding.optionsButton.visibility = View.VISIBLE
                    }else{
                        binding.callStats.text = displayStats(callViewModel.currentCall)
                        Log.e(TAG, "Outbound call streaming in progress")
                        binding.optionsButton.visibility = View.VISIBLE
                    }

                }
                else -> {null}
            }

        }

        callViewModel.isMuted().observe(viewLifecycleOwner){mute->
            Log.e(TAG, "onViewCreated: mic -> $mute")
            if (mute){
                binding.muteBtn.setImageResource(R.drawable.mute_mic)
            }else{
                binding.muteBtn.setImageResource(R.drawable.mic_on)
            }
        }

        callViewModel.isSpeaker().observe(viewLifecycleOwner){speaker ->
            Log.e(TAG, "onViewCreated: speaker -> $speaker")
            if (speaker){
                binding.btnSpeaker.setImageResource(R.drawable.speaker_on)
            }else{
                binding.btnSpeaker.setImageResource(R.drawable.speaker_off)
            }
        }

        binding.answerBtn.setOnClickListener {
            try {
                callViewModel.answerCall()
            }catch (err:Exception){
                err.printStackTrace()
                Log.e(TAG, "onViewCreated: $err", )
            }

        }

        binding.btnSpeaker.setOnClickListener{
            callViewModel.toggleSpeaker()
        }

        binding.muteBtn.setOnClickListener {
            callViewModel.mute()
        }

        binding.declineBtn.setOnClickListener {
            callViewModel.declineCall()
        }

        binding.endCallBtn.setOnClickListener {
            callViewModel.declineCall()
        }
    }

    private fun displayStats(currentCall: Call?): CharSequence? {
        var stats = ""
        if (currentCall != null){
            val durationInSeconds = currentCall.duration
            val minutes = durationInSeconds / 60
            val seconds = durationInSeconds % 60
            stats = String.format("%02d:%02d", minutes, seconds)
        }
        return stats
    }

//
//    companion object {
//        /**
//         * Use this factory method to create a new instance of
//         * this fragment using the provided parameters.
//         *
//         * @param param1 Parameter 1.
//         * @param param2 Parameter 2.
//         * @return A new instance of fragment CallFragment.
//         */
//        // TODO: Rename and change types and number of parameters
//        @JvmStatic
//        fun newInstance(param1: String, param2: String) =
//            CallFragment().apply {
//                arguments = Bundle().apply {
//                    putString(ARG_PARAM1, param1)
//                    putString(ARG_PARAM2, param2)
//                }
//            }
//    }
}