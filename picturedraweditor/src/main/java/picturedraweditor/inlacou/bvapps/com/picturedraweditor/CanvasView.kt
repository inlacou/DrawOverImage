package picturedraweditor.inlacou.bvapps.com.picturedraweditor

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Matrix
import android.graphics.Paint
import android.graphics.Path
import android.graphics.PorterDuff
import android.graphics.PorterDuffXfermode
import android.os.Environment
import android.util.AttributeSet
import android.util.Log
import android.view.MotionEvent
import android.view.View
import colorpickerlayout.inlacou.bvapps.com.colorpicklayout.ColorPickLayout

import java.io.File
import java.io.FileOutputStream
import java.util.ArrayList
import kotlin.concurrent.thread

/**
 * Created by inlacou on 20/12/17.
 */
class CanvasView(internal var context: Context, attrs: AttributeSet) : View(context, attrs) {

	private var mBitmap: Bitmap? = null
	private var mCanvas: Canvas? = null
	private var currentPath: Path = Path()
	private var currentPaint: Paint = Paint()
	private var mX: Float = 0.toFloat()
	private var mY: Float = 0.toFloat()
	private var model: PictureDrawEditorMdl? = null

	private var actionDownTime: Long = 0
	var singleClickListener: SingleClickListener? = null
	var singleClickThresholdLimit = 80

	private val paths = ArrayList<Path>()
	private val paints = ArrayList<Paint>()
	private val redoPaths = ArrayList<Path>()
	private val redoPaints = ArrayList<Paint>()

	fun setModel(model: PictureDrawEditorMdl) {
		this.model = model
	}

	fun update() {
		redoPaths.clear()
		redoPaints.clear()

		// we set a new Path
		currentPath = Path()

		// and we set a new Paint with the desired attributes
		currentPaint = Paint()
		currentPaint.isAntiAlias = true
		if (model != null && model!!.mode === Mode.erase) {
			currentPaint.color = Color.TRANSPARENT
			currentPaint.xfermode = PorterDuffXfermode(PorterDuff.Mode.CLEAR)
			currentPaint.strokeWidth = model!!.eraserWidth.toPx.toFloat()
		} else {
			currentPaint.strokeWidth = model?.colorWidth?.toPx?.toFloat() ?: 15.toPx.toFloat()
			if (model != null) {
				currentPaint.color = model!!.color
			} else {
				currentPaint.color = Color.BLACK
			}
			//Set alpha to 0.7
			currentPaint.alpha = 180
		}
		currentPaint.style = Paint.Style.STROKE
		currentPaint.strokeJoin = Paint.Join.ROUND
	}

	init {
		this.isDrawingCacheEnabled = true
		setLayerType(View.LAYER_TYPE_HARDWARE, null)
		update()
	}

	// override onSizeChanged
	override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
		super.onSizeChanged(w, h, oldw, oldh)
		Log.d(DEBUG_TAG + ".onSizeChanged", "size:   $w, $h")
		Log.d(DEBUG_TAG + ".onSizeChanged", "layer0: " + model!!.layer0!!.width + ", " + model!!.layer0!!.height)

