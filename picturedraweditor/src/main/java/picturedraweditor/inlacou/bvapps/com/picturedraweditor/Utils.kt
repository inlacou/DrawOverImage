package picturedraweditor.inlacou.bvapps.com.picturedraweditor

import android.content.res.Resources
import android.util.Log
import android.view.View

/**
 * Created by inlacou on 20/12/17.
 */
object Utils {
	fun resizeView(view: View, width: Int, height: Int) {
		val lp = view.layoutParams
		if (width > -1) {
			lp.width = width
		}
		if (height > -1) {
			lp.height = height
		}
		view.layoutParams = lp
	}
}

//Extension functions
val Int.toPx: Int
	get() = (this * Resources.getSystem().displayMetrics.density).toInt()

val Int.toDp: Int
	get() = (this / Resources.getSystem().displayMetrics.density).toInt()