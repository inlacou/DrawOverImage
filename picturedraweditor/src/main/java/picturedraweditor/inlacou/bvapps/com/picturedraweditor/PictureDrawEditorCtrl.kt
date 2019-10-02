package picturedraweditor.inlacou.bvapps.com.picturedraweditor

import android.view.View

/**
 * Created by inlacou on 19/12/17.
 */
class PictureDrawEditorCtrl(val view: PictureDrawEditorAct, val model: PictureDrawEditorMdl) {

	fun onEraserDisplayClick() {
		view.switchEraserSeekBarVisibility()
	}
	
	fun onBrushDisplayClick() {
		view.switchBrushSeekBarsVisibility()
	}
	
	fun onCanvasTouchStarted() {
		view.hideEraserSeekBar()
		view.hideBrushSeekBar()
	}
	
	fun onSingleClick() {
		if(model.isUiHideable){
			if(view.vUI.visibility == View.VISIBLE){
				view.vUI.visibility = View.GONE
			}else{
				view.vUI.visibility = View.VISIBLE
			}
		}else{
			view.vUI.visibility = View.VISIBLE
		}
	}
}