package com.piusvelte.dirigible.util;

import android.support.annotation.NonNull;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.StaggeredGridLayoutManager;

/**
 * @author bemmanuel
 * @since 3/5/16
 */
public class RecyclerViewUtils {

    private RecyclerViewUtils() {
        // not instantiable
    }

    /**
     * Find the first visible position from {@link android.support.v7.widget.RecyclerView.LayoutManager}
     *
     * @param layoutManager The LayoutManager
     * @return
     */
    static int getFirstVisiblePosition(RecyclerView.LayoutManager layoutManager) {
        if (layoutManager instanceof StaggeredGridLayoutManager) {
            StaggeredGridLayoutManager staggeredGridLayoutManager = (StaggeredGridLayoutManager) layoutManager;
            int[] firstVisibleItems = new int[staggeredGridLayoutManager.getSpanCount()];
            staggeredGridLayoutManager.findFirstVisibleItemPositions(firstVisibleItems);
            return firstVisibleItems[0];
        } else if (layoutManager instanceof LinearLayoutManager) {
            return ((LinearLayoutManager) layoutManager).findFirstVisibleItemPosition();
        } else {
            throw new IllegalArgumentException("This LayoutManager is null or not yet supported!");
        }
    }

    /**
     * Add auto-paging support to a {@link RecyclerView}
     *
     * @param recyclerView
     * @param listener
     */
    public static void addAutoPaging(@NonNull RecyclerView recyclerView, @NonNull OnScrollAutoPagingListener.OnAutoPageListener listener) {
        recyclerView.addOnScrollListener(new OnScrollAutoPagingListener(listener));
    }

    /**
     * {@link android.support.v7.widget.RecyclerView.OnScrollListener} implementation with callback when the last item in the list is visible
     */
    public static class OnScrollAutoPagingListener extends RecyclerView.OnScrollListener {

        private OnAutoPageListener mListener;

        OnScrollAutoPagingListener(@NonNull OnAutoPageListener listener) {
            mListener = listener;
        }

        @Override
        public void onScrolled(RecyclerView recyclerView, int dx, int dy) {
            RecyclerView.LayoutManager layoutManager = recyclerView.getLayoutManager();

            int firstVisiblePosition = getFirstVisiblePosition(layoutManager);
            int visibleItemCount = layoutManager.getChildCount();
            int totalItemCount = layoutManager.getItemCount();

            if (totalItemCount > visibleItemCount
                    && firstVisiblePosition + visibleItemCount >= totalItemCount) {
                mListener.onAutoPage();
            }
        }

        public interface OnAutoPageListener {
            void onAutoPage();
        }
    }
}
