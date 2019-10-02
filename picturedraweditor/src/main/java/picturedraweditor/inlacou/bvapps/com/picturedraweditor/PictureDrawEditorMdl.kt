package picturedraweditor.inlacou.bvapps.com.picturedraweditor

import android.graphics.Bitmap
import android.graphics.Color

/**
 * Created by inlacou on 19/12/17.
 */
data class PictureDrawEditorMdl(
		val filePath: String,
		val isUiHideable: Boolean = true,
		val showForwardButton: Boolean = true) {
	var layer0: Bitmap? = null
	var color: Int = Color.BLACK
	var alpha: Int = 180 //alpha from 0 to 255, 180 equals 0.7
	var mode: Mode = Mode.draw
	var eraserWidth: Int = 15
	var colorWidth: Int = 15
}

enum class Mode {
	pick, draw, erase
}