		updateBitmap(w, h)
	}

	private fun updateBitmap(w: Int, h: Int) {
		// your Canvas will draw onto the defined Bitmap
		mBitmap = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
		mCanvas = Canvas(mBitmap!!)
	}

	fun saveImage(listener: FileSavedListener) {
		Log.d(DEBUG_TAG+".saveImage", "start!")
		//Draw background into the canvas
		//mCanvas.drawBitmap(model.getLayer0(), 0, 0, null);
		for (i in paths.indices) {
			mCanvas!!.drawPath(paths[i], paints[i])
		}
		mCanvas!!.drawPath(currentPath, currentPaint)
		thread(start = true) {
			println("running from thread(): ${Thread.currentThread()}")
			val root = Environment.getExternalStorageDirectory().toString()
			val myDir = File(root)
			myDir.mkdirs()
			val fname = "Image-" + System.currentTimeMillis() + ".jpg"
			val file = File(myDir, fname)
			if (file.exists()) file.delete()
			try {
				val out = FileOutputStream(file)
				overlay(model!!.layer0, mBitmap).compress(Bitmap.CompressFormat.PNG, 90, out)
				out.flush()
				out.close()
			} catch (e: Exception) {
				e.printStackTrace()
			}
			listener.onFileSaved(file)
		}

	}

	// override onDraw
	override fun onDraw(canvas: Canvas) {
		super.onDraw(canvas)
		// Draw image into the canvas (it will be too big, and only top-left side will be renderized)
		//canvas.drawBitmap(model.getLayer0(), 0, 0, null);
		// Draw the currentPath with the currentPaint on the canvas when onDraw
		for (i in paths.indices) {
			canvas.drawPath(paths[i], paints[i])
		}
		canvas.drawPath(currentPath, currentPaint)
	}

	// when ACTION_DOWN start touch according to the x,y values
	private fun startTouch(x: Float, y: Float) {
		actionDownTime = System.currentTimeMillis()
		currentPath.moveTo(x, y)
		mX = x
		mY = y
	}

	// when ACTION_MOVE move touch according to the x,y values
	private fun moveTouch(x: Float, y: Float) {
		val dx = Math.abs(x - mX)
		val dy = Math.abs(y - mY)
		if (dx >= TOLERANCE || dy >= TOLERANCE) {
			currentPath.quadTo(mX, mY, (x + mX) / 2, (y + mY) / 2)
			mX = x
			mY = y
		}
	}

	// when ACTION_UP stop touch
	private fun upTouch() {
		if(System.currentTimeMillis()-actionDownTime<singleClickThresholdLimit) singleClickListener?.onSingleClick()
		currentPath.lineTo(mX, mY)
		if(!currentPath.isEmpty) {
			paths.add(currentPath)
			paints.add(currentPaint)
		}
	}

	fun clearCanvas() {
		currentPath.reset()
		invalidate()
	}

	/**
	 * @return true if more undos are possible
	 */
	fun undo(): Boolean {
		try {
			redoPaths.add(paths.removeAt(paths.size-1))
			redoPaints.add(paints.removeAt(paints.size-1))
			invalidate()
		}catch (ioobe : IndexOutOfBoundsException){}
		return redoPaths.size>0
	}

	/**
	 * @return true if more redos are possible
	 */
	fun redo(): Boolean {
		try {
			paths.add(redoPaths.removeAt(redoPaths.size - 1))
			paints.add(redoPaints.removeAt(redoPaints.size - 1))
			invalidate()
		}catch (ioobe : IndexOutOfBoundsException){}
		return paths.size>0
	}

	override fun onTouchEvent(event: MotionEvent): Boolean {
		val x = event.x
		val y = event.y

		if (model!!.mode == Mode.draw || model!!.mode == Mode.erase) {
			when (event.action) {
				MotionEvent.ACTION_DOWN -> {
					startTouch(x, y)
					invalidate()
				}
				MotionEvent.ACTION_MOVE -> {
					moveTouch(x, y)
					invalidate()
				}
				MotionEvent.ACTION_UP -> {
					upTouch()
					invalidate()
					update()
				}
			}
			return true
		} else
			return false
	}

	companion object {

		private val DEBUG_TAG = CanvasView::class.java.simpleName
		private val TOLERANCE = 5f

		fun overlay(bmp1: Bitmap?, bmp2: Bitmap?): Bitmap {
			Log.d(DEBUG_TAG + ".overlay1", bmp1!!.width.toString() + ", " + bmp1.height)
			Log.d(DEBUG_TAG + ".overlay2", bmp2!!.width.toString() + ", " + bmp2.height)
			val bmOverlay = Bitmap.createBitmap(bmp1.width, bmp1.height, bmp1.config)
			val scaledBmp2 = Bitmap.createScaledBitmap(bmp2, bmp1.width, bmp1.height, false)
			val canvas = Canvas(bmOverlay)
			canvas.drawBitmap(bmp1, Matrix(), null)
			canvas.drawBitmap(scaledBmp2, 0f, 0f, null)
			return bmOverlay
		}
	}

	interface FileSavedListener{
		fun onFileSaved(file: File)
	}

	interface SingleClickListener{
		fun onSingleClick()
	}

}