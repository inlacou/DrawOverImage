package picturedraweditor.inlacou.bvapps.com.picturedraweditor

import android.graphics.Bitmap
import android.graphics.Color

/**
 * Created by inlacou on 19/12/17.
 */
data class PictureDrawEditorMdl(val filePath: String) {
	var layer0: Bitmap? = null
	var color: Int = Color.BLACK
	var mode: Mode = Mode.draw
	var eraserWidth: Int = 15
	var colorWidth: Int = 15
}
enum class Mode {
	pick, draw, erase
}