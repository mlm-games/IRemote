package app.iremote.ir.tile

import android.annotation.SuppressLint
import android.graphics.drawable.Icon
import android.service.quicksettings.Tile
import android.service.quicksettings.TileService
import app.iremote.AppGraph
import app.iremote.R
import app.iremote.ir.data.LastUsedStore
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class RemoteTileService : TileService() {
    private val scope = CoroutineScope(Dispatchers.IO)

    override fun onStartListening() {
        super.onStartListening()
        val t = qsTile ?: return
        val last = LastUsedStore.lastKeyId(applicationContext)
        t.label = if (last > 0) "Send last key" else "IRemote"
        t.state = if (last > 0) Tile.STATE_ACTIVE else Tile.STATE_INACTIVE
        t.icon = Icon.createWithResource(this, R.mipmap.ic_launcher)
        t.updateTile()
    }

    @SuppressLint("StartActivityAndCollapseDeprecated")
    override fun onClick() {
        super.onClick()
        val id = LastUsedStore.lastKeyId(applicationContext)
        if (id <= 0) {
            //no last key, open app?
//            startActivityAndCollapse(
//                packageManager.getLaunchIntentForPackage(packageName)
//            )
            return
        }
        scope.launch {
            runCatching {
                AppGraph.init(applicationContext)
                AppGraph.irRepo.sendByKeyId(id)
            }
        }
    }
}