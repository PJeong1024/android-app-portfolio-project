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
import com.jdw.skillstestapp.repository.MyAppRepository
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
    fun provideContentResolver(@ApplicationContext context: Context): ContentResolver {
        return context.contentResolver
    }

    @Provides
    @Singleton
    fun provideMyAppRepository(
        contentResolver: ContentResolver,   // Inject ContentResolver here
        userImgDao: UserImgDao,
        chatMessageDao: ChatMessageDao,
        weatherApi: WeatherApi
    ): MyAppRepository {
        return MyAppRepository(contentResolver, userImgDao, chatMessageDao, weatherApi)
    }

    @Provides
    @Singleton
    fun provideImgInfoDatabase(@ApplicationContext appContext: Context): AppDatabase {
        return Room.databaseBuilder(
            appContext,
            AppDatabase::class.java,
            "app_database"
        ).build()
    }

    @Provides
    @Singleton
    fun provideUserImgDao(appDatabase: AppDatabase): UserImgDao {
        return appDatabase.userImgDao()
    }

    @Provides
    @Singleton
    fun provideChatDao(appDatabase: AppDatabase): ChatMessageDao {
        return appDatabase.chatMessageDao()
    }

    @Provides
    @Singleton
    fun provideGenerativeModel(): GenerativeModel {
        return GenerativeModel(modelName = "gemini-1.5-flash", apiKey = BuildConfig.GEMINI_API_KEY)
    }

    @Provides
    @Singleton
    fun getFirebaseAuthInstance(): FirebaseAuth {
        return FirebaseAuth.getInstance()
    }

    @Provides
    @Singleton
    fun getFirebaseFireStoreInstance(): FirebaseFirestore {
        return FirebaseFirestore.getInstance()
    }

    @Provides
    @Singleton
    fun provideOpenWeatherApi(): WeatherApi {
        return Retrofit.Builder().baseUrl(Constants.BASE_URL)
            .addConverterFactory(GsonConverterFactory.create()).build()
            .create(WeatherApi::class.java)
    }

//    @Singleton
//    @Provides
//    fun provideFirebaseBookRepository(): FirebaseBookRepository {
//        return FirebaseBookRepository(
//            queryBook = FirebaseFirestore.getInstance().collection("books")
//        )
//    }
//
//    @Singleton
//    @Provides
//    fun provideBookRepository(api: BooksApi): BookRepository {
//        return BookRepository(api)
//    }
//
//    @Singleton
//    @Provides
//    fun provideBookApi(): BooksApi {
//        return Retrofit.Builder().baseUrl(Constants.BASE_URL)
//            .addConverterFactory(GsonConverterFactory.create())
//            .build().create(BooksApi::class.java)
//    }

}