package com.example.talkmoreapp

import android.Manifest
import android.content.Intent
import android.os.Bundle
import android.util.Log
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import com.example.talkmoreapp.activity.HomeActivity
import com.example.talkmoreapp.databinding.ActivityMainBinding
import com.example.talkmoreapp.model.ApiError
import com.example.talkmoreapp.model.User
import com.example.talkmoreapp.network.NetworkModel
import com.example.talkmoreapp.utils.Constants
import com.example.talkmoreapp.utils.dataStore
import com.example.talkmoreapp.utils.showToast
import com.google.gson.Gson
import com.gun0912.tedpermission.PermissionListener
import com.gun0912.tedpermission.normal.TedPermission
import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers
import io.reactivex.rxjava3.disposables.Disposable
import io.reactivex.rxjava3.schedulers.Schedulers
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch

private const val TAG = "MainActivity"
class MainActivity : AppCompatActivity() {

    var disposable: Disposable? = null

    private val coroutineScope = CoroutineScope(Dispatchers.IO)
    private lateinit var viewModel: NetworkModel
    lateinit var loadDialog: AlertDialog

    private lateinit var binding: ActivityMainBinding

    //Preferences Key
    val userNamePref = stringPreferencesKey(Constants.USERNAME_MAP_KEY)
    val passwordRef = stringPreferencesKey(Constants.PASSWORD_MAP_KEY)
    val domainRef = stringPreferencesKey(Constants.DOMAIN_MAP_KEY)


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        binding = ActivityMainBinding.inflate(layoutInflater)

        val view = binding.root
        setContentView(view)

        checkUserPermissions()

        val userInfo:Flow<String> = applicationContext.dataStore.data.map {
            it[userNamePref] ?: ""
        }

        lifecycleScope.launch {
            val username = userInfo.first()
            Log.e(TAG, "onCreate: ${username}")

            if (username.isNotEmpty()){
                Log.e(TAG, "onCreate: i am here", )
                Intent(this@MainActivity, HomeActivity::class.java).also {
                    Intent.FLAG_ACTIVITY_CLEAR_TASK or Intent.FLAG_ACTIVITY_NEW_TASK
                    startActivity(it)
                    finish()
                }
            }
        }

        viewModel = ViewModelProvider(this).get(NetworkModel::class.java)
        loadDialog = AlertDialog.Builder(this).
            setView(layoutInflater.inflate(R.layout.dialog_layout, null, true))
            .setCancelable(false).create()

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        // Login button listener
        binding.loginBtn.setOnClickListener {

            // Get username from Edittext
            val username = binding.usernameEdittext.text.toString()

            if (username.isEmpty() || username.equals("")){
                showToast("Please enter username")
            }else{
                loadDialog.show()
                disposable = viewModel.getUser(username).subscribeOn(Schedulers.io()).observeOn(AndroidSchedulers.mainThread()).subscribe {apiResponse ->

                    if (apiResponse.isSuccessful){
                        loadDialog.dismiss()
                        val response = apiResponse.body()
                        lifecycleScope.launch {
                            applicationContext.dataStore.edit { data ->
                                data[userNamePref] = username ?: ""
                                data[passwordRef] = response?.get(0)?.password ?: ""
                                data[domainRef] = response?.get(0)?.domain.toString() ?: ""
                            }
                        }
                        Log.e(TAG, "API response: ${response?.get(0)?.username}", )
                        Intent(this@MainActivity, HomeActivity::class.java).also {
                            Intent.FLAG_ACTIVITY_CLEAR_TASK or Intent.FLAG_ACTIVITY_NEW_TASK
                            startActivity(it)
                        }
                    }else{
                        loadDialog.dismiss()
                        val gson = Gson()
                        val error = gson.fromJson<ApiError>(apiResponse.errorBody()?.string(), ApiError::class.java)
                        showToast(error.message)
                    }
                }
            }

        }

        binding.registerBtn.setOnClickListener {
            // Get username from Edittext
            val username = binding.usernameEdittext.text.toString()

            performRegistration(username)
        }
    }

    var permissionListener: PermissionListener = object : PermissionListener {
        override fun onPermissionGranted() {
        }

        override fun onPermissionDenied(p0: MutableList<String>?) {
        }
    }

    private fun checkUserPermissions(){

        TedPermission.create()
            .setPermissionListener(permissionListener)
            .setDeniedMessage("If you reject permission,you can not use this service\\n\\nPlease turn on permissions at [Setting] > [Permission]")
            .setPermissions(
                Manifest.permission.RECORD_AUDIO, Manifest.permission.READ_PHONE_STATE, Manifest.permission.CAMERA
            )
            .check()
    }

    private fun performRegistration(username: String) {
        if (username.isEmpty() || username.equals("")){
            showToast("Please enter username")
        }else{
            loadDialog.show()

            coroutineScope.launch {
                val password = username+"123@"
                val user = User(username, password)
                disposable = viewModel.createUser(user).subscribeOn(Schedulers.io()).observeOn(AndroidSchedulers.mainThread()).subscribe { apiResponse ->
                    if (apiResponse.isSuccessful) {
                        loadDialog.dismiss()
                        Log.e(TAG, "API response: ${apiResponse.body()}")
                        val response = apiResponse.body()

                        lifecycleScope.launch {
                            applicationContext.dataStore.edit { data ->
                                data[userNamePref] = username ?: ""
                                data[passwordRef] = response?.password ?: ""
                                data[domainRef] = response?.domain.toString() ?: ""
                            }
                        }
                        Intent(this@MainActivity, HomeActivity::class.java).also {
                            Intent.FLAG_ACTIVITY_CLEAR_TASK or Intent.FLAG_ACTIVITY_NEW_TASK
                            startActivity(it)
                            finish()
                        }
                    }
                }
            }
        }

    }
}