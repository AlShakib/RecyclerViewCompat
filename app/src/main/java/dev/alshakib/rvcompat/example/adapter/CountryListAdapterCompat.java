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

package dev.alshakib.rvcompat.example.adapter;

import android.graphics.drawable.Drawable;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.AsyncDifferConfig;

import dev.alshakib.dtext.DText;
import dev.alshakib.rvcompat.adapter.ListAdapterCompat;
import dev.alshakib.rvcompat.example.R;
import dev.alshakib.rvcompat.example.data.model.Country;
import dev.alshakib.rvcompat.example.diff.CountryDiffUtilItemCallback;
import dev.alshakib.rvcompat.example.viewholder.ListOneLineWithImageViewHolderCompat;
import dev.alshakib.rvcompat.view.FastScrollRecyclerView;

public class CountryListAdapterCompat extends ListAdapterCompat<Country, ListOneLineWithImageViewHolderCompat>
        implements FastScrollRecyclerView.OnSectionName {

    public CountryListAdapterCompat() {
        super(new AsyncDifferConfig.Builder<>(new CountryDiffUtilItemCallback()).build());
    }

    @NonNull
    @Override
    public ListOneLineWithImageViewHolderCompat onCreateViewHolderCompat(@NonNull ViewGroup parent, int viewType) {
        return new ListOneLineWithImageViewHolderCompat(inflateView(R.layout.list_one_line_with_image, parent, false));
    }

    @Override
    public void onBindViewHolderCompat(@NonNull ListOneLineWithImageViewHolderCompat holder, int position) {
        if (getItem(position) != null) {
            holder.getViewBinding().titleTextView.setText(getItem(position).getName());
            holder.getViewBinding().thumbnailImageView.setImageDrawable(createDrawable(getItem(position).getName()));
        }
    }

    private Drawable createDrawable(String s) {
        return new DText.Builder()
                .setText(s)
                .boldText()
                .randomBackgroundColor()
                .firstCharOnly()
                .alphaNumOnly()
                .toUpperCase()
                .drawAsRound()
                .build();
    }

    @NonNull
    @Override
    public String getSectionName(int position) {
        if (getItem(position) != null) {
            return String.valueOf(getItem(position).getName().toUpperCase().charAt(0));
        }
        return "";
    }
}
