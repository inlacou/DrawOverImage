package picturedraweditor.inlacou.bvapps.com.picturedraweditor;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffXfermode;
import android.os.Environment;
import android.util.AttributeSet;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;

import java.io.File;
import java.io.FileOutputStream;
import java.util.ArrayList;

/**
 * Created by inlacou on 20/12/17.
 */
public class CanvasView extends View {

	private static final String DEBUG_TAG = CanvasView.class.getSimpleName();

	private Bitmap mBitmap;
	private Canvas mCanvas;
	private Path currentPath;
	Context context;
	private Paint currentPaint;
	private float mX, mY;
	private static final float TOLERANCE = 5;
	private PictureDrawEditorMdl model;

	private ArrayList<Path> paths = new ArrayList<>();
	private ArrayList<Paint> paints = new ArrayList<>();

	public void setModel(PictureDrawEditorMdl model) {
		this.model = model;
	}

	public void update(){
		// we set a new Path
		if(currentPath!=null && currentPaint!=null){
			paths.add(currentPath);
			paints.add(currentPaint);
		}
		currentPath = new Path();

		// and we set a new Paint with the desired attributes
		currentPaint = new Paint();
		currentPaint.setAntiAlias(true);
		if(model != null && model.getMode()==Mode.erase) {
			currentPaint.setColor(Color.TRANSPARENT);
			currentPaint.setXfermode(new PorterDuffXfermode(PorterDuff.Mode.CLEAR));
		} else {
			if (model != null && model.getColor() != null) {
				currentPaint.setColor(model.getColor());
			} else {
				currentPaint.setColor(Color.BLACK);
			}
			//Set alpha to 0.7
			currentPaint.setAlpha(180);
		}
		currentPaint.setStyle(Paint.Style.STROKE);
		currentPaint.setStrokeJoin(Paint.Join.ROUND);
		currentPaint.setStrokeWidth(50f);
	}

	public CanvasView(Context c, AttributeSet attrs) {
		super(c, attrs);
		context = c;
		this.setDrawingCacheEnabled(true);
		setLayerType(LAYER_TYPE_HARDWARE, null);
		update();
	}

	// override onSizeChanged
	@Override
	protected void onSizeChanged(int w, int h, int oldw, int oldh) {
		super.onSizeChanged(w, h, oldw, oldh);
		Log.d(DEBUG_TAG+".onSizeChanged", "size:   " + w + ", " + h);
		Log.d(DEBUG_TAG+".onSizeChanged", "layer0: " + model.getLayer0().getWidth() + ", " + model.getLayer0().getHeight());

		updateBitmap(w, h);
	}

	private void updateBitmap(int w, int h) {
		// your Canvas will draw onto the defined Bitmap
		mBitmap = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888);
		mCanvas = new Canvas(mBitmap);
	}

	public static Bitmap overlay(Bitmap bmp1, Bitmap bmp2) {
		Log.d(DEBUG_TAG+".overlay1", bmp1.getWidth() + ", " + bmp1.getHeight());
		Log.d(DEBUG_TAG+".overlay2", bmp2.getWidth() + ", " + bmp2.getHeight());
		Bitmap bmOverlay = Bitmap.createBitmap(bmp1.getWidth(), bmp1.getHeight(), bmp1.getConfig());
		Bitmap scaledBmp2 = Bitmap.createScaledBitmap(bmp2, bmp1.getWidth(), bmp1.getHeight(), false);
		Canvas canvas = new Canvas(bmOverlay);
		canvas.drawBitmap(bmp1, new Matrix(), null);
		canvas.drawBitmap(scaledBmp2, 0, 0, null);
		return bmOverlay;
	}

	public void saveImage() {
		//Draw background into the canvas
		//mCanvas.drawBitmap(model.getLayer0(), 0, 0, null);
		for (int i=0; i<paths.size(); i++){
			mCanvas.drawPath(paths.get(i), paints.get(i));
		}
		mCanvas.drawPath(currentPath, currentPaint);
		String root = Environment.getExternalStorageDirectory().toString();
		File myDir = new File(root);
		myDir.mkdirs();
		String fname = "Image-" + System.currentTimeMillis() + ".jpg";
		File file = new File(myDir, fname);
		if (file.exists()) file.delete();
		try {
			FileOutputStream out = new FileOutputStream(file);
			overlay(model.getLayer0(), mBitmap).compress(Bitmap.CompressFormat.PNG, 90, out);
			out.flush();
			out.close();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	// override onDraw
	@Override
	protected void onDraw(Canvas canvas) {
		super.onDraw(canvas);
		// Draw image into the canvas (it will be too big, and only top-left side will be renderized)
		//canvas.drawBitmap(model.getLayer0(), 0, 0, null);
		// Draw the currentPath with the currentPaint on the canvas when onDraw
		for (int i=0; i<paths.size(); i++){
			canvas.drawPath(paths.get(i), paints.get(i));
		}
		canvas.drawPath(currentPath, currentPaint);
	}

	// when ACTION_DOWN start touch according to the x,y values
	private void startTouch(float x, float y) {
		currentPath.moveTo(x, y);
		mX = x;
		mY = y;
	}

	// when ACTION_MOVE move touch according to the x,y values
	private void moveTouch(float x, float y) {
		float dx = Math.abs(x - mX);
		float dy = Math.abs(y - mY);
		if (dx >= TOLERANCE || dy >= TOLERANCE) {
			currentPath.quadTo(mX, mY, (x + mX) / 2, (y + mY) / 2);
			mX = x;
			mY = y;
		}
	}

	// when ACTION_UP stop touch
	private void upTouch() {
		currentPath.lineTo(mX, mY);
	}

	public void clearCanvas() {
		currentPath.reset();
		invalidate();
	}

	@Override
	public boolean onTouchEvent(MotionEvent event) {
		float x = event.getX();
		float y = event.getY();

		if(model.getMode()==Mode.draw || model.getMode()==Mode.erase) {
			switch (event.getAction()) {
				case MotionEvent.ACTION_DOWN:
					startTouch(x, y);
					invalidate();
					break;
				case MotionEvent.ACTION_MOVE:
					moveTouch(x, y);
					invalidate();
					break;
				case MotionEvent.ACTION_UP:
					upTouch();
					invalidate();
					update();
					break;
			}
			return true;
		}else return false;
	}
}