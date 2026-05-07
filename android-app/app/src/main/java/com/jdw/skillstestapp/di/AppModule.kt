package com.jdw.skillstestapp.di

import android.content.ContentResolver
import android.content.Context
import androidx.room.Room
import com.google.ai.client.generativeai.GenerativeModel
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.jdw.skillstestapp.BuildConfig
import com.jdw.skillstestapp.data.AppDatabase
import com.jdw.skillstestapp.data.ChatMessageDao
import com.jdw.skillstestapp.data.UserImgDao
import com.jdw.skillstestapp.data.network.WeatherApi
import com.jdw.skillstestapp.utils.Constants
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import javax.inject.Singleton


@Module
@InstallIn(SingletonComponent::class)
object AppModule {
    @Provides
    @Singleton
    fun provideContentResolver(@ApplicationContext context: Context): ContentResolver =
        context.contentResolver

    @Provides
    @Singleton
    fun provideImgInfoDatabase(@ApplicationContext appContext: Context): AppDatabase =
        Room.databaseBuilder(appContext, AppDatabase::class.java, "app_database").build()

    @Provides
    @Singleton
    fun provideUserImgDao(appDatabase: AppDatabase): UserImgDao = appDatabase.userImgDao()

    @Provides
    @Singleton
    fun provideChatDao(appDatabase: AppDatabase): ChatMessageDao = appDatabase.chatMessageDao()

    @Provides
    @Singleton
    fun provideGenerativeModel(): GenerativeModel =
        GenerativeModel(modelName = "gemini-1.5-flash", apiKey = BuildConfig.GEMINI_API_KEY)

    @Provides
    @Singleton
    fun getFirebaseAuthInstance(): FirebaseAuth = FirebaseAuth.getInstance()

    @Provides
    @Singleton
    fun getFirebaseFireStoreInstance(): FirebaseFirestore = FirebaseFirestore.getInstance()

    @Provides
    @Singleton
    fun provideOpenWeatherApi(): WeatherApi =
        Retrofit.Builder()
            .baseUrl(Constants.BASE_URL)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(WeatherApi::class.java)
}
