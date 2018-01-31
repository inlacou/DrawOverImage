package picturedraweditor.inlacou.bvapps.com.picturedraweditor

import android.app.Activity
import android.app.Dialog
import android.app.ProgressDialog
import android.content.DialogInterface
import android.content.Intent
import android.graphics.Bitmap
import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import android.widget.ImageView
import com.github.salomonbrys.kotson.fromJson
import com.google.gson.Gson
import android.graphics.drawable.BitmapDrawable
import android.graphics.BitmapFactory
import android.graphics.Point
import android.net.Uri
import android.provider.MediaStore
import android.view.View
import android.view.ViewTreeObserver
import android.widget.SeekBar
import colorpickerlayout.inlacou.bvapps.com.colorpicklayout.ColorListener
import colorpickerlayout.inlacou.bvapps.com.colorpicklayout.ColorPickLayout
import colorpickerlayout.inlacou.bvapps.com.colorpicklayout.ColorWrapper
import java.io.File
import java.io.FileInputStream
import java.io.FileNotFoundException
import java.io.IOException

/**
 * Created by inlacou on 19/12/17.
 */
class PictureDrawEditorAct : AppCompatActivity() {

	lateinit private var model: PictureDrawEditorMdl
	lateinit private var controller: PictureDrawEditorCtrl

	lateinit private var imageView: ImageView
	lateinit private var colorPickLayout: ColorPickLayout
	lateinit private var vUI: View
	lateinit private var btnColor: View
	lateinit private var btnUndo: View
	lateinit private var btnRedo: View
	lateinit private var btnErase: View
	lateinit private var btnAccept: View
	lateinit private var btnCancel: View
	lateinit private var colorBrushSeekbar: SeekBar
	lateinit private var eraserSeekbar: SeekBar
	lateinit private var colorDisplay: CircleView
	lateinit private var eraserDisplay: CircleView
	lateinit private var brushPickerIcon: ImageView
	lateinit private var eraserIcon: ImageView
	lateinit private var canvas: CanvasView

	companion object {

		private val DEBUG_TAG = PictureDrawEditorAct::class.java.simpleName
		val RESULT_FILE_ABSOLUTE_PATH = "result_file_absolute_path"

		fun navigateForResult(activity: AppCompatActivity, requestCode: Int, mdl: PictureDrawEditorMdl) {
			val intent = Intent(activity, PictureDrawEditorAct::class.java)

			intent.putExtra("model", Gson().toJson(mdl))

			activity.startActivityForResult(intent, requestCode)
		}
	}

	override fun onCreate(savedInstanceState: Bundle?) {
		super.onCreate(savedInstanceState)

		this.requestedOrientation = android.content.pm.ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE

		setContentView(R.layout.activity_picture_draw_editor_2)

		getIntentData()

		initialize(savedInstanceState)

		populate()

		setListeners()
	}

	private fun getIntentData() {
		if (intent.hasExtra("model")) model = Gson().fromJson(intent.extras.getString("model"))
	}

	private fun initialize(savedInstanceState: Bundle?) {
		imageView = findViewById(R.id.image)
		colorPickLayout = findViewById(R.id.colorPickLayout)
		vUI = findViewById(R.id.ui)
		canvas = findViewById(R.id.canvas)
		colorDisplay = findViewById(R.id.color_display)
		eraserDisplay = findViewById(R.id.eraser_display)
		brushPickerIcon = findViewById(R.id.brush_color_icon)
		eraserIcon = findViewById(R.id.brush_color_icon)
		colorBrushSeekbar = findViewById(R.id.seekbar_color_brush)
		eraserSeekbar = findViewById(R.id.seekbar_eraser)
		btnColor = findViewById(R.id.color)
		btnErase = findViewById(R.id.erase)
		btnUndo = findViewById(R.id.undo)
		btnRedo = findViewById(R.id.redo)
		btnAccept = findViewById(R.id.btnAccept)
		btnCancel = findViewById(R.id.btnCancel)
		controller = PictureDrawEditorCtrl(view = this, model = model)
		canvas.setModel(model)
	}

	private fun populate() {
		window.decorView.systemUiVisibility = (
				//View.SYSTEM_UI_FLAG_LAYOUT_STABLE or
				//or View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION or
				//or View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN or
				View.SYSTEM_UI_FLAG_HIDE_NAVIGATION or // hide nav bar
				View.SYSTEM_UI_FLAG_FULLSCREEN or // hide status bar
				View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY)

		colorDisplay.fillColor = model.color
		eraserDisplay.visibility = View.GONE
		colorDisplay.visibility = View.VISIBLE
		eraserSeekbar.visibility = View.GONE
		colorBrushSeekbar.visibility = View.GONE
		colorBrushSeekbar.progress = 5
		colorDisplay.circleRadius = 15.toPx*(5+10)/40
		eraserSeekbar.progress = 5
		eraserDisplay.circleRadius = 15.toPx*(5+10)/40

		imageView.viewTreeObserver.addOnGlobalLayoutListener(object: ViewTreeObserver.OnGlobalLayoutListener{
			override fun onGlobalLayout() {
				val size = Point()
				windowManager.defaultDisplay.getRealSize(size)

				imageView.viewTreeObserver.removeOnGlobalLayoutListener(this)
			}
		})

		try {
			val inputStream = FileInputStream(model.filePath)
			model.layer0 = BitmapFactory.decodeStream(inputStream)
			//selectedImage = ImageUtils.scaleBitmapKeepAspectRatio(bitmap, size)
			inputStream.close()
		} catch (e: FileNotFoundException) {
			model.layer0 = MediaStore.Images.Media.getBitmap(contentResolver, Uri.parse(model.filePath))
		} catch (e: IOException) {
			e.printStackTrace()
		}

		model.layer0?.let {
			val display = windowManager.defaultDisplay;
			val size = Point()
			display.getSize(size)
			val width = size.x
			val height = size.y
			model.layer0 = CanvasView.convertToMutable(model.layer0!!)
			model.layer0 = Bitmap.createScaledBitmap(model.layer0, width, height, false)
			imageView.adjustViewBounds = true
			imageView.scaleType = ImageView.ScaleType.CENTER_INSIDE
			imageView.setImageDrawable(BitmapDrawable(resources, model.layer0))
		}
	}

