package com.toolbox.tiles

import android.hardware.camera2.CameraCharacteristics
import android.hardware.camera2.CameraManager
import android.service.quicksettings.Tile
import android.service.quicksettings.TileService

class FlashlightTileService : TileService() {

    private var isOn = false

    override fun onStartListening() {
        super.onStartListening()
        qsTile?.let { tile ->
            tile.state = if (isOn) Tile.STATE_ACTIVE else Tile.STATE_INACTIVE
            tile.updateTile()
        }
    }

    override fun onClick() {
        super.onClick()
        val cameraManager = getSystemService(CAMERA_SERVICE) as CameraManager
        val cameraId = cameraManager.cameraIdList.firstOrNull { id ->
            cameraManager.getCameraCharacteristics(id)
                .get(CameraCharacteristics.FLASH_INFO_AVAILABLE) == true
        } ?: return

        isOn = !isOn
        try {
            cameraManager.setTorchMode(cameraId, isOn)
        } catch (_: Exception) {
            isOn = false
        }

        qsTile?.let { tile ->
            tile.state = if (isOn) Tile.STATE_ACTIVE else Tile.STATE_INACTIVE
            tile.updateTile()
        }
    }

    override fun onTileRemoved() {
        super.onTileRemoved()
        // Turn off flashlight when tile is removed
        if (isOn) {
            try {
                val cameraManager = getSystemService(CAMERA_SERVICE) as CameraManager
                val cameraId = cameraManager.cameraIdList.firstOrNull { id ->
                    cameraManager.getCameraCharacteristics(id)
                        .get(CameraCharacteristics.FLASH_INFO_AVAILABLE) == true
                }
                if (cameraId != null) {
                    cameraManager.setTorchMode(cameraId, false)
                }
            } catch (_: Exception) {}
            isOn = false
        }
    }
}
