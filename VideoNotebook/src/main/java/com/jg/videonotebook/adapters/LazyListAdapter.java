package com.jg.videonotebook.adapters;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;

import java.util.HashSet;
import java.util.Set;
import java.util.TreeSet;

import de.greenrobot.dao.query.LazyList;

public abstract class LazyListAdapter<T> extends ArrayAdapter<T> {

    protected LazyList<T> mData;
    private int mResourceId;
    private Context mContext;

    // Set of items to mask from view
    private Set<Integer> mPositionsToHide = new TreeSet<Integer>();

    public LazyListAdapter(Context c, LazyList<T> data, int resource) {
        super(c, resource);
        mData = data;
        mContext = c;
        mResourceId = resource;
    }

    @Override
    public int getCount() {
        return mData.size();
    }

    @Override
    public T getItem(int position) {
        return mData.get(position);
    }

    @Override
    public void add(T object) {

    }

    @Override
    public void remove(T object) {

    }

    @Override
    public int getPosition(T item) {
        return mData.indexOf(item);
    }

    /**
     * Call this to mask items from display in the list view until next call to setData
     * @param position the position of the item to hide
     */
    public void hideItemAtPosition(int position) {
        mPositionsToHide.add(position);
        notifyDataSetChanged();
    }

    public void setData(LazyList<T> data) {
        mData = data;
        mPositionsToHide.clear();
        notifyDataSetChanged();
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        View v = convertView;
        if (v == null) {
            LayoutInflater li = LayoutInflater.from(mContext);
            v = li.inflate(mResourceId, null);
        }

        View toReturn = fillViewImpl(position, v);
        if (mPositionsToHide.contains(position)) {
            toReturn.setVisibility(View.GONE);
        } else {
            toReturn.setVisibility(View.VISIBLE);
        }
        return toReturn;
    }

    protected abstract View fillViewImpl(int position, View v);

    @Override
    public boolean isEmpty() {
        return mData == null || mData.isEmpty();
    }
}
