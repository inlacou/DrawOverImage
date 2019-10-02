package picturedraweditor.inlacou.bvapps.com.picturedraweditor

import android.app.Activity
import android.app.ProgressDialog
import android.content.DialogInterface
import android.content.Intent
import android.graphics.*
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import android.widget.ImageView
import com.google.gson.Gson
import android.graphics.drawable.BitmapDrawable
import android.net.Uri
import android.provider.MediaStore
import android.view.View
import android.view.ViewTreeObserver
import android.widget.SeekBar
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import colorpickerlayout.inlacou.bvapps.com.colorpicklayout.ColorListener
import colorpickerlayout.inlacou.bvapps.com.colorpicklayout.ColorPickLayout
import colorpickerlayout.inlacou.bvapps.com.colorpicklayout.ColorWrapper
import timber.log.Timber
import java.io.File
import java.io.FileInputStream
import java.io.FileNotFoundException
import java.io.IOException

/**
 * Created by inlacou on 19/12/17.
 */
class PictureDrawEditorAct : AppCompatActivity() {

	private lateinit var model: PictureDrawEditorMdl
	private lateinit var controller: PictureDrawEditorCtrl

	private lateinit var imageView: ImageView
	private lateinit var colorPickLayout: ColorPickLayout
	lateinit var vUI: View
	private lateinit var btnColor: View
	private lateinit var btnUndo: View
	private lateinit var btnRedo: View
	private lateinit var btnErase: View
	private lateinit var btnAccept: View
	private lateinit var btnCancel: View
	private lateinit var lBrush: View
	private lateinit var sbBrushColor: SeekBar
	private lateinit var sbBrushOpacity: SeekBar
	private lateinit var eraserSeekbar: SeekBar
	private lateinit var colorDisplay: CircleView
	private lateinit var eraserDisplay: CircleView
	private lateinit var brushPickerIcon: ImageView
	private lateinit var eraserIcon: ImageView
	private lateinit var canvas: CanvasView

	companion object {
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

		setContentView(R.layout.activity_picture_draw_editor)

		getIntentData()

		initialize(savedInstanceState)

		populate()

		setListeners()
	}

	private fun getIntentData() {
		if (intent.hasExtra("model")) model = Gson().fromJson(intent!!.extras!!.getString("model")!!, PictureDrawEditorMdl::class.java)
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
		lBrush = findViewById(R.id.layout_brush)
		sbBrushColor = findViewById(R.id.seekbar_brush_size)
		sbBrushOpacity = findViewById(R.id.seekbar_brush_opacity)
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
		lBrush.visibility = View.GONE
		sbBrushColor.progress = 5
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
			//Load image 1
			Timber.d("Load image: ${model.filePath}")
			val inputStream = FileInputStream(model.filePath)
			model.layer0 = BitmapFactory.decodeStream(inputStream)
			//selectedImage = ImageUtils.scaleBitmapKeepAspectRatio(bitmap, size)
			inputStream.close()
		} catch (e: FileNotFoundException) {
			//Load image fallback
			Timber.d("Load image fallback: ${model.filePath}")
			model.layer0 = MediaStore.Images.Media.getBitmap(contentResolver, Uri.parse(model.filePath))
		} catch (e: IOException) {
			Timber.d("Load image failed")
			e.printStackTrace()
		}

		model.layer0?.let {
			val display = windowManager.defaultDisplay
			val size = Point()
			display.getSize(size)
			val width = size.x
			val height = size.y
			model.layer0 = CanvasView.convertToMutable(model.layer0!!)
			model.layer0 = model.layer0!!.scaleKeepAspectRatio(width, height)
			imageView.adjustViewBounds = false
			imageView.scaleType = ImageView.ScaleType.CENTER_INSIDE
			imageView.setImageDrawable(BitmapDrawable(resources, model.layer0))
		}
	}
	
	

	private fun setListeners() {
		colorPickLayout.singleClickThresholdLimit = 80
		colorPickLayout.singleClickListener = object: ColorPickLayout.SingleClickListener {
			override fun onSingleClick() {
				controller.onSingleClick()
			}
		}
		canvas.singleClickThresholdLimit = 80
		canvas.singleClickListener = object: CanvasView.SingleClickListener {
			override fun onSingleClick() {
				controller.onSingleClick()
			}
		}
		canvas.touchStartedListener = object: CanvasView.TouchStartedListener {
			override fun onTouchStarted() {
				controller.onCanvasTouchStarted()
			}
		}
		btnColor.setOnClickListener {
			if(model.mode==Mode.draw){
				model.mode = Mode.pick
				Toast.makeText(this, "Arrastra el dedo por la imagen para seleccionar un color. ", Toast.LENGTH_LONG).show()
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
				colorDisplay.visibility = View.GONE
				lBrush.visibility = View.GONE
				eraserDisplay.visibility = View.VISIBLE
			}
			canvas.update()
		}
		eraserDisplay.setOnClickListener {
			//show size picker
			controller.onEraserDisplayClick()
		}
		colorDisplay.setOnClickListener {
			//show size
			controller.onBrushDisplayClick()
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
		sbBrushColor.setOnSeekBarChangeListener(object: SeekBar.OnSeekBarChangeListener{
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
		sbBrushOpacity.setOnSeekBarChangeListener(object: SeekBar.OnSeekBarChangeListener{
			override fun onProgressChanged(p0: SeekBar?, p1: Int, p2: Boolean) {
				model.alpha = p1
				colorDisplay.colorAlpha = p1
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
		if(model.showForwardButton) {
			btnRedo.setOnClickListener {
				canvas.redo()
			}
			btnRedo.visibility = View.VISIBLE
		}else{
			btnRedo.visibility = View.GONE
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
					Timber.d("onColorSelected 1: ${envelope.color}")
					colorDisplay.fillColor = model.color
					Timber.d("onColorSelected 2: ${colorDisplay.fillColor}")
				}
			}
		})
	}

	override fun onDestroy() {
		canvas.onDestroy()
		super.onDestroy()
	}
	
	fun switchEraserSeekBarVisibility() {
		if(eraserSeekbar.visibility == View.VISIBLE) {
			hideEraserSeekBar()
		}else{
			showEraserSeekBar()
		}
	}
	
	fun showEraserSeekBar() {
		eraserSeekbar.visibility = View.VISIBLE
	}
	
	fun hideEraserSeekBar() {
		eraserSeekbar.visibility = View.GONE
	}
	
	fun switchBrushSeekBarsVisibility() {
		if(lBrush.visibility!=View.VISIBLE) {
			showBrushSeekBar()
		}else{
			hideBrushSeekBar()
		}
	}
	
	fun showBrushSeekBar() {
		lBrush.visibility = View.VISIBLE
	}
	
	fun hideBrushSeekBar() {
		lBrush.visibility = View.GONE
	}
	
	override fun onBackPressed() {
		if(canvas.hasSomething){
			AlertDialog.Builder(this)
					.setMessage(R.string.Has_changes_would_you_like_to_exit)
					.setPositiveButton(R.string.Exit) { dialogInterface: DialogInterface, i: Int -> super.onBackPressed() }
					.setNegativeButton(R.string.Keep_here) { dialogInterface: DialogInterface, i: Int -> dialogInterface.cancel() }
					.show()
		}else{
			super.onBackPressed()
		}
	}
}
