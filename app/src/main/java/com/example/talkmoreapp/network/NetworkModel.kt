package com.example.talkmoreapp.network

import androidx.lifecycle.ViewModel
import com.example.talkmoreapp.model.User
import com.example.talkmoreapp.model.UserResponse
import io.reactivex.rxjava3.core.Observable
import retrofit2.Response

class NetworkModel: ViewModel() {

   val myApiService by lazy {
       ApiService.retrofit.create(ApiService::class.java)
   }

    suspend fun createUser(user: User): Observable<Response<UserResponse>> {
        return myApiService.createUser(user)
    }

    fun getUser(username:String): Observable<Response<List<UserResponse>>> {
        return myApiService.getUser(username)
    }

    fun getUsers(): Observable<Response<List<UserResponse>>> {
        return myApiService.getUsers()
    }
}