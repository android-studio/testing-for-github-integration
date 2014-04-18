package com.jg.videonotebook;

import android.app.Activity;
import android.app.ListFragment;
import android.content.Intent;
import android.database.sqlite.SQLiteDatabase;
import android.graphics.Bitmap;
import android.media.ThumbnailUtils;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.view.View;
import android.widget.AbsListView;
import android.widget.Button;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListView;

import com.jg.videonotebook.adapters.VideoNoteAdapter;
import com.jg.videonotebook.dao.DaoMaster;
import com.jg.videonotebook.dao.DaoSession;
import com.jg.videonotebook.dao.VideoNote;
import com.jg.videonotebook.dao.VideoNoteDao;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.Date;

import de.greenrobot.dao.query.Query;

/**
 * A fragment representing a single Notebook detail screen.
 * This fragment is either contained in a {@link NotebookListActivity}
 * in two-pane mode (on tablets) or a {@link NotebookDetailActivity}
 * on handsets.
 */
public class NotebookDetailFragment extends ListFragment implements View.OnClickListener {
    /**
     * The fragment argument representing the item ID that this fragment
     * represents.
     */
    public static final String ARG_ITEM_ID = "item_id";

    /**
     * The contents
     */
    private String mNotebookId;
    private VideoNoteDao mVideoNotesDao;
    private Button mAddButton;
    private Button mTakeVideo;
    private Button mChooseVideo;
    private EditText mNewComment;
    private ImageView mVideoPreview;
    private FrameLayout mVideoContainer;
    private LinearLayout mVideoButtonContainer;
    private Query mQuery;
    private VideoNoteAdapter mAdapter;
    private SwipeDismissList mSwipeList;

    /**
     * Mandatory empty constructor for the fragment manager to instantiate the
     * fragment (e.g. upon screen orientation changes).
     */
    public NotebookDetailFragment() {
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (getArguments().containsKey(ARG_ITEM_ID)) {
            mNotebookId = getArguments().getString(ARG_ITEM_ID);

            DaoMaster.DevOpenHelper helper = new DaoMaster.DevOpenHelper(getActivity(), "notes-db", null);
            SQLiteDatabase db = helper.getWritableDatabase();
            DaoMaster daoMaster = DaoMaster.getDaoMaster(db);
            DaoSession mDaoSession = daoMaster.newSession();
            mVideoNotesDao = mDaoSession.getVideoNoteDao();

            mQuery = mVideoNotesDao.queryBuilder()
                    .where(VideoNoteDao.Properties.NotebookId.eq(mNotebookId))
                    .orderAsc(VideoNoteDao.Properties.Date)
                    .build();
        }
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        View footer = getActivity().getLayoutInflater().inflate(R.layout.new_note, null);
        if (footer != null) {
            mAddButton = (Button)footer.findViewById(R.id.create_note);
            mChooseVideo = (Button)footer.findViewById(R.id.new_note_choose_video);
            mChooseVideo.setEnabled(false);
            mTakeVideo = (Button)footer.findViewById(R.id.new_note_take_video);
            mNewComment = (EditText)footer.findViewById(R.id.new_note_comment);
            mVideoPreview = (ImageView)footer.findViewById(R.id.video_preview_thumb);
            mVideoContainer = (FrameLayout)footer.findViewById(R.id.video_chooser_and_preview);
            mVideoButtonContainer = (LinearLayout)footer.findViewById(R.id.video_button_container);

            if (mAddButton != null)
                mAddButton.setOnClickListener(this);

            if (mChooseVideo != null)
                mChooseVideo.setOnClickListener(this);

            if (mTakeVideo != null)
                mTakeVideo.setOnClickListener(this);

            footer.setLayoutParams(new ListView.LayoutParams(
                    ListView.LayoutParams.MATCH_PARENT, ListView.LayoutParams.WRAP_CONTENT));
            getListView().addFooterView(footer);
        }
        mAdapter = new VideoNoteAdapter(getActivity(), mQuery.listLazy(), R.layout.note_detail);
        setListAdapter(mAdapter);

        ListView lv = getListView();

        lv.setStackFromBottom(true);

        SwipeDismissList.UndoMode mode = SwipeDismissList.UndoMode.COLLAPSED_UNDO;
        mSwipeList = new SwipeDismissList(lv, mCallback, mode);
    }

    @Override
    public void onStop() {
        mSwipeList.discardUndo();
        super.onStop();
    }

