/*
 * MIT License
 *
 * Copyright (c) 2020 Al Shakib (shakib@alshakib.dev)
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

package dev.alshakib.rvcompat.adapter;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import dev.alshakib.rvcompat.viewholder.ViewHolderCompat;

interface AdapterCompat<VH> {

    @NonNull
    LayoutInflater getLayoutInflater();
    @NonNull
    Context getContext();

    @NonNull
    VH onCreateViewHolderCompat(@NonNull ViewGroup parent, int viewType);
    void onBindViewHolderCompat(@NonNull VH holder, int position);

    @Nullable
    ViewHolderCompat.OnItemClickListener getOnItemClickListener();
    void setOnItemClickListener(@Nullable ViewHolderCompat.OnItemClickListener onItemClickListener);

    @Nullable
    ViewHolderCompat.OnItemLongClickListener getOnItemLongClickListener();
    void setOnItemLongClickListener(@Nullable ViewHolderCompat.OnItemLongClickListener onItemLongClickListener);
}
