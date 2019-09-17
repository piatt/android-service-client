package com.piatt.androidserviceclient.clients

import android.app.Activity
import android.app.Application
import android.content.Context
import com.piatt.androidserviceclient.common.WeatherClientModule
import dagger.Component
import dagger.Module
import dagger.Provides
import dagger.android.*
import javax.inject.Inject
import javax.inject.Singleton

class ClientApp : Application(), HasActivityInjector {
    @Inject
    internal lateinit var dispatchingActivityInjector: DispatchingAndroidInjector<Activity>

    override fun activityInjector(): AndroidInjector<Activity> = dispatchingActivityInjector

    override fun onCreate() {
        super.onCreate()
        DaggerAppComponent.builder()
            .appModule(AppModule(this))
            .build().inject(this)
    }
}

@Singleton
@Component(modules = [
    AndroidInjectionModule::class,
    ProvidedInjectorsModule::class,
    AppModule::class,
    WeatherClientModule::class
])
interface AppComponent {
    fun inject(app: ClientApp)
}

@Module
class AppModule(private val app: Application) {
    @Provides
    @Singleton
    fun provideContext(): Context = app
}

@Module
abstract class ProvidedInjectorsModule {
    @ContributesAndroidInjector
    internal abstract fun contributeActivityInjector(): ClientActivity
}