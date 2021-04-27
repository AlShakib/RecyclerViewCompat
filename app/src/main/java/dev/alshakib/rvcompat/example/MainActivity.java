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

package dev.alshakib.rvcompat.example;

import android.os.Bundle;
import android.view.View;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;

import java.util.ArrayList;
import java.util.List;

import dev.alshakib.rvcompat.example.adapter.CountryListAdapterCompat;
import dev.alshakib.rvcompat.example.data.model.Country;
import dev.alshakib.rvcompat.example.databinding.ActivityMainBinding;
import dev.alshakib.rvcompat.viewholder.ViewHolderCompat;

public class MainActivity extends AppCompatActivity
        implements ViewHolderCompat.OnItemClickListener, ViewHolderCompat.OnItemLongClickListener {
    private static final String LOG_TAG = MainActivity.class.getSimpleName();

    private ActivityMainBinding viewBinding;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        viewBinding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(viewBinding.getRoot());
        setSupportActionBar(viewBinding.materialToolbar);

        viewBinding.recyclerView.setHasFixedSize(true);
        viewBinding.recyclerView.setLayoutManager(new LinearLayoutManager(this));

        CountryListAdapterCompat countryListAdapterCompat = new CountryListAdapterCompat();
        viewBinding.recyclerView.setAdapter(countryListAdapterCompat);

        countryListAdapterCompat.submitList(fetchData());
        countryListAdapterCompat.setOnItemClickListener(this);
        countryListAdapterCompat.setOnItemLongClickListener(this);
    }

    private List<Country> fetchData() {
        List<Country> countryList = new ArrayList<>();
        String[] dataSet = getResources().getStringArray(R.array.countries_array);

        for (int i = 0; i < dataSet.length; ++i) {
            countryList.add(new Country(i, dataSet[i]));
        }
        return countryList;
    }

    @Override
    public void onItemClick(@NonNull ViewHolderCompat viewHolderCompat, @NonNull View v, int viewType, int position) {
        if (v.getId() == R.id.more_options_button) {
            Toast.makeText(this, "More options clicked; Position: " + position, Toast.LENGTH_SHORT).show();
        } else {
            Toast.makeText(this, "Item clicked; Position: " + position, Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    public boolean onItemLongClick(@NonNull ViewHolderCompat viewHolderCompat, @NonNull View v, int viewType, int position) {
        Toast.makeText(this, "Item long clicked; Position: " + position, Toast.LENGTH_SHORT).show();
        return true;
    }
}