    @Override
    public void onClick(View v) {
        if (v == null) {
            return;
        }

        if (v.equals(mTakeVideo)) {
            dispatchTakeVideoIntent();
        } else if (v.equals(mChooseVideo)) {
            dispatchChooseVideoIntent();
        } else if (v.equals(mAddButton)) {
            saveNewNote();
        }
    }

    @Override
    public void onListItemClick(ListView l, View v, int position, long id) {
        super.onListItemClick(l, v, position, id);
        String uriString = mAdapter.getItem(position).getVideoUri();
        if (uriString != null) {
            Uri uri = Uri.parse(uriString);
            Intent intent = new Intent(Intent.ACTION_VIEW, uri);
            intent.setDataAndType(uri, "video/*");
            getActivity().startActivity(intent);
        }
    }

    private SwipeDismissList.OnDismissCallback mCallback = new SwipeDismissList.OnDismissCallback() {
        public SwipeDismissList.Undoable onDismiss(AbsListView listView, final int position) {
            // Delete the item from your adapter (sample code):
            mAdapter.hideItemAtPosition(position);
            return new SwipeDismissList.Undoable() {
                public void undo() {
                    // We don't really remove, so nothing to do here
                }

                @Override
                public void discard() {
                    VideoNote vn = mAdapter.getItem(position);
                    mAdapter.remove(vn);
                    vn.delete();
                    mAdapter.setData(mQuery.listLazy());
                    super.discard();
                }
            };
        }
    };

    private static final int ACTION_TAKE_VIDEO = 1;
    private static final int ACTION_CHOOSE_VIDEO = 2;

    private Uri potentialNewVideo;

    private Uri undergoingRecording;

    private void dispatchTakeVideoIntent() {
        Intent takeVideoIntent = new Intent(MediaStore.ACTION_VIDEO_CAPTURE);

        File videosFolder = new File(Environment.getExternalStorageDirectory(), "VideoNotebookFiles");
        videosFolder.mkdirs();
        String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date()) + ".mp4";
        File image = new File(videosFolder, timeStamp);
        undergoingRecording  = Uri.fromFile(image);
        takeVideoIntent.putExtra(MediaStore.EXTRA_OUTPUT, undergoingRecording);
        takeVideoIntent.putExtra(MediaStore.EXTRA_VIDEO_QUALITY, 1);

        startActivityForResult(takeVideoIntent, ACTION_TAKE_VIDEO);
    }

    private void dispatchChooseVideoIntent() {
        Intent photoPickerIntent = new Intent(Intent.ACTION_GET_CONTENT);
        photoPickerIntent.setType("video/*");
        startActivityForResult(photoPickerIntent, ACTION_CHOOSE_VIDEO);
    }

    private void handleCameraVideo(Uri uri) {
        if (uri != null) {
            potentialNewVideo = uri;
            Bitmap thumb = ThumbnailUtils.createVideoThumbnail(uri.getPath(), MediaStore.Images.Thumbnails.MINI_KIND);
            mVideoPreview.setImageBitmap(thumb);
            mVideoContainer.bringChildToFront(mVideoPreview);
            mVideoPreview.setVisibility(View.VISIBLE);
            mVideoButtonContainer.setVisibility(View.GONE);
        } else {
            mVideoContainer.bringChildToFront(mVideoButtonContainer);
            mVideoButtonContainer.setVisibility(View.VISIBLE);
            mVideoPreview.setVisibility(View.GONE);
        }
    }

    private void saveNewNote() {
        String comment = mNewComment.getText().toString();
        String videoUri = (potentialNewVideo == null) ? null : potentialNewVideo.toString();

        if (comment != null || videoUri != null) {
            VideoNote note = new VideoNote(null, new Date(), comment,
                    videoUri, Integer.parseInt(mNotebookId));
            mVideoNotesDao.insert(note);
            mAdapter.add(note);
            mAdapter.setData(mQuery.listLazy());

            // Clear out creation area
            potentialNewVideo = null;
            mNewComment.setText("");
            mVideoContainer.bringChildToFront(mVideoButtonContainer);
            mVideoButtonContainer.setVisibility(View.VISIBLE);
            mVideoPreview.setVisibility(View.GONE);
        }
    }

    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        switch (requestCode) {
            case ACTION_TAKE_VIDEO:
                if (resultCode == Activity.RESULT_OK) {
                    handleCameraVideo(undergoingRecording);
                    undergoingRecording = null;
                }
                break;
            case ACTION_CHOOSE_VIDEO:
                if (resultCode == Activity.RESULT_OK) {
                    // Todo: implement
                }
                break;
        }
    }
}
