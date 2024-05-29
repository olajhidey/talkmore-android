package com.example.talkmoreapp.activity

import android.content.Intent
import android.graphics.Color
import android.media.MediaPlayer
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.SearchView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.fragment.app.FragmentManager
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.talkmoreapp.MainActivity
import com.example.talkmoreapp.R
import com.example.talkmoreapp.databinding.ActivityHomeBinding
import com.example.talkmoreapp.model.ApiError
import com.example.talkmoreapp.model.UserResponse
import com.example.talkmoreapp.network.NetworkModel
import com.example.talkmoreapp.utils.Constants
import com.example.talkmoreapp.utils.ListAdapters
import com.example.talkmoreapp.utils.dataStore
import com.example.talkmoreapp.utils.showToast
import com.example.talkmoreapp.viewmodel.CallViewModel
import com.google.gson.Gson
import com.xwray.groupie.GroupieAdapter
import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers
import io.reactivex.rxjava3.disposables.Disposable
import io.reactivex.rxjava3.schedulers.Schedulers
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import org.linphone.core.Account
import org.linphone.core.AudioDevice
import org.linphone.core.Call
import org.linphone.core.Core
import org.linphone.core.CoreListenerStub
import org.linphone.core.Factory
import org.linphone.core.MediaEncryption
import org.linphone.core.RegistrationState
import org.linphone.core.TransportType
import java.util.ArrayList

private const val TAG = "HomeActivity"

class HomeActivity : AppCompatActivity() {

    var disposable: Disposable? = null

    private lateinit var callModel: CallViewModel

    private lateinit var networkModel: NetworkModel

    private lateinit var adapter: GroupieAdapter

    val filteredList = ArrayList<UserResponse>()

    private val coroutineScope = CoroutineScope(Dispatchers.IO)

    val userNamePref = stringPreferencesKey(Constants.USERNAME_MAP_KEY)
    val passwordRef = stringPreferencesKey(Constants.PASSWORD_MAP_KEY)
    val domainRef = stringPreferencesKey(Constants.DOMAIN_MAP_KEY)

    private lateinit var binding: ActivityHomeBinding
    private lateinit var callFragment: CallFragment

    private lateinit var fragmentManager: FragmentManager

    lateinit var core: Core

    private var mediaPlayer: MediaPlayer? = null

    private val coreListener = object : CoreListenerStub() {
        override fun onAccountRegistrationStateChanged(
            core: Core,
            account: Account,
            state: RegistrationState?,
            message: String
        ) {

            if (state == RegistrationState.Failed || state == RegistrationState.Cleared) {
                Log.e(TAG, "registration status: $message")
                binding.status.text = "Offline"
                binding.status.setTextColor(Color.MAGENTA)
            } else if (state == RegistrationState.Ok) {
                Log.e(TAG, "Registration status: $message")
                binding.status.text = "Online"
                binding.status.setTextColor(Color.GREEN)
                Toast.makeText(applicationContext, "Connected!", Toast.LENGTH_SHORT).show()
            } else if (state == RegistrationState.Progress){
                Log.e(TAG, "Registration status: $message")
                binding.status.text = "Connecting..."
                binding.status.setTextColor(Color.MAGENTA)
            }
        }

        override fun onAudioDeviceChanged(core: Core, audioDevice: AudioDevice) {
            super.onAudioDeviceChanged(core, audioDevice)
        }

        override fun onAudioDevicesListUpdated(core: Core) {
            super.onAudioDevicesListUpdated(core)
        }

        override fun onCallStateChanged(
            core: Core,
            call: Call,
            state: Call.State?,
            message: String
        ) {
            when (state) {
                Call.State.IncomingReceived -> {

                    if (call.dir == Call.Dir.Incoming){
                        mediaPlayer?.isLooping = true
                        mediaPlayer?.start()
                        val args = Bundle()
                        args.putString(Constants.REMOTE_FROM, call.callLog.fromAddress.username)
                        callFragment.arguments = args
                        callFragment.show(supportFragmentManager, "callFragment")

                        Log.e(TAG, "onCallStateChanged: IncomingCall call incoming call")
                    }
                }

                Call.State.Connected -> {

                    Log.e(TAG, "connected!", )
                    callModel.callState.postValue(state)
                    mediaPlayer?.stop()
                }

                Call.State.Released -> {

                    if (call.dir == Call.Dir.Incoming) {
                        Log.e(TAG, "Incoming call release", )
                        mediaPlayer?.stop()
                        callFragment.dismiss()
                    }else{
                        callFragment.dismiss()
                    }
                }

                Call.State.Idle -> TODO()
                Call.State.PushIncomingReceived -> TODO()
                Call.State.OutgoingInit -> {
                    val args = Bundle()
                    args.putString(Constants.REMOTE_FROM, call.callLog.toAddress.username)
                    args.putString(Constants.CALL_DIRECTION, "outbound")
                    callFragment.arguments = args
                    callFragment.show(supportFragmentManager, "callFragment")
                }
                Call.State.OutgoingProgress -> {
                    Log.e(TAG, "onCallStateChanged: Outgoing progress")
                }
                Call.State.OutgoingRinging -> {
                    Log.e(TAG, "onCallStateChanged: Outgoing Ringing")
                }
                Call.State.OutgoingEarlyMedia -> TODO()
                Call.State.StreamsRunning -> {

                    Log.e(TAG, "onCallStateChanged: $state", )
                }
                Call.State.Pausing -> TODO()
                Call.State.Paused -> TODO()
                Call.State.Resuming -> TODO()
                Call.State.Referred -> TODO()
                Call.State.Error -> {
                    Log.e(TAG, "onCallStateChanged: Error")
                    showToast("Call ${message}")
                    Log.e(TAG, "onCallStateChanged: ${message}", )
                }
                Call.State.End ->{
                    callFragment.dismiss()
                }
                Call.State.PausedByRemote -> TODO()
                Call.State.UpdatedByRemote -> TODO()
                Call.State.IncomingEarlyMedia -> TODO()
                Call.State.Updating -> {
                    Log.e(TAG, "onCallStateChanged: Updating")
                }

                Call.State.EarlyUpdatedByRemote -> TODO()
                Call.State.EarlyUpdating -> TODO()
                null -> TODO()
            }
        }
    }


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        binding = ActivityHomeBinding.inflate(layoutInflater)
        val view = binding.root

