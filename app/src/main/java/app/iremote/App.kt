package app.iremote

import android.app.Application

// Simple app context holder for services (TileService)
object App {
    lateinit var ctx: Application
}

class MainApp : Application() {
    override fun onCreate() {
        super.onCreate()
        AppGraph.init(applicationContext)
        App.ctx = this
    }
}