package com.ssafy.final_pennant_preset.config

import android.app.Application
import android.os.Environment
import android.util.Log
import com.google.gson.GsonBuilder
import com.ssafy.final_pennant_preset.service.FirebaseTokenService
import com.ssafy.final_pennant_preset.util.SharedPreferencesUtil
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit

private const val TAG = "ApplicationClass"
class ApplicationClass : Application() {

    //url must be end with "/"
    val IP_ADDR = "43.200.40.175:9987" // 프리티어
//    val IP_ADDR = "43.202.250.12:9987"
//    val IP_ADDR = "192.168.0.7:9987"
//    val IP_ADDR = "192.168.33.113:9987"
    val API_URL = "http://$IP_ADDR/"

    companion object {
        // 전역변수 문법을 통해 Retrofit 인스턴스를 앱 실행 시 1번만 생성하여 사용 (싱글톤 객체)
        lateinit var sRetrofit: Retrofit
        lateinit var sSharedPreferences: SharedPreferencesUtil

        const val SHARED_PREFERENCES_NAME = "Music_Ssafy_OFFICE"

        const val X_ACCESS_TOKEN = "X-ACC  ESS-TOKEN"
        const val COOKIES_KEY_NAME = "cookies"


        const val CHANNEL_DANCE = "dance"
        const val CHANNEL_BALLAD = "ballad"
        const val CHANNEL_IDOL = "idol"
        const val CHANNEL_POP = "pop"
        const val CHANNEL_ROCK = "rock"

        var receive_notification_ballad = true
        var receive_notification_dance = true
        var receive_notification_rock = true
        var receive_notification_pop = true
        var receive_notification_idol = true
    }

    override fun onCreate() {
        super.onCreate()

        // 앱이 처음 생성되는 순간, retrofit 인스턴스를 생성
        sSharedPreferences = SharedPreferencesUtil(applicationContext)
        // 레트로핏 인스턴스 생성
        initRetrofitInstance()
        initNotificationChannel()
    }

    // 레트로핏 인스턴스를 생성하고, 레트로핏에 각종 설정값들을 지정해줍니다.
    // 연결 타임아웃시간은 5초로 지정이 되어있고, HttpLoggingInterceptor를 붙여서 어떤 요청이 나가고 들어오는지를 보여줍니다.
    private fun initRetrofitInstance() {
        val client: OkHttpClient = OkHttpClient.Builder()
            .readTimeout(5000, TimeUnit.MILLISECONDS)
            .connectTimeout(5000, TimeUnit.MILLISECONDS)
            // 로그캣에 okhttp.OkHttpClient로 검색하면 http 통신 내용을 보여줍니다.
//            .addInterceptor(HttpLoggingInterceptor().setLevel(HttpLoggingInterceptor.Level.BODY))
            .addNetworkInterceptor(XAccessTokenInterceptor()) // JWT 자동 헤더 전송
            .addInterceptor(AddCookiesInterceptor())  //쿠키 전송
            .addInterceptor(ReceivedCookiesInterceptor()) //쿠키 추출
            .build()

        var gson = GsonBuilder().setLenient().create()
        sRetrofit = Retrofit.Builder()
            .baseUrl(API_URL)
            .client(client)
            .addConverterFactory(GsonConverterFactory.create(gson))
            .build()
    }

    private fun initNotificationChannel() {
        receive_notification_ballad = sSharedPreferences.getNotification(CHANNEL_BALLAD)
        receive_notification_dance = sSharedPreferences.getNotification(CHANNEL_DANCE)
        receive_notification_rock = sSharedPreferences.getNotification(CHANNEL_ROCK)
        receive_notification_pop = sSharedPreferences.getNotification(CHANNEL_POP)
        receive_notification_idol = sSharedPreferences.getNotification(CHANNEL_IDOL)
    }
}