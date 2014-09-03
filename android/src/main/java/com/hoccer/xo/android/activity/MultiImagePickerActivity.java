package com.hoccer.xo.android.activity;

import android.app.Activity;
import android.app.LoaderManager;
import android.content.Context;
import android.content.CursorLoader;
import android.content.Intent;
import android.content.Loader;
import android.database.Cursor;
import android.graphics.Point;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.CursorAdapter;
import android.widget.GridView;
import android.widget.ImageView;
import com.hoccer.xo.android.util.DisplayUtils;
import com.hoccer.xo.android.view.SquaredImageView;
import com.hoccer.xo.android.view.SquaredRelativeLayout;
import com.hoccer.xo.release.R;
import com.squareup.picasso.Picasso;
import org.apache.log4j.Logger;

import java.util.HashSet;

public class MultiImagePickerActivity extends Activity implements LoaderManager.LoaderCallbacks<Cursor> {

    private static final Logger LOG = Logger.getLogger(MultiImagePickerActivity.class);

    public static final String EXTRA_IMAGES = "IMAGES";

    private ImageAdapter mAdapter;
    private GridView mImageGridView;
    private HashSet<String> mSelectedImages = new HashSet<String>();
    private Button mSelectBtn;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_multi_image_picker);
        mImageGridView = (GridView) findViewById(R.id.gv_images);
        mImageGridView.setClipToPadding(false);
        mAdapter = new ImageAdapter(this);
        mImageGridView.setAdapter(mAdapter);
        mSelectBtn = (Button) findViewById(R.id.selectBtn);
        mSelectBtn.setOnClickListener(new View.OnClickListener() {

            public void onClick(View v) {
                if (!mSelectedImages.isEmpty()) {
                    String[] images = new String[mSelectedImages.size()];
                    int counter = 0;
                    for (String uri : mSelectedImages) {
                        images[counter++] = uri;
                    }
                    Intent intent = new Intent();
                    intent.setData(Uri.EMPTY);
                    intent.putExtra(EXTRA_IMAGES, images);
                    setResult(RESULT_OK, intent);
                    finish();
                }
            }
        });
        mSelectBtn.setText(String.format(getString(R.string.select_count), 0));
        getLoaderManager().initLoader(ImageQuery.QUERY_ID, null, this);
    }

    @Override
    protected void onResume() {
        super.onResume();
        Point size = DisplayUtils.getDisplaySize(this);
        if (size.x > size.y) {
            // horizontal
            mImageGridView.setNumColumns(5);
        } else {
            // vertical
            mImageGridView.setNumColumns(3);
        }

    }

    @Override
    public Loader<Cursor> onCreateLoader(int i, Bundle bundle) {

        String[] projection = {
                MediaStore.Images.Thumbnails._ID,
                MediaStore.Images.Thumbnails.DATA,
                MediaStore.Images.Thumbnails.IMAGE_ID,
        };

        return new CursorLoader(this, MediaStore.Images.Thumbnails.EXTERNAL_CONTENT_URI, projection, null, null, "image_id DESC");
    }

    @Override
    public void onLoadFinished(Loader<Cursor> cursorLoader, Cursor cursor) {
        mAdapter.swapCursor(cursor);
    }

    @Override
    public void onLoaderReset(Loader<Cursor> cursorLoader) {
        mAdapter.swapCursor(null);
    }

    private class ImageAdapter extends CursorAdapter {
        private LayoutInflater mInflater;
        private Context mContext;

        public ImageAdapter(Context context) {
            super(context, null, 0);
            mContext = context;
            mInflater = (LayoutInflater) getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        }

        class ViewHolder {
            SquaredRelativeLayout squaredRelativeLayout;
            ImageView thumbnailImageView;
            int id;
        }

        @Override
        public View newView(Context context, Cursor cursor, ViewGroup viewGroup) {
            final View itemLayout = mInflater.inflate(R.layout.item_multi_image_picker, viewGroup, false);

            final ViewHolder holder = new ViewHolder();
            holder.squaredRelativeLayout = (SquaredRelativeLayout) itemLayout.findViewById(R.id.squared_rl_selected);
            holder.thumbnailImageView = (SquaredImageView) itemLayout.findViewById(R.id.thumbImage);
            itemLayout.setTag(holder);

            return itemLayout;
        }

        @Override
        public void bindView(View view, Context context, Cursor cursor) {
            final ViewHolder holder = (ViewHolder) view.getTag();

            final Uri thumbPath = Uri.parse(cursor.getString(cursor.getColumnIndexOrThrow(MediaStore.Images.Thumbnails.DATA)));
            LOG.info("Path of thumbnail image: " + thumbPath);

            String originalImageId = cursor.getString(cursor.getColumnIndexOrThrow(MediaStore.Images.Thumbnails.IMAGE_ID));
            final Uri dataUri = Uri.withAppendedPath(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, "" + originalImageId);
            LOG.info("Data Uri of original image: " + dataUri);

            if (mSelectedImages.contains(thumbPath.toString())) {
                holder.squaredRelativeLayout.setSelected(true);
                holder.squaredRelativeLayout.setVisibility(View.VISIBLE);
            } else {
                holder.squaredRelativeLayout.setSelected(false);
                holder.squaredRelativeLayout.setVisibility(View.GONE);
            }

            holder.thumbnailImageView.setOnLongClickListener(new View.OnLongClickListener() {
                @Override
                public boolean onLongClick(View v) {
                    Intent intent = new Intent();
                    intent.setAction(Intent.ACTION_VIEW);
                    intent.setDataAndType(dataUri, "image/*");
                    startActivity(intent);
                    return true;
                }
            });

            holder.thumbnailImageView.setOnClickListener(new View.OnClickListener() {
                public void onClick(View v) {
                    if (!holder.squaredRelativeLayout.isSelected()) {
                        holder.squaredRelativeLayout.setSelected(true);
                        holder.squaredRelativeLayout.setVisibility(View.VISIBLE);
                        mSelectedImages.add(thumbPath.toString());
                    } else {
                        holder.squaredRelativeLayout.setSelected(false);
                        holder.squaredRelativeLayout.setVisibility(View.GONE);
                        mSelectedImages.remove(thumbPath.toString());
                    }
                    mSelectBtn.setText(String.format(getString(R.string.select_count), mSelectedImages.size()));
                }
            });

            Picasso.with(mContext).cancelRequest(holder.thumbnailImageView);
            Picasso.with(mContext)
                    .load("file://" + thumbPath)
                    .placeholder(R.drawable.ic_img_placeholder)
                    .error(R.drawable.ic_img_placeholder_error)
                    .centerCrop()
                    .fit()
                    .into(holder.thumbnailImageView);
        }

        @Override
        public int getCount() {
            if (getCursor() == null) {
                return 0;
            }
            return super.getCount();
        }
    }

    public interface ImageQuery {
        final static int QUERY_ID = 1;
        final static int ID = 0;
        final static int DATA = 1;
    }
}
