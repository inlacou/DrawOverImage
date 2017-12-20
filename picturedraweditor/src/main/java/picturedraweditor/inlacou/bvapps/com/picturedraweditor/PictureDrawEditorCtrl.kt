package picturedraweditor.inlacou.bvapps.com.picturedraweditor

import android.content.Intent

/**
 * Created by inlacou on 19/12/17.
 */
class PictureDrawEditorCtrl(val view: PictureDrawEditorAct, val model: PictureDrawEditorMdl) {

	companion object {
		private val DEBUG_TAG = PictureDrawEditorCtrl::class.java.simpleName
	}

	init { //initialize

	}

	fun onDestroy() {
	}

	fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
	}

	fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
	}
}