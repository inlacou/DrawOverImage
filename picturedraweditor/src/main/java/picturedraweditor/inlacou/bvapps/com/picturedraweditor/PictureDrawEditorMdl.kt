package picturedraweditor.inlacou.bvapps.com.picturedraweditor

import android.graphics.Bitmap
import android.graphics.Color

/**
 * Created by inlacou on 19/12/17.
 */
data class PictureDrawEditorMdl(val filePath: String) {
	var layer0: Bitmap? = null
	var layer1: Bitmap? = null
	var layer2: Bitmap? = null
	var color: Int = Color.BLACK
	var mode: Mode = Mode.draw
}
enum class Mode {
	pick, draw, erase
}