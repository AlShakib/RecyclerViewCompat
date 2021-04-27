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

package dev.alshakib.rvcompat.viewholder;

import android.content.Context;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.recyclerview.widget.RecyclerView;

public abstract class ViewHolderCompat extends RecyclerView.ViewHolder
        implements View.OnClickListener, View.OnLongClickListener {

    private OnItemClickListener onItemClickListener;
    private OnItemLongClickListener onItemLongClickListener;

    private final View view;

    public ViewHolderCompat(@NonNull View view) {
        super(view);
        this.view = view;
    }

    @NonNull
    public View getView() {
        return view;
    }

    @NonNull
    public Context getContext() {
        return view.getContext();
    }

    @Nullable
    public OnItemClickListener getOnItemClickListener() {
        return onItemClickListener;
    }

    public void setOnItemClickListener(@Nullable OnItemClickListener onItemClickListener) {
        this.onItemClickListener = onItemClickListener;
    }

    @Nullable
    public OnItemLongClickListener getOnItemLongClickListener() {
        return onItemLongClickListener;
    }

    public void setOnItemLongClickListener(@Nullable OnItemLongClickListener onItemLongClickListener) {
        this.onItemLongClickListener = onItemLongClickListener;
    }

    @Override
    public void onClick(View v) {
        if (getOnItemClickListener() != null) {
            getOnItemClickListener().onItemClick(this, v, getItemViewType(), getLayoutPosition());
        }
    }

    @Override
    public boolean onLongClick(View v) {
        if (getOnItemLongClickListener() != null) {
            return getOnItemLongClickListener().onItemLongClick(this, v, getItemViewType(), getLayoutPosition());
        }
        return false;
    }

    public interface OnItemClickListener {
        void onItemClick(@NonNull ViewHolderCompat viewHolderCompat, @NonNull View v, int viewType, int position);
    }

    public interface OnItemLongClickListener {
        boolean onItemLongClick(@NonNull ViewHolderCompat viewHolderCompat, @NonNull View v, int viewType, int position);
    }
}