        setContentView(view)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }


        callModel = ViewModelProvider(this).get(CallViewModel::class.java)
        networkModel = ViewModelProvider(this).get(NetworkModel::class.java)

        fragmentManager = supportFragmentManager

        mediaPlayer = MediaPlayer.create(this, R.raw.ringtone)

        val factory = Factory.instance()

        factory.setDebugMode(true, "Hello Linphone")

        core = factory.createCore(null, null, this)

        callModel.initCore(core)

        callFragment = CallFragment()

        lifecycleScope.launch {

            val username: Flow<String> = applicationContext.dataStore.data.map {
                it[userNamePref] ?: ""
            }
            val password: Flow<String> = applicationContext.dataStore.data.map {
                it[passwordRef] ?: ""
            }
            val domain: Flow<String> = applicationContext.dataStore.data.map {
                it[domainRef] ?: ""
            }

            binding.username.text = username.first().replaceFirstChar { it.uppercase() }

            performLogin(username.first(), password.first(), domain.first())

        }

        // TopApp Bar menu item listener
        binding.topAppBar.setOnMenuItemClickListener { menuItem ->
            when (menuItem.itemId) {
                R.id.menu_log_out -> {
                    unregister()
                    lifecycleScope.launch {
                        dataStore.edit {
                            it.remove(userNamePref)
                            it.remove(passwordRef)
                            it.remove(domainRef)
                            Intent(this@HomeActivity, MainActivity::class.java).also {
                                Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                                startActivity(it)
                            }
                        }
                    }
                    true
                }

                else -> {
                    true
                }
            }
        }

        // Users search listener
        binding.userSearchView.setOnQueryTextListener(object : SearchView.OnQueryTextListener,
            androidx.appcompat.widget.SearchView.OnQueryTextListener {
            override fun onQueryTextSubmit(query: String?): Boolean {
                Log.e(TAG, "onQueryTextSubmit: ${query}", )

                binding.progressCircular.visibility = View.VISIBLE

                disposable = networkModel.getUsers().subscribeOn(Schedulers.io()).observeOn(
                    AndroidSchedulers.mainThread()).subscribe { apiResponse ->

                    if (apiResponse.isSuccessful){

                        binding.progressCircular.visibility = View.GONE

                        val users = apiResponse.body()
                        val filteredUser = users?.filter {userResponse ->
                            userResponse.username.contains(query.toString())
                        }

                        adapter = GroupieAdapter().apply {
                            filteredUser?.forEach {
                                Log.e(TAG, "onCreate-001: $it", )
                                add(ListAdapters(it))
                            }
                        }

                       adapter.setOnItemClickListener { item, view ->
                           val group = item as ListAdapters
                           val user = group.content

                           performOutboundCall(user)


                       }

                        val decoration = DividerItemDecoration(this@HomeActivity, DividerItemDecoration.VERTICAL)
                        binding.userList.layoutManager = LinearLayoutManager(this@HomeActivity)
                        binding.userList.addItemDecoration(decoration)
                        binding.userList.adapter = adapter
                    }else{
                        val gson = Gson()
                        val error = gson.fromJson<ApiError>(apiResponse.errorBody()?.string(), ApiError::class.java)
                        showToast(error.message)
                    }
                }

                return true
            }

            override fun onQueryTextChange(newText: String?): Boolean {
                return false
            }
        })



    }

    // function to unregister SIP account connection
    private fun unregister() {
        val account = core.defaultAccount
        account ?: return

        val params = account.params

        val clonedParams = params.clone()

        clonedParams.registerEnabled = false

        account.params = clonedParams
    }

    private fun performOutboundCall(user: UserResponse) {
        val remoteAddress = "sip:${user.username}@${user.domain};transport=udp"
        remoteAddress ?: return

        val callParams = core.createCallParams(null)

        callParams ?: return

        callParams.mediaEncryption = MediaEncryption.SRTP
        core.inviteWithParams(remoteAddress, callParams)
    }


    fun performLogin(
        username: String,
        password: String,
        domain: String
    ) {

        Log.e(TAG, "performLogin: I am here")
        val transportType = TransportType.Udp

        val authInfo =
            Factory.instance().createAuthInfo(username, null, password, null, null, domain, null)

        val accountParams = core.createAccountParams()

        val identity = Factory.instance().createAddress("sip:$username@$domain")

        accountParams.identityAddress = identity

        val address = Factory.instance().createAddress("sip:$domain")

        address?.transport = transportType

        accountParams.serverAddress = address

        accountParams.registerEnabled = true

        val account = core.createAccount(accountParams)

        core.addAuthInfo(authInfo)

        core.addAccount(account)

        core.defaultAccount = account

        core.addListener(coreListener)

        account.addListener { _, state, message ->
            Log.i(TAG, "[Account] Registration state changed: $state, $message")
        }

        core.start()

    }

}