	private fun setListeners() {
		colorPickLayout.singleClickThresholdLimit = 80
		colorPickLayout.singleClickListener = object: ColorPickLayout.SingleClickListener{
			override fun onSingleClick() {
				if(vUI.visibility == View.VISIBLE){
					vUI.visibility = View.GONE
				}else{
					vUI.visibility = View.VISIBLE
				}
			}
		}
		canvas.singleClickThresholdLimit = 80
		canvas.singleClickListener = object: CanvasView.SingleClickListener{
			override fun onSingleClick() {
				if(vUI.visibility == View.VISIBLE){
					vUI.visibility = View.GONE
				}else{
					vUI.visibility = View.VISIBLE
				}
			}
		}
		btnColor.setOnClickListener {
			if(model.mode==Mode.draw){
				model.mode = Mode.pick
				brushPickerIcon.setImageDrawable(resources.getDrawable(R.drawable.color_picker))
			}else{
				model.mode = Mode.draw
				brushPickerIcon.setImageDrawable(resources.getDrawable(R.drawable.brush))
			}
			eraserDisplay.visibility = View.GONE
			eraserSeekbar.visibility = View.GONE
			colorDisplay.visibility = View.VISIBLE
			canvas.update()
		}
		btnErase.setOnClickListener {
			if(model.mode==Mode.erase){
				model.mode = Mode.draw
				eraserDisplay.visibility = View.GONE
				eraserSeekbar.visibility = View.GONE
				colorDisplay.visibility = View.VISIBLE
			}else{
				model.mode = Mode.erase
				eraserDisplay.visibility = View.VISIBLE
				colorDisplay.visibility = View.GONE
				colorBrushSeekbar.visibility = View.GONE
			}
			canvas.update()
		}
		eraserDisplay.setOnClickListener {
			//show size picker
			if(eraserSeekbar.visibility == View.VISIBLE) {
				eraserSeekbar.visibility = View.GONE
			}else{
				eraserSeekbar.visibility = View.VISIBLE
			}
		}
		colorDisplay.setOnClickListener {
			//show size picker
			if(colorBrushSeekbar.visibility == View.VISIBLE) {
				colorBrushSeekbar.visibility = View.GONE
			}else{
				colorBrushSeekbar.visibility = View.VISIBLE
			}
		}
		eraserSeekbar.setOnSeekBarChangeListener(object: SeekBar.OnSeekBarChangeListener{
			override fun onProgressChanged(p0: SeekBar?, p1: Int, p2: Boolean) {
				//min: 3.toPx
				//max: 15.toPx
				//Seekbar goes from 10 to 40
				eraserDisplay.circleRadius = 15.toPx*(p1+10)/40
				model.eraserWidth = p1+10
				canvas.update()
			}

			override fun onStartTrackingTouch(p0: SeekBar?) {
			}

			override fun onStopTrackingTouch(p0: SeekBar?) {
			}
		})
		colorBrushSeekbar.setOnSeekBarChangeListener(object: SeekBar.OnSeekBarChangeListener{
			override fun onProgressChanged(p0: SeekBar?, p1: Int, p2: Boolean) {
				//min: 3.toPx
				//max: 15.toPx
				//Seekbar goes from 10 to 40
				colorDisplay.circleRadius = 15.toPx*(p1+10)/40
				model.colorWidth = p1+10
				canvas.update()
			}

			override fun onStartTrackingTouch(p0: SeekBar?) {
			}

			override fun onStopTrackingTouch(p0: SeekBar?) {
			}
		})
		btnUndo.setOnClickListener {
			canvas.undo()
		}
		btnRedo.setOnClickListener {
			canvas.redo()
		}
		btnAccept.setOnClickListener{
			val progressDialog = ProgressDialog(this)
			progressDialog.setMessage(getString(R.string.Saving))
			progressDialog.isIndeterminate = true
			progressDialog.setOnShowListener {
				canvas.saveImage(object: CanvasView.FileSavedListener{
					override fun onFileSaved(file: File) {
						progressDialog.dismiss()
						val data = Intent()
						data.putExtra(RESULT_FILE_ABSOLUTE_PATH, file.absolutePath)
						setResult(Activity.RESULT_OK, data)
						finish()
					}
				})
			}
			progressDialog.show()
		}
		btnCancel.setOnClickListener{
			onBackPressed()
		}
		colorPickLayout.setSelector(object: ColorListener{
			override fun onColorSelected(envelope: ColorWrapper) {
				if(model.mode==Mode.pick) {
					model.color = envelope.color
					colorDisplay.fillColor = model.color
				}
			}
		})
	}

	override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
		super.onActivityResult(requestCode, resultCode, data)
		controller.onActivityResult(requestCode, resultCode, data)
	}

	override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
		super.onRequestPermissionsResult(requestCode, permissions, grantResults)
		controller.onRequestPermissionsResult(requestCode, permissions, grantResults)
	}

	override fun onDestroy() {
		controller.onDestroy()
		canvas.onDestroy()
		super.onDestroy()
	}

}
