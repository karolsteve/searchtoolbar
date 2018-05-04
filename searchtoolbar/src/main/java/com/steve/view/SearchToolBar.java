/*
 * Copyright 2018 Steve Tchatchouang
 *
 *    Licensed under the Apache License, Version 2.0 (the "License");
 *    you may not use this file except in compliance with the License.
 *    You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *    Unless required by applicable law or agreed to in writing, software
 *    distributed under the License is distributed on an "AS IS" BASIS,
 *    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *    See the License for the specific language governing permissions and
 *    limitations under the License.
 */

package com.steve.view;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.annotation.SuppressLint;
import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Color;
import android.os.Build;
import android.support.annotation.Nullable;
import android.support.annotation.RequiresApi;
import android.support.v4.content.ContextCompat;
import android.support.v7.widget.SearchView;
import android.support.v7.widget.Toolbar;
import android.util.AttributeSet;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewAnimationUtils;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import java.lang.reflect.Field;

/**
 * Created by Steve Tchatchouang on 28/12/2017
 */

public class SearchToolBar extends LinearLayout {

    private static final String TAG = SearchToolBar.class.getSimpleName();

    private MenuItem              itemSearch;
    private Toolbar               toolbar;
    private int                   mPositionFromRight;
    private SearchToolBarListener mListener;

    public SearchToolBar(Context context) {
        this(context, null);
    }

    public SearchToolBar(Context context, @Nullable AttributeSet attrs) {
        this(context, attrs, 0);
    }

    @Override
    public boolean hasOverlappingRendering() {
        return false;
    }

    public SearchToolBar(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        TypedArray a = getContext().obtainStyledAttributes(attrs, R.styleable.SearchToolBar);
        mPositionFromRight = a.getInteger(R.styleable.SearchToolBar_search_position, 0);
        a.recycle();
        inflate(getContext(), R.layout.toolbar_search_view, this);
        toolbar = findViewById(R.id.toolbar_search);
        toolbar.inflateMenu(R.menu.toolbar_search_menu);
        Menu searchMenu = toolbar.getMenu();
        itemSearch = searchMenu.findItem(R.id.action_toolbar_search);
        itemSearch.setOnActionExpandListener(new MenuItem.OnActionExpandListener() {
            @Override
            public boolean onMenuItemActionExpand(MenuItem item) {
                if (toolbar.getVisibility() != View.VISIBLE) {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                        createToolbarSearchReveal(true);
                    } else {
                        toolbar.setVisibility(View.VISIBLE);
                    }
                    return mListener.onMenuItemActionExpand(item);
                }
                return true;
            }

            @Override
            public boolean onMenuItemActionCollapse(MenuItem item) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                    createToolbarSearchReveal(false);
                } else {
                    toolbar.setVisibility(View.INVISIBLE);
                }
                return mListener.onMenuItemActionCollapse(item);
            }
        });
        toolbar.setNavigationOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                itemSearch.collapseActionView();
            }
        });
        setUpSearchView(itemSearch);
    }

    public void expand() {
        itemSearch.expandActionView();
    }

    @SuppressWarnings("unused")
    public boolean isExpanded() {
        return itemSearch.isActionViewExpanded();
    }

    public void collapse() {
        itemSearch.collapseActionView();
    }

    private void setUpSearchView(MenuItem itemSearch) {
        final SearchView searchView = (SearchView) itemSearch.getActionView();
        View v = searchView.findViewById(android.support.v7.appcompat.R.id.search_plate);
        v.setBackgroundColor(Color.TRANSPARENT);
        searchView.setSubmitButtonEnabled(false);
        ImageView closeBtn = searchView.findViewById(R.id.search_close_btn);
        closeBtn.setImageDrawable(ContextCompat.getDrawable(getContext(), R.drawable.ic_close_grey_700_24dp));

        //search
        EditText editText = searchView.findViewById(R.id.search_src_text);
        editText.setHint(R.string.action_search);
        editText.setHintTextColor(ContextCompat.getColor(getContext(), R.color.grey_400));
        editText.setTextColor(ContextCompat.getColor(getContext(), R.color.grey_700));
        editText.setOnFocusChangeListener(new OnFocusChangeListener() {
            @Override
            public void onFocusChange(View v, boolean hasFocus) {
                if (!hasFocus) collapse();
            }
        });

        //cursor
        try {
            Field mCursorDrawableRes = TextView.class.getDeclaredField("mCursorDrawableRes");
            mCursorDrawableRes.setAccessible(true);
            mCursorDrawableRes.set(editText, R.drawable.toolbar_search_cursor);
        } catch (Exception e) {
            Log.e(TAG, "setUpSearchView: ", e);
        }

        searchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextSubmit(String query) {
                if (mListener == null) throw new RuntimeException("Null listener for searchView");
                mListener.onQueryTextChange(query);
                return true;
            }

            @Override
            public boolean onQueryTextChange(String newText) {
                if (mListener == null) throw new RuntimeException("Null listener for searchView");
                mListener.onQueryTextChange(newText);
                return true;
            }
        });
    }

    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    public void createToolbarSearchReveal(boolean show) {
        int width = toolbar.getWidth();
        int height = toolbar.getHeight();
        @SuppressLint("PrivateResource")
        int b = toolbar.getContext().getResources().getDimensionPixelSize(R.dimen.abc_action_button_min_width_material);
        //position of search icon
        if (mPositionFromRight > 0) {
            width -= mPositionFromRight * b;
        }
        //center of search icon
        width -= b / 2;

        //clipped circle coordinates
        int cx = width;
        int cy = height / 2;

        final Animator animator = ViewAnimationUtils.createCircularReveal(
                toolbar, cx, cy, show ? 0 : (float) width, show ? (float) width : 0
        );
        animator.setDuration(240);
        if (!show) {
            animator.addListener(new AnimatorListenerAdapter() {
                @Override
                public void onAnimationEnd(Animator animation) {
                    toolbar.setVisibility(View.INVISIBLE);
                    animator.removeListener(this);
                }
            });
        } else {
            toolbar.setVisibility(View.VISIBLE);
        }
        animator.start();
    }

    public void setSearchToolbarListener(SearchToolBarListener mListener) {
        this.mListener = mListener;
    }

    public interface SearchToolBarListener {

        void onQueryTextChange(String query);

        boolean onMenuItemActionExpand(MenuItem item);

        boolean onMenuItemActionCollapse(MenuItem item);
    }
}
