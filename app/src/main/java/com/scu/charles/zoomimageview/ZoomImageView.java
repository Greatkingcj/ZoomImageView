package com.scu.charles.zoomimageview;

import android.content.Context;
import android.graphics.Matrix;
import android.graphics.RectF;
import android.graphics.drawable.Drawable;
import android.support.v4.view.ViewPager;
import android.util.AttributeSet;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.ScaleGestureDetector;
import android.view.ScaleGestureDetector.OnScaleGestureListener;
import android.view.View;
import android.view.ViewConfiguration;
import android.view.ViewTreeObserver.OnGlobalLayoutListener;
import android.widget.ImageView;
import android.view.View.OnTouchListener;

;

public class ZoomImageView extends ImageView implements OnGlobalLayoutListener,
		OnScaleGestureListener, OnTouchListener {

	private boolean mOnce = false;
	/**
	 * 初始化缩放的值
	 */
	private float mInitScale;
	/**
	 * 中间值ֵ
	 */
	private float mMidScale;
	/**
	 * 最大值
	 */
	private float mMaxScale;
	private Matrix mScaleMatrix;

	// -----------------自由移动-------------------
	/**
	 * 记录上一次多点触控的数量
	 */
	private int mLastPointerCount;
	private float mLastX;
	private float mLastY;
	
	private int mTouchSlop;
	
	private boolean isCheckLeftAndRight;
	private boolean isCheckTopAndBottom;

	private boolean isCanDrag;
	private ScaleGestureDetector mScaleGestureDetector;
	
	//--------------------双击放大与缩小-------------------------
	private GestureDetector mGestureDetector;
	private boolean isAutoScale;
	
	
	public ZoomImageView(Context context, AttributeSet attrs, int defStyle) {
		super(context, attrs, defStyle);
		setScaleType(ScaleType.MATRIX);
		mScaleMatrix = new Matrix();
		mScaleGestureDetector = new ScaleGestureDetector(context, this);
		setOnTouchListener(this);
		
		mTouchSlop = ViewConfiguration.get(context).getScaledTouchSlop();
		mGestureDetector = new GestureDetector(context,
				new GestureDetector.SimpleOnGestureListener(){
			@Override
			public boolean onDoubleTap(MotionEvent e) {
				
				if(isAutoScale)
					return true;
				
				float x =e.getX();
				float y =e.getY();
				if(getScale()<mMidScale){
//					mScaleMatrix.postScale(mMidScale/getScale(), mMidScale/getScale(),x,y);
//					setImageMatrix(mScaleMatrix);
					postDelayed(new AutoScaleRunnable(mMidScale, x, y), 16);
					isAutoScale = true;
				}else{
//					mScaleMatrix.postScale(mInitScale/getScale(), mInitScale/getScale(),x,y);
//					setImageMatrix(mScaleMatrix);
					postDelayed(new AutoScaleRunnable(mInitScale, x, y), 16);
					isAutoScale = true;
				}
				return true;
			}
		});
	}
	
	private class AutoScaleRunnable implements Runnable{
		private float mTargetScale;
		private float x;
		private float y;
		
		private final float BIGGER = 1.07f;
		private final float SMALL = 0.93f;
		private float tmpScale;
		
		
		public AutoScaleRunnable(float mTargetScale, float x, float y) {
			super();
			this.mTargetScale = mTargetScale;
			this.x = x;
			this.y = y;
			
			if(getScale()<mTargetScale){
				tmpScale = BIGGER;
			}
			
			if(getScale()>mTargetScale){
				tmpScale = SMALL;
			}
		}



		@Override
		public void run() {
			//进行缩放
			mScaleMatrix.postScale(tmpScale, tmpScale,x,y);
			checkBorderAndCenter();
			setImageMatrix(mScaleMatrix);
			
			float currentScale = getScale();
			
			if((tmpScale>1.0f&&currentScale<mTargetScale)
					||(tmpScale<1.0f&&currentScale>mTargetScale)){
				postDelayed(this, 16);
			}else{
				float scale = getScale();
				mScaleMatrix.postScale(scale, scale,x,y);
				checkBorderAndCenter();
				setImageMatrix(mScaleMatrix);
				
				isAutoScale = false;
			}
		}
		
	}
	

	public ZoomImageView(Context context, AttributeSet attrs) {
		this(context, attrs, 0);
	}

	public ZoomImageView(Context context) {
		this(context, null);
	}

	@Override
	protected void onAttachedToWindow() {
		super.onAttachedToWindow();
		getViewTreeObserver().addOnGlobalLayoutListener(this);
	}

	@SuppressWarnings("deprecation")
	@Override
	protected void onDetachedFromWindow() {
		super.onDetachedFromWindow();
		getViewTreeObserver().removeGlobalOnLayoutListener(this);
	}

	/**
	 * 获取图片和控件的宽高，并进行比较缩放
	 */
	@Override
	public void onGlobalLayout() {
		float scale = 1.0f;
		if (!mOnce) {
			int width = getWidth();
			int height = getHeight();

			Drawable d = getDrawable();
			if (d == null)
				return;
			int dw = d.getIntrinsicWidth();
			int dh = d.getIntrinsicHeight();
			/**
			 * 比较图片和控件的宽高确定缩放值
			 */
			if (dw > width && dh < height) {
				scale = width * 1.0f / dw;
			}

			if (dw < width && dh > height) {
				scale = height * 1.0f / dh;
			}

			if ((dw > width && dh > height) || (dw < width && dh < height)) {
				scale = Math.min(width * 1.0f / dw, height * 1.0f / dh);
			}

			mInitScale = scale;
			mMaxScale = mInitScale * 4;
			mMidScale = mInitScale * 2;

			// 将图片移动到控件中心
			float dx = getWidth() / 2 - dw / 2;
			float dy = getHeight() / 2 - dh / 2;
			mScaleMatrix.postTranslate(dx, dy);
			mScaleMatrix.postScale(mInitScale, mInitScale, width / 2,
					height / 2);
			setImageMatrix(mScaleMatrix);
			mOnce = true;
		}
	}

	/**
	 * 获取当前图片的缩放值
	 * 
	 * @return
	 */
	private float getScale() {
		float[] values = new float[9];
		mScaleMatrix.getValues(values);
		return values[Matrix.MSCALE_X];
	}

	@Override
	public boolean onScale(ScaleGestureDetector detector) {
		float scale = getScale();
		float scaleFactor = detector.getScaleFactor();
		if (getDrawable() == null)
			return true;
		// 缩放范围控制
		if ((scale < mMaxScale && scaleFactor > 1.0f)
				|| (scale > mInitScale && scaleFactor < 1.0f)) {
			if (scale * scaleFactor < mInitScale) {
				scaleFactor = mInitScale / scale;
			}

			if (scale * scaleFactor > mMaxScale) {
				scaleFactor = mMaxScale / scale;
			}

			mScaleMatrix.postScale(scaleFactor, scaleFactor,
					detector.getFocusX(), detector.getFocusY());
			checkBorderAndCenter();

			setImageMatrix(mScaleMatrix);
		}
		return true;
	}

	private RectF getMatrixRectF() {
		Matrix matrix = mScaleMatrix;
		RectF rectF = new RectF();
		Drawable d = getDrawable();
		if (d != null) {
			rectF.set(0, 0, d.getIntrinsicWidth(), d.getIntrinsicHeight());
			matrix.mapRect(rectF);
		}

		return rectF;
	}

	private void checkBorderAndCenter() {
		RectF rect = getMatrixRectF();
		float deltaX = 0;
		float deltaY = 0;
		int width = getWidth();
		int height = getHeight();
		if (rect.width() >= width) {
			if (rect.left > 0) {
				deltaX = -rect.left;
			}

			if (rect.right < width) {
				deltaX = width - rect.right;
			}
		}

		if (rect.height() >= height) {
			if (rect.top > 0) {
				deltaY = -rect.top;
			}

			if (rect.bottom < height) {
				deltaY = height - rect.bottom;
			}
		}

		if (rect.width() < width) {
			deltaX = width / 2f - rect.right + rect.width() / 2f;
		}
		if (rect.height() < height) {
			deltaY = height / 2f - rect.bottom + rect.height() / 2f;
		}

		mScaleMatrix.postTranslate(deltaX, deltaY);
	}

	@Override
	public boolean onScaleBegin(ScaleGestureDetector detector) {

		return true;
	}

	@Override
	public void onScaleEnd(ScaleGestureDetector detector) {

	}

	@Override
	public boolean onTouch(View v, MotionEvent event) {
		if(mGestureDetector.onTouchEvent(event))
			return true;
			
		mScaleGestureDetector.onTouchEvent(event);
		
		float x = 0;
		float y = 0;
		int pointerCount = event.getPointerCount();
		for (int i = 0; i < pointerCount; i++) {
			x += event.getX(i);
			y += event.getY(i);
		}

		x /= pointerCount;
		y /= pointerCount;

		if (mLastPointerCount != pointerCount) {
			isCanDrag = false;
			mLastX = x;
			mLastY = y;
		}

		mLastPointerCount = pointerCount;
		RectF rect = getMatrixRectF();
		switch (event.getAction()) {
		case MotionEvent.ACTION_MOVE:
			
			if(rect.width()>getWidth()
					){
				if(getParent()instanceof ViewPager)
				v.getParent().requestDisallowInterceptTouchEvent(true);
			}
			
			float dx = x-mLastX;
			float dy = y-mLastY;
			

			if(!isCanDrag){
				isCanDrag =isMoveAction(dx,dy);
			}
			if(isCanDrag){
				RectF rectF = getMatrixRectF();
				if(getDrawable()!=null){
					isCheckLeftAndRight =true;
					isCheckTopAndBottom = true;
					if(rectF.width()<getWidth()){
						isCheckLeftAndRight =false;
						dx = 0;
					}
					
					if(rectF.height()<getHeight()){
						isCheckTopAndBottom = false;
						dy = 0;
					}
					
					mScaleMatrix.postTranslate(dx, dy);
					checkBorderWhenTranslate();
					setImageMatrix(mScaleMatrix);
					
				}
			}
			mLastX =x;
			mLastY =y;
			break;
		case MotionEvent.ACTION_UP:
		case MotionEvent.ACTION_CANCEL:
			mLastPointerCount =0;
			break;
		case MotionEvent.ACTION_DOWN:
			
			if(rect.width()>getWidth()
					){
				if(getParent()instanceof ViewPager)
				v.getParent().requestDisallowInterceptTouchEvent(true);
			}
			break;
		}

		return true;
	}
	
	/**
	 * 当移动时进行边界检查
	 */
	private void checkBorderWhenTranslate() {

		RectF rectF = getMatrixRectF();
		float deltaX = 0;
		float deltaY = 0;
		int width = getWidth();
		int height = getHeight();
		
		if(rectF.top>0&&isCheckTopAndBottom){
			deltaY = -rectF.top;
		}
		
		if(rectF.bottom<height&&isCheckTopAndBottom){
			deltaY = height-rectF.bottom;
		}
		
		if(rectF.left>0&&isCheckLeftAndRight){
			deltaX = -rectF.left;
		}
		
		if(rectF.right<width&&isCheckLeftAndRight){
			deltaX = width-rectF.right;
		}
		
		mScaleMatrix.postTranslate(deltaX, deltaY);
		
	}

	/**
	 * 判断是否是move
	 * @param dx
	 * @param dy
	 * @return
	 */
	private boolean isMoveAction(float dx, float dy) {
		return Math.sqrt(dx*dx+dy*dy)>mTouchSlop;
	}

}