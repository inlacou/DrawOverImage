package picturedraweditor.inlacou.bvapps.com.picturedraweditor.example

import android.content.Intent
import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.widget.Button
import libraries.inlacou.com.imagegetter.ImageGetter
import picturedraweditor.inlacou.bvapps.com.picturedraweditor.PictureDrawEditorAct
import picturedraweditor.inlacou.bvapps.com.picturedraweditor.PictureDrawEditorMdl

class ExampleActivity : AppCompatActivity() {

	private val REQUEST_CODE_SELECT_PICTURE: Int = 0
	private val REQUEST_CODE_CROP: Int = 1
	private val REQUEST_CODE_EDIT: Int = 2

	private var imageGetter: ImageGetter? = null

	override fun onCreate(savedInstanceState: Bundle?) {
		super.onCreate(savedInstanceState)
		setContentView(R.layout.activity_example)
		imageGetter = ImageGetter(this@ExampleActivity,
				crop = false, circular = false, fixed = true,
				useCamera = true, useGallery = true,
				width = 1, height = 1,
				request_code_select_picture = REQUEST_CODE_SELECT_PICTURE, request_code_crop = REQUEST_CODE_CROP,
				callbacks = getImageGetterCallback())

		findViewById<Button>(R.id.startButton).setOnClickListener({ imageGetter!!.start("") })
	}

	private fun getImageGetterCallback(): ImageGetter.Callbacks {
		return object : ImageGetter.Callbacks {
			override fun setImage(path: String, tag: String?) {
				//Get image from path and do whatever you want with it.
				//For example, load it into an imageView
				Log.d("ExampleAct", "path: $path")
				PictureDrawEditorAct.navigateForResult(this@ExampleActivity, REQUEST_CODE_EDIT, PictureDrawEditorMdl(path, true, true))
			}
		}
	}

	override fun onBackPressed() {
		//If you want ImageGetter to delete the image from external memory (recommended)
		if (imageGetter != null) imageGetter?.destroy()
		super.onBackPressed()
	}

	protected override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
		super.onActivityResult(requestCode, resultCode, data)
		imageGetter?.onActivityResult(requestCode, resultCode, data)
	}

	override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<String>, grantResults: IntArray) {
		imageGetter?.onRequestPermissionsResult(requestCode, permissions, grantResults)
	}

	protected override fun onSaveInstanceState(outState: Bundle) {
		super.onSaveInstanceState(outState)
		if (imageGetter != null)
			imageGetter?.onSaveInstanceState(outState)
	}

	protected override fun onRestoreInstanceState(savedInstanceState: Bundle) {
		super.onRestoreInstanceState(savedInstanceState)
		imageGetter = ImageGetter.onRestoreInstanceState(savedInstanceState, this@ExampleActivity, getImageGetterCallback())
	}
}
