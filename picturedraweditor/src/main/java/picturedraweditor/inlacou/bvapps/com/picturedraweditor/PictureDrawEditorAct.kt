package picturedraweditor.inlacou.bvapps.com.picturedraweditor

import android.content.Intent
import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import android.widget.ImageView
import com.github.salomonbrys.kotson.fromJson
import com.google.gson.Gson
import android.graphics.drawable.BitmapDrawable
import android.graphics.BitmapFactory
import android.graphics.Point
import android.graphics.drawable.ColorDrawable
import android.net.Uri
import android.provider.MediaStore
import android.util.Log
import android.view.View
import android.view.ViewTreeObserver
import android.widget.Button
import colorpickerlayout.inlacou.bvapps.com.colorpicklayout.ColorListener
import colorpickerlayout.inlacou.bvapps.com.colorpicklayout.ColorPickLayout
import colorpickerlayout.inlacou.bvapps.com.colorpicklayout.ColorWrapper
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
	lateinit private var color: Button
	lateinit private var erase: Button
	lateinit private var canvas: CanvasView

	companion object {

		private val DEBUG_TAG = PictureDrawEditorAct::class.java.simpleName

		fun navigate(activity: AppCompatActivity, mdl: PictureDrawEditorMdl) {
			val intent = Intent(activity, PictureDrawEditorAct::class.java)

			intent.putExtra("model", Gson().toJson(mdl))

			activity.startActivity(intent)
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
		if (intent.hasExtra("model")) model = Gson().fromJson<PictureDrawEditorMdl>(intent.extras.getString("model"))
	}

	private fun initialize(savedInstanceState: Bundle?) {
		imageView = findViewById(R.id.image)
		colorPickLayout = findViewById(R.id.colorPickLayout)
		canvas = findViewById(R.id.canvas)
		color = findViewById(R.id.color)
		erase = findViewById(R.id.erase)
		controller = PictureDrawEditorCtrl(view = this, model = model)
		canvas.setModel(model)
	}

	private fun populate() {
		Log.d(DEBUG_TAG, "model: $model")

		//Utils.resizeView(imageView, -1, (size.y*0.5).toInt())
		imageView.viewTreeObserver.addOnGlobalLayoutListener(object: ViewTreeObserver.OnGlobalLayoutListener{
			override fun onGlobalLayout() {
				var size = Point()
				windowManager.defaultDisplay.getRealSize(size)
				Log.d(DEBUG_TAG, "screen size: $size | proportion: ${size.x.toDouble()/size.y}")
				Log.d(DEBUG_TAG, "image size: ${imageView.width}, ${imageView.height} | proportion: ${imageView.width.toDouble()/imageView.height}")
				//Log.d(DEBUG_TAG, "new screen size: $size | proportion: ${size.x.toDouble()/size.y}")
				//Log.d(DEBUG_TAG, "new image size: ${imageView.width}, ${imageView.width*size.y/size.x} | proportion: ${imageView.width.toDouble()/(imageView.width*size.y/size.x)}")

				imageView.viewTreeObserver.removeOnGlobalLayoutListener(this)
				//Utils.resizeView(imageView, -1, imageView.width*size.y/size.x)
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

		if (model.layer0 == null) {
			return
		}
		imageView.adjustViewBounds = true
		imageView.scaleType = ImageView.ScaleType.CENTER_INSIDE
		val drawable = BitmapDrawable(resources, model.layer0)
		imageView.setImageDrawable(drawable)
	}

	private fun setListeners() {
		color.setOnClickListener {
			if(model.mode==Mode.draw){
				model.mode = Mode.pick
			}else{
				model.mode = Mode.draw
			}
			canvas.update()
		}
		erase.setOnClickListener {
			if(model.mode==Mode.erase){
				model.mode = Mode.draw
			}else{
				model.mode = Mode.erase
			}
			canvas.update()
		}
		colorPickLayout.setSelector(object: ColorListener{
			override fun onColorSelected(envelope: ColorWrapper) {
				color.background = ColorDrawable(envelope.color)
				model.color = envelope.color
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
		super.onDestroy()
	}

}
