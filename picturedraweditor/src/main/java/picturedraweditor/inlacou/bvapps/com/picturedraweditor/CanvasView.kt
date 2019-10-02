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
import android.view.MotionEvent
import android.view.View
import timber.log.Timber
import java.io.*

import java.nio.channels.FileChannel
import java.util.ArrayList
import kotlin.concurrent.thread
import kotlin.math.abs

/**
 * Created by inlacou on 20/12/17.
 * Last updated by inlacou on 2/10/19.
 */
class CanvasView(context: Context, attrs: AttributeSet) : View(context, attrs) {

	private var mCanvas: Canvas? = null
	private var currentPath: Path = Path()
	private var currentPaint: Paint = Paint()
	private var mX: Float = 0.toFloat()
	private var mY: Float = 0.toFloat()
	private var model: PictureDrawEditorMdl? = null

	var touchStartedListener: TouchStartedListener? = null
	var singleClickListener: SingleClickListener? = null
	//Variable know the value that indicates a click was a single click, and not a longer click
	private var actionDownTime: Long = 0
	//Variable to hold time past since action_down started
	var singleClickThresholdLimit = 80

	private val paths = ArrayList<Path>()
	private val paints = ArrayList<Paint>()
	private val redoPaths = ArrayList<Path>()
	private val redoPaints = ArrayList<Paint>()

	val hasSomething get() = paints.isNotEmpty() && paths.isNotEmpty()
	
	fun setModel(model: PictureDrawEditorMdl) {
		Timber.d("setModel")
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
		
		model.let {
			if(it!=null && it.mode==Mode.erase){
				currentPaint.color = Color.TRANSPARENT
				currentPaint.xfermode = PorterDuffXfermode(PorterDuff.Mode.CLEAR)
				currentPaint.strokeWidth = it.eraserWidth.toPx.toFloat()
			} else {
				currentPaint.strokeWidth = it?.colorWidth?.toPx?.toFloat() ?: 15.toPx.toFloat()
				currentPaint.color = it?.color ?: Color.BLACK
				currentPaint.alpha = model?.alpha ?: 180
			}
		}
		
		currentPaint.style = Paint.Style.STROKE
		currentPaint.strokeJoin = Paint.Join.ROUND
	}

	init {
		this.isDrawingCacheEnabled = true
		setLayerType(LAYER_TYPE_HARDWARE, null)
		update()
	}

	// override onSizeChanged
	override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
		super.onSizeChanged(w, h, oldw, oldh)
		Timber.d("onSizeChanged | size:   $w, $h")
		Timber.d("onSizeChanged | layer0: ${model!!.layer0!!.width}, ${model!!.layer0!!.height}")

		updateBitmap(w, h)
	}

	private fun updateBitmap(w: Int, h: Int) {
		// your Canvas will draw onto the defined Bitmap
		if(mCanvas==null) {
			mCanvas = Canvas(model!!.layer0!!)
		}else{
			Timber.d("updateBitmap | ignored")
		}
	}

	fun saveImage(listener: FileSavedListener) {
		Timber.d("saveImage | start!")
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
				//val bm = overlay(model!!.layer0!!, mBitmap!!)
				model!!.layer0!!.compress(Bitmap.CompressFormat.PNG, 90, out)
				//bm.recycle()
				model?.layer0?.recycle()
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
		touchStartedListener?.onTouchStarted()
		actionDownTime = System.currentTimeMillis()
		currentPath.moveTo(x, y)
		mX = x
		mY = y
	}

	// when ACTION_MOVE move touch according to the x,y values
	private fun moveTouch(x: Float, y: Float) {
		val dx = abs(x - mX)
		val dy = abs(y - mY)
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

	fun onDestroy(){
		model?.layer0?.recycle()
	}

	companion object {

		private val TOLERANCE = 5f

		@Deprecated("Just here for reference")
		private fun overlay(bmp1: Bitmap, bmp2: Bitmap): Bitmap {
			val w1 = bmp1.width
			val h1 = bmp1.width
			val bmOverlay = Bitmap.createBitmap(w1, h1, bmp1.config)
			val canvas = Canvas(bmOverlay)
			canvas.drawBitmap(bmp1, Matrix(), null)
			bmp1.recycle()
			val scaledBmp2 = Bitmap.createScaledBitmap(bmp2, w1, h1, false)
			canvas.drawBitmap(scaledBmp2, 0f, 0f, null)
			bmp2.recycle()
			scaledBmp2.recycle()
			return bmOverlay
		}

		fun convertToMutable(imgIn: Bitmap): Bitmap {
			var img = imgIn
			try {
				//this is the file going to use temporally to save the bytes.
				// This file will not be a image, it will store the raw image data.
				val file = File(Environment.getExternalStorageDirectory().toString() + File.separator + "temp.tmp")

				//Open an RandomAccessFile
				//Make sure you have added uses-permission android:name="android.permission.WRITE_EXTERNAL_STORAGE"
				//into AndroidManifest.xml file
				val randomAccessFile = RandomAccessFile(file, "rw")

				// get the width and height of the source bitmap.
				val width = img.width
				val height = img.height
				val type = img.config

				//Copy the byte to the file
				//Assume source bitmap loaded using options.inPreferredConfig = Config.ARGB_8888;
				val channel = randomAccessFile.channel
				val map = channel.map(FileChannel.MapMode.READ_WRITE, 0, imgIn.rowBytes.toLong() * height.toLong())
				img.copyPixelsToBuffer(map)
				//recycle the source bitmap, this will be no longer used.
				img.recycle()
				System.gc()// try to force the bytes from the imgIn to be released

				//Create a new bitmap to load the bitmap again. Probably the memory will be available.
				img = Bitmap.createBitmap(width, height, type)
				map.position(0)
				//load it back from temporary
				img.copyPixelsFromBuffer(map)
				//close the temporary file and channel , then delete that also
				channel.close()
				randomAccessFile.close()

				// delete the temp file
				file.delete()

			} catch (e: FileNotFoundException) {
				e.printStackTrace()
			} catch (e: IOException) {
				e.printStackTrace()
			}

			return img
		}
	}

	interface FileSavedListener{
		fun onFileSaved(file: File)
	}

	interface SingleClickListener{
		fun onSingleClick()
	}

	interface TouchStartedListener{
		fun onTouchStarted()
	}

}