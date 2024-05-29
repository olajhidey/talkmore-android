package com.example.talkmoreapp.network

import com.example.talkmoreapp.model.User
import com.example.talkmoreapp.model.UserResponse
import hu.akarnokd.rxjava3.retrofit.RxJava3CallAdapterFactory
import io.reactivex.rxjava3.core.Observable
import retrofit2.Call
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Path

interface ApiService{
    companion object{
        private const val BASE_URL = "https://8dkkg3tr46.execute-api.us-east-1.amazonaws.com/"
        val retrofit:Retrofit
            get(){
                return Retrofit.Builder()
                    .baseUrl(BASE_URL)
                    .addCallAdapterFactory(RxJava3CallAdapterFactory.create())
                    .addConverterFactory(GsonConverterFactory.create())
                    .build()
            }
    }

    @POST("api/create")
    fun createUser(@Body user: User): Observable<Response<UserResponse>>

    @GET("api/get/{username}")
    fun getUser(@Path("username") username: String): Observable<Response<List<UserResponse>>>

    @GET("api/users")
    fun getUsers(): Observable<Response<List<UserResponse>>>
}


