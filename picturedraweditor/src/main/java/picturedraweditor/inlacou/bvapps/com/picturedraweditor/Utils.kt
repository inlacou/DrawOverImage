package picturedraweditor.inlacou.bvapps.com.picturedraweditor

import android.content.res.Resources
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Matrix
import android.graphics.Paint
import android.os.Build
import timber.log.Timber

/**
 * Scale to exactly width*height
 */
internal fun Bitmap.scaleKeepAspectRatio(width: Int, height: Int): Bitmap {
	Timber.d("scaleKeepAspectRatio | original width $width and height $height")
	if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
		Timber.d("scaleKeepAspectRatio | original allocationByteCount ${allocationByteCount.toDouble()/1024}mb")
	}
	Timber.d("scaleKeepAspectRatio | scaleSize $width*$height")
	if(width==width && height==height) return this
	val background = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
	val originalWidth = width.toFloat()
	val originalHeight = height.toFloat()
	val canvas = Canvas(background)
	val scale = width / originalWidth
	val xTranslation = 0.0f
	val yTranslation = (height - originalHeight * scale) / 2.0f
	val transformation = Matrix()
	transformation.postTranslate(xTranslation, yTranslation)
	transformation.preScale(scale, scale)
	val paint = Paint()
	paint.isFilterBitmap = true
	canvas.drawBitmap(this, transformation, paint)
	return background.also {
		Timber.d("resulting width $width and height $height")
		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
			Timber.d("scaleKeepAspectRatio | resulting allocationByteCount ${allocationByteCount.toDouble()/1024}mb")
		}
	}
}

internal val Int.toPx: Int
	get() = (this * Resources.getSystem().displayMetrics.density).toInt()