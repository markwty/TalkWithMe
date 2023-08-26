package com.example.talkwithme;

import android.graphics.Rect;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

class CustomItemDecoration extends RecyclerView.ItemDecoration {
    private int topOffset, interspace, bottomOffset;

    CustomItemDecoration(int topOffset, int interspace, int bottomOffset) {
        this.topOffset = topOffset;
        this.interspace = interspace;
        this.bottomOffset = bottomOffset;
    }

    @Override
    public void getItemOffsets(@NonNull Rect outRect, @NonNull View view, @NonNull RecyclerView parent, @NonNull RecyclerView.State state) {
        super.getItemOffsets(outRect, view, parent, state);
        int dataSize = state.getItemCount();
        int position = parent.getChildAdapterPosition(view);
        if (dataSize > 0) {
            if(position == 0){
                outRect.set(0, topOffset, 0, 0);
            }else{
                outRect.set(0, interspace, 0, 0);
                if(position == dataSize - 1){
                    outRect.set(0, interspace, 0, bottomOffset);
                }
            }
        }
    }
}
