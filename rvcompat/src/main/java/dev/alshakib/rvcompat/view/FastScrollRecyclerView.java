/*
 * MIT License
 *
 * Copyright (c) 2021 Al Shakib (shakib@alshakib.dev)
 *
 * This file is part of Recycler View Compat
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 *
 */

package dev.alshakib.rvcompat.view;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.ObjectAnimator;
import android.content.Context;
import android.content.res.Resources;
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.Point;
import android.graphics.Rect;
import android.graphics.RectF;
import android.graphics.Typeface;
import android.text.TextUtils;
import android.util.AttributeSet;
import android.util.SparseIntArray;
import android.util.TypedValue;
import android.view.MotionEvent;
import android.view.View;

import androidx.annotation.ColorInt;
import androidx.annotation.IntDef;
import androidx.annotation.Keep;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.interpolator.view.animation.FastOutLinearInInterpolator;
import androidx.interpolator.view.animation.LinearOutSlowInInterpolator;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.lang.annotation.Retention;

import dev.alshakib.rvcompat.R;

import static java.lang.annotation.RetentionPolicy.SOURCE;

public class FastScrollRecyclerView extends RecyclerView
        implements RecyclerView.OnItemTouchListener {
    private boolean isFastScrollEnabled;
    private int currentPositionX;
    private int currentPositionY;
    private int lastKnownPositionY;

    private final CurrentScrollState currentScrollState;
    private final FastScroller fastScroller;
    private final SparseIntArray sparseIntArray;
    private final ScrollOffsetInvalidator scrollOffsetInvalidator;
    private OnFastScrollStateChangeListener onFastScrollStateChangeListener;

    public FastScrollRecyclerView(Context context) {
        this(context, null);
    }

    public FastScrollRecyclerView(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public FastScrollRecyclerView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        TypedArray typedArray = context.getTheme().obtainStyledAttributes(
                attrs, R.styleable.FastScrollRecyclerView, 0, 0);
        try {
            isFastScrollEnabled = typedArray.getBoolean(R.styleable.FastScrollRecyclerView_fastScrollEnabled, true);
        } finally {
            typedArray.recycle();
        }

        currentScrollState = new CurrentScrollState();
        fastScroller = new FastScroller(context, this, attrs);
        scrollOffsetInvalidator = new ScrollOffsetInvalidator();
        sparseIntArray = new SparseIntArray();
    }

    public int getTouchInset() {
        return fastScroller.getTouchInset();
    }

    public int getThumbWidth() {
        return fastScroller.getThumbWidth();
    }

    public int getThumbHeight() {
        return fastScroller.getThumbHeight();
    }

    public int getTrackWidth() {
        return fastScroller.getTrackWidth();
    }

    public int getScrollBarWidth() {
        return fastScroller.getWidth();
    }

    public int getScrollBarThumbHeight() {
        return fastScroller.getThumbHeight();
    }

    public void showScrollbar() {
        fastScroller.show();
    }

    public void setTouchInset(int inset) {
        fastScroller.setTouchInset(inset);
    }

    public void setThumbWidth(int width) {
        fastScroller.setThumbWidth(width);
    }

    public void setThumbHeight(int height) {
        fastScroller.setThumbHeight(height);
    }

    public void setThumbColor(@ColorInt int color) {
        fastScroller.setThumbColor(color);
    }

    public void setTrackWidth(int width) {
        fastScroller.setTrackWidth(width);
    }

    public void setTrackColor(@ColorInt int color) {
        fastScroller.setTrackColor(color);
    }

    public void setPopupBgColor(@ColorInt int color) {
        fastScroller.setPopupBgColor(color);
    }

    public void setPopupTextColor(@ColorInt int color) {
        fastScroller.setPopupTextColor(color);
    }

    public void setPopupTextSize(int textSize) {
        fastScroller.setPopupTextSize(textSize);
    }

    public void setPopUpTypeface(Typeface typeface) {
        fastScroller.setPopupTypeface(typeface);
    }

    public void setAutoHideDelay(int hideDelay) {
        fastScroller.setAutoHideDelay(hideDelay);
    }

    public void setAutoHideEnabled(boolean autoHideEnabled) {
        fastScroller.setAutoHideEnabled(autoHideEnabled);
    }

    public void setOnFastScrollStateChangeListener(OnFastScrollStateChangeListener stateChangeListener) {
        onFastScrollStateChangeListener = stateChangeListener;
    }

    public void setPopupPosition(@FastScroller.PopupPosition int popupPosition) {
        fastScroller.setPopupPosition(popupPosition);
    }

    public void setThumbInactiveColor(@ColorInt int color) {
        fastScroller.setThumbInactiveColor(color);
    }

    public void enableThumbInactiveColor(boolean allowInactiveColor) {
        fastScroller.enableThumbInactiveColor(allowInactiveColor);
    }

    public void setFastScrollEnabled(boolean fastScrollEnabled) {
        isFastScrollEnabled = fastScrollEnabled;
    }

    public String scrollToPositionAtProgress(float touchFraction) {
        int itemCount = 0;
        if (getAdapter() != null) {
            itemCount = getAdapter().getItemCount();
        }

        if (itemCount == 0) {
            return "";
        }

        int spanCount = 1;
        int rowCount = itemCount;
        if (getLayoutManager() instanceof GridLayoutManager) {
            spanCount = ((GridLayoutManager) getLayoutManager()).getSpanCount();
            rowCount = (int) Math.ceil((float) rowCount / (float) spanCount);
        }

        stopScroll();
        updateCurrentScrollState(currentScrollState);

        float itemPos;
        int availableScrollHeight;

        int scrollPosition;
        int scrollOffset;

        if (getAdapter() instanceof OnViewHolderHeight) {
            itemPos = findItemPosition(touchFraction);
            availableScrollHeight = getAvailableScrollHeight(calculateAdapterHeight());
            int passedHeight = (int) (availableScrollHeight * touchFraction);
            scrollPosition = findMeasureAdapterFirstVisiblePosition(passedHeight);
            scrollOffset = calculateScrollDistanceToPosition(scrollPosition) - passedHeight;
        } else {
            itemPos = findItemPosition(touchFraction);
            availableScrollHeight = getAvailableScrollHeight(rowCount *
                    currentScrollState.currentRowHeight);

            int exactItemPosition = (int) (availableScrollHeight * touchFraction);

            // Have smooth scrolling
            float rowHeight = currentScrollState.currentRowHeight > 0 ? currentScrollState.currentRowHeight : 1f;
            scrollPosition = (int) ((float) spanCount * (float) exactItemPosition / rowHeight);
            scrollOffset = (int) -((float) exactItemPosition % rowHeight);
        }

        LinearLayoutManager layoutManager = ((LinearLayoutManager) getLayoutManager());
        if (layoutManager != null) {
            layoutManager.scrollToPositionWithOffset(scrollPosition, scrollOffset);
        }

        if (!(getAdapter() instanceof OnSectionName)) {
            return "";
        }

        int position = (int) ((touchFraction == 1) ? getAdapter().getItemCount() - 1 : itemPos);

        OnSectionName onSectionName = (OnSectionName) getAdapter();
        return onSectionName.getSectionName(position);
    }

    private boolean handleTouchEvent(MotionEvent motionEvent) {
        int action = motionEvent.getAction();
        int x = (int) motionEvent.getX();
        int y = (int) motionEvent.getY();
        switch (action) {
            case MotionEvent.ACTION_DOWN:
                currentPositionX = x;
                currentPositionY = lastKnownPositionY = y;
                fastScroller.handleTouchEvent(motionEvent, currentPositionX, currentPositionY,
                        lastKnownPositionY, onFastScrollStateChangeListener);
                break;
            case MotionEvent.ACTION_MOVE:
                lastKnownPositionY = y;
                fastScroller.handleTouchEvent(motionEvent, currentPositionX, currentPositionY,
                        lastKnownPositionY, onFastScrollStateChangeListener);
                break;
            case MotionEvent.ACTION_UP:
            case MotionEvent.ACTION_CANCEL:
                fastScroller.handleTouchEvent(motionEvent, currentPositionX, currentPositionY,
                        lastKnownPositionY, onFastScrollStateChangeListener);
                break;
        }
        return fastScroller.isDragging();
    }

    private int getAvailableScrollHeight(int adapterHeight) {
        return (getPaddingTop() + adapterHeight + getPaddingBottom()) - getHeight();
    }

    private int getAvailableScrollBarHeight() {
        return (getHeight() - getPaddingTop() - getPaddingBottom()) - fastScroller.getThumbHeight();
    }

    private void updateThumbPosition(CurrentScrollState currentScrollState, int rowCount) {
        int availableScrollHeight;
        int scrolledHeight;

        if (getAdapter() instanceof OnViewHolderHeight) {
            availableScrollHeight = getAvailableScrollHeight(calculateAdapterHeight());
            scrolledHeight = calculateScrollDistanceToPosition(currentScrollState.firstVisibleRowIndex);
        } else {
            availableScrollHeight = getAvailableScrollHeight(rowCount * currentScrollState.currentRowHeight);
            scrolledHeight = currentScrollState.firstVisibleRowIndex * currentScrollState.currentRowHeight;
        }

        // Show the scrollbar if there is height to be scrolled
        if (availableScrollHeight <= 0) {
            fastScroller.setThumbPosition(-1, -1);
            return;
        }

        // Calculate the current scroll position, the scrollY of the recycler view accounts for the
        // view padding, while the scrollBarY is drawn right up to the background padding (ignoring
        // padding)
        int availableScrollBarHeight = getAvailableScrollBarHeight();
        int scrollY = Math.min(availableScrollHeight, getPaddingTop() + scrolledHeight);
        if (isLayoutManagerReversed()) {
            scrollY = scrollY + currentScrollState.firstVisibleRowOffset - availableScrollBarHeight;
        } else {
            scrollY = scrollY - currentScrollState.firstVisibleRowOffset;
        }
        int scrollBarY = (int) (((float) scrollY / (float) availableScrollHeight) * (float) availableScrollBarHeight);
        if (isLayoutManagerReversed()) {
            scrollBarY = availableScrollBarHeight - scrollBarY + getPaddingBottom();
        } else {
            scrollBarY += getPaddingTop();
        }

        // Calculate the position and size of the scroll bar
        int scrollBarX;
        if (AndroidExt.isRtl(getResources())) {
            scrollBarX = 0;
        } else {
            scrollBarX = getWidth() - fastScroller.getWidth();
        }
        fastScroller.setThumbPosition(scrollBarX, scrollBarY);
    }

    @SuppressWarnings("unchecked")
    private int findMeasureAdapterFirstVisiblePosition(int passedHeight) {
        if (getAdapter() instanceof OnViewHolderHeight) {
            OnViewHolderHeight<ViewHolder> onViewHolderHeight = (OnViewHolderHeight<ViewHolder>) getAdapter();
            for (int i = 0; i < getAdapter().getItemCount(); ++i) {
                int top = calculateScrollDistanceToPosition(i);
                int bottom = top + onViewHolderHeight.getViewHolderHeight(this,
                        findViewHolderForAdapterPosition(i), getAdapter().getItemViewType(i));
                if (i == getAdapter().getItemCount() - 1) {
                    if (passedHeight >= top && passedHeight <= bottom) {
                        return i;
                    }
                } else {
                    if (passedHeight >= top && passedHeight < bottom) {
                        return i;
                    }
                }
            }
            int low = calculateScrollDistanceToPosition(0);
            int height = calculateScrollDistanceToPosition(getAdapter().getItemCount() - 1)
                    + onViewHolderHeight.getViewHolderHeight(this,
                    findViewHolderForAdapterPosition(getAdapter().getItemCount() - 1),
                    getAdapter().getItemViewType(getAdapter().getItemCount() - 1));
            throw new IllegalStateException(String.format("Invalid passed height: %d, " +
                    "[low: %d, height: %d]", passedHeight, low, height));
        } else {
            throw new IllegalStateException("findMeasureAdapterFirstVisiblePosition() should " +
                    "only be called where the RecyclerView.Adapter is an instance " +
                    "of OnViewHolderHeight");
        }
    }

    @SuppressWarnings("unchecked")
    private float findItemPosition(float touchFraction) {
        if (getAdapter() instanceof OnViewHolderHeight) {
            OnViewHolderHeight<ViewHolder> measurer = (OnViewHolderHeight<ViewHolder>) getAdapter();
            int viewTop = (int) (touchFraction * calculateAdapterHeight());

            for (int i = 0; i < getAdapter().getItemCount(); ++i) {
                int top = calculateScrollDistanceToPosition(i);
                int bottom = top + measurer.getViewHolderHeight(this,
                        findViewHolderForAdapterPosition(i), getAdapter().getItemViewType(i));
                if (i == getAdapter().getItemCount() - 1) {
                    if (viewTop >= top && viewTop <= bottom) {
                        return i;
                    }
                } else {
                    if (viewTop >= top && viewTop < bottom) {
                        return i;
                    }
                }
            }
        }
        if (getAdapter() != null) {
            return touchFraction * getAdapter().getItemCount();
        }
        return 0;
    }

    private void updateScrollbar() {
        if (getAdapter() == null) {
            return;
        }

        int rowCount = getAdapter().getItemCount();
        if (getLayoutManager() instanceof GridLayoutManager) {
            int spanCount = ((GridLayoutManager) getLayoutManager()).getSpanCount();
            rowCount = (int) Math.ceil((float) rowCount / (float) spanCount);
        }

        if (rowCount == 0) {
            fastScroller.setThumbPosition(-1, -1);
            return;
        }

        updateCurrentScrollState(currentScrollState);
        if (currentScrollState.firstVisibleRowIndex < 0) {
            fastScroller.setThumbPosition(-1, -1);
            return;
        }

        updateThumbPosition(currentScrollState, rowCount);
    }

    private boolean isLayoutManagerReversed() {
        if (getLayoutManager() instanceof LinearLayoutManager) {
            return ((LinearLayoutManager) getLayoutManager()).getReverseLayout();
        }
        return false;
    }

    @SuppressWarnings("unchecked")
    private void updateCurrentScrollState(CurrentScrollState currentScrollState) {
        int itemCount = 0;
        if (getAdapter() != null) {
            itemCount = getAdapter().getItemCount();
        }
        if (itemCount == 0 || getChildCount() == 0) {
            return;
        }

        View child = getChildAt(0);
        currentScrollState.firstVisibleRowIndex = getChildAdapterPosition(child);
        if (getLayoutManager() instanceof GridLayoutManager) {
            currentScrollState.firstVisibleRowIndex = currentScrollState.firstVisibleRowIndex /
                    ((GridLayoutManager) getLayoutManager()).getSpanCount();
        }
        if (getAdapter() instanceof OnViewHolderHeight) {
            if (getLayoutManager() != null) {
                currentScrollState.firstVisibleRowOffset = getLayoutManager().getDecoratedTop(child);
            }
            currentScrollState.currentRowHeight = ((OnViewHolderHeight<ViewHolder>) getAdapter())
                    .getViewHolderHeight(this,
                            findViewHolderForAdapterPosition(currentScrollState.firstVisibleRowIndex),
                            getAdapter().getItemViewType(currentScrollState.firstVisibleRowIndex));
        } else {
            if (getLayoutManager() != null) {
                currentScrollState.firstVisibleRowOffset = getLayoutManager().getDecoratedTop(child);
            }
            currentScrollState.currentRowHeight = child.getHeight() +
                    getLayoutManager().getTopDecorationHeight(child) +
                    getLayoutManager().getBottomDecorationHeight(child);
        }
    }

    @SuppressWarnings("unchecked")
    private int calculateScrollDistanceToPosition(int adapterIndex) {
        if (!(getAdapter() instanceof OnViewHolderHeight)) {
            throw new IllegalStateException("calculateScrollDistanceToPosition() should only be" +
                    " called where the RecyclerView.Adapter is an instance of OnViewHolderHeight");
        }

        if (sparseIntArray.indexOfKey(adapterIndex) >= 0) {
            return sparseIntArray.get(adapterIndex);
        }

        int totalHeight = 0;
        OnViewHolderHeight<ViewHolder> measurer = (OnViewHolderHeight<ViewHolder>) getAdapter();

        for (int i = 0; i < adapterIndex; i++) {
            sparseIntArray.put(i, totalHeight);
            int viewType = getAdapter().getItemViewType(i);
            totalHeight += measurer.getViewHolderHeight(this,
                    findViewHolderForAdapterPosition(i), viewType);
        }

        sparseIntArray.put(adapterIndex, totalHeight);
        return totalHeight;
    }

    private int calculateAdapterHeight() {
        if (!(getAdapter() instanceof OnViewHolderHeight)) {
            throw new IllegalStateException("calculateAdapterHeight() should only be called " +
                    "where the RecyclerView.Adapter is an instance of OnViewHolderHeight");
        }
        return calculateScrollDistanceToPosition(getAdapter().getItemCount());
    }

    @Override
    protected void onFinishInflate() {
        super.onFinishInflate();
        addOnItemTouchListener(this);
    }

    @Override
    public void setAdapter(Adapter adapter) {
        if (getAdapter() != null) {
            getAdapter().unregisterAdapterDataObserver(scrollOffsetInvalidator);
        }
        if (adapter != null) {
            adapter.registerAdapterDataObserver(scrollOffsetInvalidator);
        }
        super.setAdapter(adapter);
    }

    @Override
    public boolean onInterceptTouchEvent(@NonNull RecyclerView rv, @NonNull MotionEvent ev) {
        return handleTouchEvent(ev);
    }

    @Override
    public void onTouchEvent(@NonNull RecyclerView rv, @NonNull MotionEvent ev) {
        handleTouchEvent(ev);
    }

    @Override
    public void draw(Canvas c) {
        super.draw(c);
        if (isFastScrollEnabled) {
            updateScrollbar();
            fastScroller.draw(c);
        }
    }

    @Override
    public void onRequestDisallowInterceptTouchEvent(boolean disallowIntercept) { }

    public interface OnSectionName {
        @NonNull
        String getSectionName(int position);
    }

    public interface OnViewHolderHeight<VH extends ViewHolder> {
        int getViewHolderHeight(RecyclerView recyclerView, @Nullable VH viewHolder, int viewType);
    }

    public interface OnFastScrollStateChangeListener {
        void onFastScrollStart();
        void onFastScrollStop();
    }

    private static class CurrentScrollState {
        int firstVisibleRowIndex = -1;
        int firstVisibleRowOffset = -1;
        int currentRowHeight = -1;
    }

    private class ScrollOffsetInvalidator extends AdapterDataObserver {
        private void invalidateAllScrollOffsets() {
            sparseIntArray.clear();
        }

        @Override
        public void onChanged() {
            invalidateAllScrollOffsets();
        }

        @Override
        public void onItemRangeChanged(int positionStart, int itemCount) {
            invalidateAllScrollOffsets();
        }

        @Override
        public void onItemRangeChanged(int positionStart, int itemCount, Object payload) {
            invalidateAllScrollOffsets();
        }

        @Override
        public void onItemRangeInserted(int positionStart, int itemCount) {
            invalidateAllScrollOffsets();
        }

        @Override
        public void onItemRangeRemoved(int positionStart, int itemCount) {
            invalidateAllScrollOffsets();
        }

        @Override
        public void onItemRangeMoved(int fromPosition, int toPosition, int itemCount) {
            invalidateAllScrollOffsets();
        }
    }

    private static class FastScrollPopup {

        private final Resources resources;
        private final FastScrollRecyclerView fastScrollRecyclerView;

        private final Path backgroundPath;
        private final RectF backgroundRectF;
        private final Rect backgroundRect;
        private final Paint backgroundPaint;

        private final Paint textPaint;
        private final Rect textRect;

        private final Rect invalidateRect;
        private final Rect tempRect;

        private String sectionName;
        private int backgroundColor;
        private int backgroundSize;
        private int cornerRadius;
        private float alpha = 1;

        private ObjectAnimator objectAnimator;
        private boolean isVisible;

        @FastScroller.PopupTextVerticalAlignmentMode private int popupTextVerticalAlignmentMode;
        @FastScroller.PopupPosition private int popupPosition;

        private FastScrollPopup(Resources resources, FastScrollRecyclerView recyclerView) {
            this.resources = resources;
            this.fastScrollRecyclerView = recyclerView;

            this.backgroundPath = new Path();
            this.backgroundRectF = new RectF();
            this.backgroundRect = new Rect();
            this.backgroundPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
            this.backgroundColor = 0xff000000;

            this.invalidateRect = new Rect();
            this.tempRect = new Rect();
            this.textPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
            this.textPaint.setAlpha(0);

            this.textRect = new Rect();

            setTextSize(AndroidExt.convertSpToPx(this.resources, 32));
            setBackgroundSize(AndroidExt.convertDpToPx(this.resources, 62));
        }

        private void setBackgroundColor(int color) {
            this.backgroundColor = color;
            this.backgroundPaint.setColor(color);
            this.fastScrollRecyclerView.invalidate(backgroundRect);
        }

        private void setTextColor(int color) {
            this.textPaint.setColor(color);
            this.fastScrollRecyclerView.invalidate(backgroundRect);
        }

        private void setTextSize(int size) {
            this.textPaint.setTextSize(size);
            this.fastScrollRecyclerView.invalidate(backgroundRect);
        }

        private void setBackgroundSize(int size) {
            this.backgroundSize = size;
            this.cornerRadius = (int) ((float) backgroundSize / 2f);
            this.fastScrollRecyclerView.invalidate(backgroundRect);
        }

        private void setTypeface(Typeface typeface) {
            this.textPaint.setTypeface(typeface);
            this.fastScrollRecyclerView.invalidate(backgroundRect);
        }

        private void animateVisibility(boolean visible) {
            if (isVisible != visible) {
                isVisible = visible;
                if (objectAnimator != null) {
                    objectAnimator.cancel();
                }
                objectAnimator = ObjectAnimator.ofFloat(this, "alpha", visible ? 1f : 0f);
                objectAnimator.setDuration(visible ? 300 : 250);
                objectAnimator.start();
            }
        }

        @Keep
        private void setAlpha(float alpha) {
            this.alpha = alpha;
            this.fastScrollRecyclerView.invalidate(backgroundRect);
        }

        @Keep
        private float getAlpha() {
            return this.alpha;
        }

        private void setPopupTextVerticalAlignmentMode(@FastScroller.PopupTextVerticalAlignmentMode int mode) {
            this.popupTextVerticalAlignmentMode = mode;
        }

        @FastScroller.PopupTextVerticalAlignmentMode
        private int getPopupTextVerticalAlignmentMode() {
            return this.popupTextVerticalAlignmentMode;
        }

        private void setPopupPosition(@FastScroller.PopupPosition int position) {
            this.popupPosition = position;
        }

        @FastScroller.PopupPosition
        private int getPopupPosition() {
            return this.popupPosition;
        }

        private float[] createRadii() {
            if (popupPosition == FastScroller.PopupPosition.CENTER) {
                return new float[]{cornerRadius, cornerRadius, cornerRadius, cornerRadius,
                        cornerRadius, cornerRadius, cornerRadius, cornerRadius};
            }

            if (AndroidExt.isRtl(resources)) {
                return new float[]{cornerRadius, cornerRadius, cornerRadius, cornerRadius,
                        cornerRadius, cornerRadius, 0, 0};
            } else {
                return new float[]{cornerRadius, cornerRadius, cornerRadius, cornerRadius,
                        0, 0, cornerRadius, cornerRadius};
            }
        }

        private void draw(Canvas canvas) {
            if (isVisible()) {
                int restoreCount = canvas.save();
                float[] radii = createRadii();
                canvas.translate(backgroundRect.left, backgroundRect.top);
                tempRect.set(backgroundRect);
                tempRect.offsetTo(0, 0);

                backgroundPath.reset();
                backgroundRectF.set(tempRect);
                backgroundPath.addRoundRect(backgroundRectF, radii, Path.Direction.CW);
                backgroundPaint.setAlpha((int) (Color.alpha(backgroundColor) * alpha));

                float baselinePosition;
                if (popupTextVerticalAlignmentMode == FastScroller.PopupTextVerticalAlignmentMode.FONT_METRICS) {
                    Paint.FontMetrics fontMetrics = textPaint.getFontMetrics();
                    baselinePosition = (backgroundRect.height() - fontMetrics.ascent - fontMetrics.descent) / 2f;
                } else {
                    baselinePosition = (float) (backgroundRect.height() + textRect.height()) / 2f;
                }

                textPaint.setAlpha((int) (alpha * 255));
                canvas.drawPath(backgroundPath, backgroundPaint);
                canvas.drawText(
                        sectionName,
                        (float) (backgroundRect.width() - textRect.width()) / 2f,
                        baselinePosition,
                        textPaint
                );
                canvas.restoreToCount(restoreCount);
            }
        }

        private void setSectionName(String sectionName) {
            if (!sectionName.equals(this.sectionName)) {
                this.sectionName = sectionName;
                textPaint.getTextBounds(sectionName, 0, sectionName.length(), textRect);
                textRect.right = (int) (textRect.left + textPaint.measureText(sectionName));
            }
        }

        private Rect updateFastScrollerBounds(FastScrollRecyclerView recyclerView, int thumbOffsetY) {
            invalidateRect.set(backgroundRect);

            if (isVisible()) {
                // Calculate the dimensions and position of the fast scroller popup
                int edgePadding = recyclerView.getScrollBarWidth();
                int bgPadding = Math.round((float) (backgroundSize - textRect.height()) / 10f) * 5;
                int bgHeight = backgroundSize;
                int bgWidth = Math.max(backgroundSize, textRect.width() + (2 * bgPadding));
                if (popupPosition == FastScroller.PopupPosition.CENTER) {
                    backgroundRect.left = (int) ((float) (recyclerView.getWidth() - bgWidth) / 2f);
                    backgroundRect.right = backgroundRect.left + bgWidth;
                    backgroundRect.top = (int) ((float) (recyclerView.getHeight() - bgHeight) / 2f);
                } else {
                    if (AndroidExt.isRtl(resources)) {
                        backgroundRect.left = (2 * recyclerView.getScrollBarWidth());
                        backgroundRect.right = backgroundRect.left + bgWidth;
                    } else {
                        backgroundRect.right = recyclerView.getWidth() - (2 * recyclerView.getScrollBarWidth());
                        backgroundRect.left = backgroundRect.right - bgWidth;
                    }
                    backgroundRect.top = (int) ((float) recyclerView.getPaddingTop() - (float) recyclerView.getPaddingBottom() +
                            (float) thumbOffsetY - (float) bgHeight + (float) recyclerView.getScrollBarThumbHeight() / 2f);
                    backgroundRect.top = Math.max(recyclerView.getPaddingTop() + edgePadding, Math.min(backgroundRect.top, recyclerView.getPaddingTop() + recyclerView.getHeight() - edgePadding - bgHeight));
                }
                backgroundRect.bottom = backgroundRect.top + bgHeight;
            } else {
                backgroundRect.setEmpty();
            }

            // Combine the old and new fast scroller bounds to create the full invalidate rect
            invalidateRect.union(backgroundRect);
            return invalidateRect;
        }

        private boolean isVisible() {
            return (alpha > 0f) && (!TextUtils.isEmpty(sectionName));
        }
    }

    private static class FastScroller {

        private static final int DEFAULT_AUTO_HIDE_DELAY = 1500;
        private static final int DEFAULT_TOUCH_INSET_IN_DP = -24;
        private static final int DEFAULT_THUMB_COLOR = 0x79000000;
        private static final int DEFAULT_THUMB_INACTIVE_COLOR = 0x79000000;
        private static final int DEFAULT_THUMB_HEIGHT_IN_DP = 52;
        private static final int DEFAULT_THUMB_WIDTH_IN_DP = 8;
        private static final int DEFAULT_TRACK_COLOR = 0x28000000;
        private static final int DEFAULT_TRACK_WIDTH_IN_DP = 8;
        private static final int DEFAULT_POPUP_BACKGROUND_COLOR = 0xff000000;
        private static final int DEFAULT_POPUP_BACKGROUND_SIZE_IN_DP = 62;
        private static final int DEFAULT_POPUP_TEXT_COLOR = 0xffffffff;
        private static final int DEFAULT_POPUP_TEXT_SIZE_IN_SP = 32;

        private final FastScrollRecyclerView fastScrollRecyclerView;
        private final FastScrollPopup fastScrollPopup;

        private final Paint thumbPaint;
        private int thumbHeight;
        private int thumbWidth;

        private final Paint trackPaint;
        private int trackWidth;

        private final RectF fastScrollerRectF;
        private final Rect invalidateRect;
        private final Rect tempInvalidateRect;
        private final Rect tempRect;

        private int touchInset;

        private int touchOffset;

        private final Point thumbPositionPoint;
        private final Point offsetPoint;

        private boolean isDragging;

        private Animator autoHideAnimator;
        private boolean isAnimating;
        private int autoHideDelay;
        private boolean isAutoHideEnabled;
        private final Runnable hideRunnable;

        private int thumbActiveColor;
        private int thumbInactiveColor;
        private boolean isThumbInactiveEnabled;

        @Retention(SOURCE)
        @IntDef({PopupTextVerticalAlignmentMode.TEXT_BOUNDS, PopupTextVerticalAlignmentMode.FONT_METRICS})
        private @interface PopupTextVerticalAlignmentMode {
            int TEXT_BOUNDS = 0;
            int FONT_METRICS = 1;
        }
        @IntDef({PopupPosition.ADJACENT, PopupPosition.CENTER})
        private @interface PopupPosition {
            int ADJACENT = 0;
            int CENTER = 1;
        }

        private FastScroller(Context context, FastScrollRecyclerView recyclerView, AttributeSet attrs) {
            Resources resources = context.getResources();
            this.fastScrollRecyclerView = recyclerView;
            this.fastScrollPopup = new FastScrollPopup(resources, recyclerView);

            this.thumbPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
            this.trackPaint = new Paint(Paint.ANTI_ALIAS_FLAG);

            this.fastScrollerRectF = new RectF();
            this.invalidateRect = new Rect();
            this.tempInvalidateRect = new Rect();
            this.tempRect = new Rect();

            this.thumbPositionPoint = new Point(-1, -1);
            this.offsetPoint = new Point(0, 0);

            TypedArray typedArray = context.getTheme().obtainStyledAttributes(
                    attrs, R.styleable.FastScrollRecyclerView, 0, 0);
            try {
                this.isAutoHideEnabled = typedArray
                        .getBoolean(R.styleable.FastScrollRecyclerView_fastScrollAutoHide,
                                true);
                this.autoHideDelay = typedArray
                        .getInteger(R.styleable.FastScrollRecyclerView_fastScrollAutoHideDelay,
                                DEFAULT_AUTO_HIDE_DELAY);
                this.isThumbInactiveEnabled = typedArray
                        .getBoolean(R.styleable.FastScrollRecyclerView_fastScrollEnableThumbInactiveColor,
                                true);
                this.thumbActiveColor = typedArray
                        .getColor(R.styleable.FastScrollRecyclerView_fastScrollThumbActiveColor,
                                DEFAULT_THUMB_COLOR);
                this.thumbInactiveColor = typedArray
                        .getColor(R.styleable.FastScrollRecyclerView_fastScrollThumbInactiveColor,
                                DEFAULT_THUMB_INACTIVE_COLOR);
                this.touchInset = typedArray
                        .getDimensionPixelSize(R.styleable.FastScrollRecyclerView_fastScrollTouchInset,
                                AndroidExt.convertDpToPx(resources, DEFAULT_TOUCH_INSET_IN_DP));
                this.thumbHeight = typedArray
                        .getDimensionPixelSize(R.styleable.FastScrollRecyclerView_fastScrollThumbHeight,
                                AndroidExt.convertDpToPx(resources, DEFAULT_THUMB_HEIGHT_IN_DP));
                this.thumbWidth = typedArray
                        .getDimensionPixelSize(R.styleable.FastScrollRecyclerView_fastScrollThumbWidth,
                                AndroidExt.convertDpToPx(resources, DEFAULT_THUMB_WIDTH_IN_DP));
                this.trackWidth = typedArray
                        .getDimensionPixelSize(R.styleable.FastScrollRecyclerView_fastScrollTrackWidth,
                                AndroidExt.convertDpToPx(resources, DEFAULT_TRACK_WIDTH_IN_DP));

                int trackColor = typedArray
                        .getColor(R.styleable.FastScrollRecyclerView_fastScrollTrackBackgroundColor,
                                DEFAULT_TRACK_COLOR);
                int popupBgColor = typedArray
                        .getColor(R.styleable.FastScrollRecyclerView_fastScrollPopupBackgroundColor,
                                DEFAULT_POPUP_BACKGROUND_COLOR);
                int popupTextColor = typedArray
                        .getColor(R.styleable.FastScrollRecyclerView_fastScrollPopupTextColor,
                                DEFAULT_POPUP_TEXT_COLOR);
                int popupTextSize = typedArray
                        .getDimensionPixelSize(R.styleable.FastScrollRecyclerView_fastScrollPopupTextSize,
                                AndroidExt.convertSpToPx(resources, DEFAULT_POPUP_TEXT_SIZE_IN_SP));
                int popupBackgroundSize = typedArray
                        .getDimensionPixelSize(R.styleable.FastScrollRecyclerView_fastScrollPopupBackgroundSize,
                                AndroidExt.convertDpToPx(resources, DEFAULT_POPUP_BACKGROUND_SIZE_IN_DP));
                @PopupTextVerticalAlignmentMode int popupTextVerticalAlignmentMode = typedArray
                        .getInteger(R.styleable.FastScrollRecyclerView_fastScrollPopupTextVerticalAlignmentMode,
                                PopupTextVerticalAlignmentMode.FONT_METRICS);
                @PopupPosition int popupPosition = typedArray
                        .getInteger(R.styleable.FastScrollRecyclerView_fastScrollPopupPosition,
                                PopupPosition.ADJACENT);

                this.trackPaint.setColor(trackColor);
                this.thumbPaint.setColor(isThumbInactiveEnabled ? thumbInactiveColor : thumbActiveColor);
                this.fastScrollPopup.setBackgroundColor(popupBgColor);
                this.fastScrollPopup.setTextColor(popupTextColor);
                this.fastScrollPopup.setTextSize(popupTextSize);
                this.fastScrollPopup.setBackgroundSize(popupBackgroundSize);
                this.fastScrollPopup.setPopupTextVerticalAlignmentMode(popupTextVerticalAlignmentMode);
                this.fastScrollPopup.setPopupPosition(popupPosition);
            } finally {
                typedArray.recycle();
            }

            this.hideRunnable = new Runnable() {
                @Override
                public void run() {
                    if (!isDragging) {
                        if (autoHideAnimator != null) {
                            autoHideAnimator.cancel();
                        }
                        autoHideAnimator = ObjectAnimator.ofInt(FastScroller.this,
                                "offsetX",
                                (AndroidExt.isRtl(fastScrollRecyclerView
                                        .getResources()) ? -1 : 1) * getWidth());
                        autoHideAnimator.setInterpolator(new FastOutLinearInInterpolator());
                        autoHideAnimator.setDuration(300);
                        autoHideAnimator.start();
                    }
                }
            };

            this.fastScrollRecyclerView.addOnScrollListener(new OnScrollListener() {
                @Override
                public void onScrolled(@NonNull RecyclerView recyclerView, int dx, int dy) {
                    super.onScrolled(recyclerView, dx, dy);
                    if (!fastScrollRecyclerView.isInEditMode()) {
                        show();
                    }
                }
            });

            if (this.isAutoHideEnabled) {
                postAutoHideDelayed();
            }
        }

        private int getTouchInset() {
            return this.touchInset;
        }

        private int getThumbWidth() {
            return this.thumbWidth;
        }

        private int getThumbHeight() {
            return this.thumbHeight;
        }

        private int getWidth() {
            return Math.max(this.trackWidth, this.thumbWidth);
        }

        private int getTrackWidth() {
            return this.trackWidth;
        }

        private boolean isDragging() {
            return this.isDragging;
        }

        private void handleTouchEvent(MotionEvent motionEvent, int currentPositionX,
                                      int currentPositionY, int lastKnownPositionY,
                                      OnFastScrollStateChangeListener stateChangeListener) {
            int action = motionEvent.getAction();
            int y = (int) motionEvent.getY();
            switch (action) {
                case MotionEvent.ACTION_DOWN:
                    if (isNearPoint(currentPositionX, currentPositionY)) {
                        this.touchOffset = currentPositionY - this.thumbPositionPoint.y;
                    }
                    break;
                case MotionEvent.ACTION_MOVE:
                    if (!this.isDragging && isNearPoint(currentPositionX, currentPositionY)) {
                        this.fastScrollRecyclerView.getParent().requestDisallowInterceptTouchEvent(true);
                        this.isDragging = true;
                        this.touchOffset += (lastKnownPositionY - currentPositionY);
                        this.fastScrollPopup.animateVisibility(true);
                        if (stateChangeListener != null) {
                            stateChangeListener.onFastScrollStart();
                        }
                        if (this.isThumbInactiveEnabled) {
                            this.thumbPaint.setColor(thumbActiveColor);
                        }
                    }
                    if (this.isDragging) {
                        boolean layoutManagerReversed = this.fastScrollRecyclerView.isLayoutManagerReversed();
                        int bottom = this.fastScrollRecyclerView.getHeight() - this.thumbHeight;
                        float boundedY = (float) Math.max(0, Math.min(bottom, y - this.touchOffset));

                        float touchFraction = boundedY / (float) bottom;
                        if (layoutManagerReversed) {
                            touchFraction = 1 - touchFraction;
                        }

                        String sectionName = this.fastScrollRecyclerView.scrollToPositionAtProgress(touchFraction);
                        this.fastScrollPopup.setSectionName(sectionName);
                        this.fastScrollPopup.animateVisibility(!sectionName.isEmpty());
                        this.fastScrollRecyclerView.invalidate(this.fastScrollPopup
                                .updateFastScrollerBounds(this.fastScrollRecyclerView, this.thumbPositionPoint.y));
                    }
                    break;
                case MotionEvent.ACTION_UP:
                case MotionEvent.ACTION_CANCEL:
                    this.touchOffset = 0;
                    if (this.isDragging) {
                        this.isDragging = false;
                        this.fastScrollPopup.animateVisibility(false);
                        if (stateChangeListener != null) {
                            stateChangeListener.onFastScrollStop();
                        }
                    }
                    if (this.isThumbInactiveEnabled) {
                        this.thumbPaint.setColor(this.thumbInactiveColor);
                    }
                    break;
            }
        }

        private void draw(Canvas canvas) {
            if (this.thumbPositionPoint.x < 0 || this.thumbPositionPoint.y < 0) {
                return;
            }
            this.fastScrollerRectF
                    .set(this.thumbPositionPoint.x + this.offsetPoint.x + (this.thumbWidth -
                                    this.trackWidth),
                            this.offsetPoint.y + this.fastScrollRecyclerView.getPaddingTop(),
                            this.thumbPositionPoint.x + this.offsetPoint.x + this.trackWidth +
                                    (this.thumbWidth - this.trackWidth),
                            this.fastScrollRecyclerView.getHeight() + this.offsetPoint.y -
                                    this.fastScrollRecyclerView.getPaddingBottom());

            canvas.drawRoundRect(this.fastScrollerRectF, this.trackWidth, this.trackWidth,
                    this.trackPaint);

            this.fastScrollerRectF.set((float) this.thumbPositionPoint.x + (float) this.offsetPoint.x +
                            (float) (this.thumbWidth - this.trackWidth) / 2.0f,
                    (float) this.thumbPositionPoint.y + (float) this.offsetPoint.y,
                    (float) this.thumbPositionPoint.x + (float) this.offsetPoint.x + (float) this.thumbWidth +
                            (float) (this.thumbWidth - this.trackWidth) / 2.0f,
                    (float) this.thumbPositionPoint.y + (float) this.offsetPoint.y + (float) this.thumbHeight);

            canvas.drawRoundRect(this.fastScrollerRectF, (float) this.thumbWidth, (float) this.thumbWidth,
                    this.thumbPaint);

            this.fastScrollPopup.draw(canvas);
        }

        private boolean isNearPoint(int x, int y) {
            this.tempRect.set(this.thumbPositionPoint.x, this.thumbPositionPoint.y,
                    this.thumbPositionPoint.x + this.trackWidth,
                    this.thumbPositionPoint.y + this.thumbHeight);
            this.tempRect.inset(this.touchInset, this.touchInset);
            return this.tempRect.contains(x, y);
        }

        private void setThumbPosition(int x, int y) {
            if (this.thumbPositionPoint.x == x && this.thumbPositionPoint.y == y) {
                return;
            }
            this.invalidateRect.set(this.thumbPositionPoint.x + this.offsetPoint.x, this.offsetPoint.y,
                    this.thumbPositionPoint.x + this.offsetPoint.x + this.trackWidth,
                    this.fastScrollRecyclerView.getHeight() + this.offsetPoint.y);
            this.thumbPositionPoint.set(x, y);
            this.tempInvalidateRect.set(this.thumbPositionPoint.x + this.offsetPoint.x,
                    this.offsetPoint.y,
                    this.thumbPositionPoint.x + this.offsetPoint.x + this.trackWidth,
                    this.fastScrollRecyclerView.getHeight() + this.offsetPoint.y);
            this.invalidateRect.union(this.tempInvalidateRect);
            this.fastScrollRecyclerView.invalidate(this.invalidateRect);
        }

        private void setOffset(int x, int y) {
            if (this.offsetPoint.x == x && this.offsetPoint.y == y) {
                return;
            }
            this.invalidateRect
                    .set(this.thumbPositionPoint.x + this.offsetPoint.x,
                            this.offsetPoint.y,
                            this.thumbPositionPoint.x + this.offsetPoint.x + this.trackWidth,
                            this.fastScrollRecyclerView.getHeight() + this.offsetPoint.y);
            this.offsetPoint.set(x, y);
            this.tempInvalidateRect.set(this.thumbPositionPoint.x + this.offsetPoint.x,
                    this.offsetPoint.y,
                    this.thumbPositionPoint.x + this.offsetPoint.x + this.trackWidth,
                    this.fastScrollRecyclerView.getHeight() + this.offsetPoint.y);
            this.invalidateRect.union(this.tempInvalidateRect);
            this.fastScrollRecyclerView.invalidate(this.invalidateRect);
        }

        @Keep
        private void setOffsetX(int x) {
            setOffset(x, this.offsetPoint.y);
        }

        @Keep
        private int getOffsetX() {
            return this.offsetPoint.x;
        }

        private void show() {
            if (!this.isAnimating) {
                if (this.autoHideAnimator != null) {
                    this.autoHideAnimator.cancel();
                }
                this.autoHideAnimator = ObjectAnimator.ofInt(this, "offsetX", 0);
                this.autoHideAnimator.setInterpolator(new LinearOutSlowInInterpolator());
                this.autoHideAnimator.setDuration(300);
                this.autoHideAnimator.addListener(new AnimatorListenerAdapter() {
                    @Override
                    public void onAnimationCancel(Animator animation) {
                        super.onAnimationCancel(animation);
                        isAnimating = false;
                    }

                    @Override
                    public void onAnimationEnd(Animator animation) {
                        super.onAnimationEnd(animation);
                        isAnimating = false;
                    }
                });
                this.isAnimating = true;
                this.autoHideAnimator.start();
            }
            if (this.isAutoHideEnabled) {
                postAutoHideDelayed();
            } else {
                cancelAutoHide();
            }
        }

        private void postAutoHideDelayed() {
            if (this.fastScrollRecyclerView != null) {
                cancelAutoHide();
                this.fastScrollRecyclerView.postDelayed(hideRunnable, autoHideDelay);
            }
        }

        private void cancelAutoHide() {
            if (fastScrollRecyclerView != null) {
                fastScrollRecyclerView.removeCallbacks(hideRunnable);
            }
        }

        private void setTouchInset(int inset) {
            this.touchInset = inset;
        }

        private void setThumbColor(@ColorInt int color) {
            this.thumbActiveColor = color;
            this.thumbPaint.setColor(color);
            this.fastScrollRecyclerView.invalidate(this.invalidateRect);
        }

        private void setThumbWidth(int width) {
            this.thumbWidth = width;
        }

        private void setThumbHeight(int height) {
            this.thumbHeight = height;
        }

        private void setTrackColor(@ColorInt int color) {
            this.trackPaint.setColor(color);
            this.fastScrollRecyclerView.invalidate(invalidateRect);
        }

        private void setTrackWidth(int width) {
            this.trackWidth = width;
        }

        private void setPopupBgColor(@ColorInt int color) {
            this.fastScrollPopup.setBackgroundColor(color);
        }

        private void setPopupTextColor(@ColorInt int color) {
            this.fastScrollPopup.setTextColor(color);
        }

        private void setPopupTypeface(Typeface typeface) {
            this.fastScrollPopup.setTypeface(typeface);
        }

        private void setPopupTextSize(int size) {
            this.fastScrollPopup.setTextSize(size);
        }

        private void setAutoHideDelay(int hideDelay) {
            this.autoHideDelay = hideDelay;
            if (this.isAutoHideEnabled) {
                postAutoHideDelayed();
            }
        }

        private void setAutoHideEnabled(boolean autoHideEnabled) {
            this.isAutoHideEnabled = autoHideEnabled;
            if (autoHideEnabled) {
                postAutoHideDelayed();
            } else {
                cancelAutoHide();
            }
        }

        private void setPopupPosition(@PopupPosition int popupPosition) {
            this.fastScrollPopup.setPopupPosition(popupPosition);
        }

        private void setThumbInactiveColor(@ColorInt int color) {
            this.thumbInactiveColor = color;
            enableThumbInactiveColor(true);
        }

        private void enableThumbInactiveColor(boolean enableInactiveColor) {
            this.isThumbInactiveEnabled = enableInactiveColor;
            this.thumbPaint.setColor(isThumbInactiveEnabled ? thumbInactiveColor : thumbActiveColor);
        }
    }

    private static class AndroidExt {
        public static boolean isRtl(Resources res) {
            return res.getConfiguration().getLayoutDirection() == View.LAYOUT_DIRECTION_RTL;
        }

        public static int convertDpToPx(Resources res, float dp) {
            return (int) (dp * res.getDisplayMetrics().density);
        }

        public static int convertSpToPx(Resources res, float sp) {
            return (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_SP, sp, res.getDisplayMetrics());
        }
    }
}