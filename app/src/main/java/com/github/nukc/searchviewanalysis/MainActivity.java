package com.github.nukc.searchviewanalysis;

import android.app.SearchManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.database.AbstractCursor;
import android.database.Cursor;
import android.graphics.Color;
import android.os.Bundle;
import android.support.v4.view.MenuItemCompat;
import android.support.v4.widget.SimpleCursorAdapter;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.SearchView;
import android.text.TextUtils;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.TextView;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.Locale;

public class MainActivity extends AppCompatActivity {
    private static final String TAG = MainActivity.class.getSimpleName();

    private SearchView mSearchView;
    private SearchView.SearchAutoComplete mSearchSrcTextView;
    private TextView mTextView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

//        SearchViewCompat
//        android.widget.SearchView

        mTextView = (TextView) findViewById(R.id.text);
        mSearchView = (SearchView) findViewById(R.id.search_view);
        setupSearchView();
//        setupByField();
        setupByFindView();

    }

    private void setupSearchView() {
//        mSearchView.setIconifiedByDefault(false);  //如果设置false,SearchView会一直处于展开状态
//        mSearchView.setIconified(false);  //设置为false，SearchView会展开，反之会缩成1个Icon
        mSearchView.setQueryHint(getString(R.string.hint));  //设置提示文字

        mSearchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextSubmit(String query) {
                //当用户提交查询的时候会调用
                Log.d(TAG, "onQueryTextSubmit - query = " + query);
                return false; //返回false，SearchView会处理一些操作，反之SearchView不会做任何处理
            }

            @Override
            public boolean onQueryTextChange(String newText) {
                //当查询文字改变的时候会调用
                Log.d(TAG, "onQueryTextChange - newText = " + newText);
                return false;
            }
        });

//        SearchManager searchManager = (SearchManager) getSystemService(SEARCH_SERVICE);
//        ComponentName componentName = new ComponentName(this, SearchResultActivity.class);
//        mSearchView.setSearchableInfo(searchManager.getSearchableInfo(componentName));

        mSearchView.setSuggestionsAdapter(new SearchSuggestionsAdapter(this));
        mSearchView.setOnSuggestionListener(new SearchView.OnSuggestionListener() {
            @Override
            public boolean onSuggestionSelect(int position) {
                Log.d(TAG, "onSuggestionSelect - position = " + position);
                return false;
            }

            @Override
            public boolean onSuggestionClick(int position) {
                Log.d(TAG, "onSuggestionClick - position = " + position);
                return false;
            }
        });

        //mSearchView.setQuery("", true);
    }

    private void setupByField() {
        try {
            Field field = mSearchView.getClass().getDeclaredField("mSearchSrcTextView");
            field.setAccessible(true);

            //通过反射拿到SearchView里面的SearchAutoComplete组件
            mSearchSrcTextView = (SearchView.SearchAutoComplete) field.get(mSearchView);
            //设置提示文字的颜色
            mSearchSrcTextView.setHintTextColor(Color.BLUE);
        } catch (NoSuchFieldException e) {
            e.printStackTrace();
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        }
    }

    private void setupByFindView() {
        mSearchSrcTextView = (SearchView.SearchAutoComplete)
                mSearchView.findViewById(android.support.v7.appcompat.R.id.search_src_text);
        mSearchSrcTextView.setHintTextColor(Color.RED);
    }

    @Override
    protected void onNewIntent(Intent intent) {
        if (Intent.ACTION_SEARCH.equals(intent.getAction())) {
            String query = intent.getStringExtra(SearchManager.QUERY);
            mTextView.setText(query);
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.main, menu);

        SearchManager searchManager =
                (SearchManager) getSystemService(Context.SEARCH_SERVICE);

        MenuItem searchMenuItem = menu.findItem(R.id.search);
        SearchView searchView =
                (SearchView) MenuItemCompat.getActionView(searchMenuItem);

        ComponentName componentName = new ComponentName(this, SearchResultActivity.class);
        searchView.setSearchableInfo(searchManager.getSearchableInfo(getComponentName()));
        return true;
    }

    //https://gist.github.com/slightfoot/5514856
    private static class SearchSuggestionsAdapter extends SimpleCursorAdapter {

        private static final String[] mFields = {"_id", "result"};
        private static final String[] mVisible = {"result"};
        private static final int[] mViewIds = {android.R.id.text1};


        public SearchSuggestionsAdapter(Context context) {
            super(context, android.R.layout.simple_list_item_1, null, mVisible, mViewIds, 0);
        }

        @Override
        public Cursor runQueryOnBackgroundThread(CharSequence constraint) {
            return new SuggestionsCursor(constraint);
        }

        private static class SuggestionsCursor extends AbstractCursor {
            private ArrayList<String> mResults;

            public SuggestionsCursor(CharSequence constraint) {
                final int count = 100;
                mResults = new ArrayList<String>(count);
                for (int i = 0; i < count; i++) {
                    mResults.add("Result " + (i + 1));
                }
                if (!TextUtils.isEmpty(constraint)) {
                    String constraintString = constraint.toString().toLowerCase(Locale.ROOT);
                    Iterator<String> iter = mResults.iterator();
                    while (iter.hasNext()) {
                        if (!iter.next().toLowerCase(Locale.ROOT).startsWith(constraintString)) {
                            iter.remove();
                        }
                    }
                }
            }

            @Override
            public int getCount() {
                return mResults.size();
            }

            @Override
            public String[] getColumnNames() {
                return mFields;
            }

            @Override
            public long getLong(int column) {
                if (column == 0) {
                    return mPos;
                }
                throw new UnsupportedOperationException("unimplemented");
            }

            @Override
            public String getString(int column) {
                if (column == 1) {
                    return mResults.get(mPos);
                }
                throw new UnsupportedOperationException("unimplemented");
            }

            @Override
            public short getShort(int column) {
                throw new UnsupportedOperationException("unimplemented");
            }

            @Override
            public int getInt(int column) {
                throw new UnsupportedOperationException("unimplemented");
            }

            @Override
            public float getFloat(int column) {
                throw new UnsupportedOperationException("unimplemented");
            }

            @Override
            public double getDouble(int column) {
                throw new UnsupportedOperationException("unimplemented");
            }

            @Override
            public boolean isNull(int column) {
                return false;
            }
        }
    }


}
