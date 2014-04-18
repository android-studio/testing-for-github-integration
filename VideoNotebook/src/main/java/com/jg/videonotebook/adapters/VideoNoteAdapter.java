package com.jg.videonotebook.adapters;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.YuvImage;
import android.media.ThumbnailUtils;
import android.net.Uri;
import android.provider.MediaStore;
import android.util.LruCache;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import com.jg.videonotebook.R;
import com.jg.videonotebook.dao.VideoNote;

import de.greenrobot.dao.query.LazyList;


public class VideoNoteAdapter extends LazyListAdapter<VideoNote> {

    public VideoNoteAdapter(Context c, LazyList<VideoNote> data, int resource) {
        super(c, data, resource);
    }

    // Cache 4Mb of bitmaps to avoid disk read and decode costs
    private LruCache<Uri, Bitmap> thumbnailCache = new LruCache<Uri, Bitmap>(4 * 1024 * 1024);

    @Override
    protected View fillViewImpl(int position, View v) {
        VideoNote note = mData.get(position);
        String uriString = note.getVideoUri();

        // Comments
        TextView commentsView = (TextView)v.findViewById(R.id.comments_text);
        commentsView.setText(note.getComments());

        // Video Preview
        ImageView imageView = (ImageView)v.findViewById(R.id.thumbnail);
        if (uriString != null) {
            Uri uri = Uri.parse(uriString);
            Bitmap thumb = thumbnailCache.get(uri);
            if (thumb == null) {
                thumb = ThumbnailUtils.createVideoThumbnail(uri.getPath(), MediaStore.Images.Thumbnails.MINI_KIND);
                if (thumb != null) {
                    thumbnailCache.put(uri, thumb);
                } else {
                    // Put a blank in
                    thumbnailCache.put(uri, Bitmap.createBitmap(1,1, Bitmap.Config.ARGB_4444));
                }
            }
            imageView.setImageBitmap(thumb);
        } else {
            imageView.setImageBitmap(null);
        }
        return v;
    }
}
