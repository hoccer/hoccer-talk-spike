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
import android.os.AsyncTask;
import android.os.Bundle;
import android.provider.MediaStore;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.CursorAdapter;
import android.widget.GridView;
import android.widget.ImageView;
import com.hoccer.xo.android.XoApplication;
import com.hoccer.xo.android.util.DisplayUtils;
import com.hoccer.xo.android.util.UriUtils;
import com.hoccer.xo.android.view.SquaredImageView;
import com.hoccer.xo.android.view.SquaredRelativeLayout;
import com.artcom.hoccer.R;
import com.squareup.picasso.Callback;
import com.squareup.picasso.Picasso;
import org.apache.log4j.Logger;

import java.util.HashSet;

public class MultiImagePickerActivity extends Activity implements LoaderManager.LoaderCallbacks<Cursor> {

    private static final Logger LOG = Logger.getLogger(MultiImagePickerActivity.class);

    public static final String EXTRA_IMAGES = "IMAGES";

    private ImageAdapter mAdapter;
    private GridView mImageGridView;
    private final HashSet<String> mSelectedImages = new HashSet<String>();
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
                MediaStore.Images.Media._ID,
                MediaStore.Images.Media.DATA,
        };
        return new CursorLoader(this, MediaStore.Images.Media.EXTERNAL_CONTENT_URI, projection, null, null, MediaStore.Images.Media.DATE_ADDED + " DESC");
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
        private final LayoutInflater mInflater;
        private final Context mContext;

        public ImageAdapter(Context context) {
            super(context, null, 0);
            mContext = context;
            mInflater = (LayoutInflater) getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        }

        class ViewHolder {
            SquaredRelativeLayout squaredRelativeLayout;
            ImageView thumbnailImageView;
            AsyncTask thumbnailTask;
        }

        private String getThumbnailPathByImageId(String imageId) {
            String thumbnailPath = null;

            long start = System.currentTimeMillis();

            String[] projection = {MediaStore.Images.Thumbnails.DATA};
            Cursor thumbCursor = getContentResolver().query(MediaStore.Images.Thumbnails.EXTERNAL_CONTENT_URI, projection, MediaStore.Images.Thumbnails.IMAGE_ID + " LIKE ?", new String[]{imageId}, null);
            if (thumbCursor != null && thumbCursor.moveToFirst()) {
                thumbnailPath = thumbCursor.getString(thumbCursor.getColumnIndex(MediaStore.Images.Thumbnails.DATA));
            }
            LOG.trace("Path of thumbnail image: " + thumbnailPath);
            LOG.trace("Duration: " + (System.currentTimeMillis() - start));

            return thumbnailPath;
        }

        @Override
        public View newView(Context context, Cursor cursor, ViewGroup viewGroup) {
            final View itemLayout = mInflater.inflate(R.layout.item_multi_image_picker, viewGroup, false);

            final ViewHolder holder = new ViewHolder();
            holder.squaredRelativeLayout = (SquaredRelativeLayout) itemLayout.findViewById(R.id.squared_rl_selected);
            holder.thumbnailImageView = (SquaredImageView) itemLayout.findViewById(R.id.thumbImage);
            holder.thumbnailImageView.setEnabled(false);
            itemLayout.setTag(holder);

            return itemLayout;
        }

        @Override
        public void bindView(View view, Context context, Cursor cursor) {
            final ViewHolder holder = (ViewHolder) view.getTag();

            final String imageId = cursor.getString(cursor.getColumnIndexOrThrow(MediaStore.Images.Media._ID));
            final String imagePath = cursor.getString(cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DATA));
            final Uri imageUri = Uri.withAppendedPath(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, imageId);
            LOG.info("Image ID: " + imageId);
            LOG.info("File path of image: " + imagePath);
            LOG.info("Uri of image: " + imageUri);

            Picasso.with(mContext).cancelRequest(holder.thumbnailImageView);
            holder.thumbnailImageView.setImageBitmap(null);
            if (holder.thumbnailTask != null) {
                holder.thumbnailTask.cancel(true);
            }
            holder.thumbnailTask = new AsyncTask<Object, Void, String>() {

                @Override
                protected String doInBackground(Object... params) {

                    String imageId = (String) params[0];
                    String imagePath = (String) params[1];

                    String thumbnailPath = getThumbnailPathByImageId(imageId);
                    if (thumbnailPath == null || thumbnailPath.isEmpty()) {
                        thumbnailPath = imagePath;
                    }

                    return thumbnailPath;
                }

                @Override
                protected void onPostExecute(String thumbnailPath) {
                    Picasso.with(mContext)
                            .load(UriUtils.FILE_URI_PREFIX + thumbnailPath)
                            .placeholder(R.drawable.ic_img_placeholder)
                            .error(R.drawable.ic_img_placeholder_error)
                            .centerCrop()
                            .fit()
                            .into(holder.thumbnailImageView, new Callback() {
                                @Override
                                public void onSuccess() {
                                    holder.thumbnailImageView.setEnabled(true);
                                }

                                @Override
                                public void onError() {
                                }
                            });
                }
            };
            holder.thumbnailTask.execute(imageId, imagePath);

            if (mSelectedImages.contains(imageUri.toString())) {
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
                    LOG.info("imageUri: " + imageUri);
                    intent.setDataAndType(imageUri, "image/*");
                    startActivity(intent);
                    return true;
                }
            });

            holder.thumbnailImageView.setOnClickListener(new View.OnClickListener() {
                public void onClick(View v) {
                    if (!holder.squaredRelativeLayout.isSelected()) {
                        holder.squaredRelativeLayout.setSelected(true);
                        holder.squaredRelativeLayout.setVisibility(View.VISIBLE);
                        mSelectedImages.add(imageUri.toString());
                        if (XoApplication.getConfiguration().isDevelopmentModeEnabled()) {
                            LOG.info("Selected image path: " + imagePath);
                        }
                    } else {
                        holder.squaredRelativeLayout.setSelected(false);
                        holder.squaredRelativeLayout.setVisibility(View.GONE);
                        mSelectedImages.remove(imageUri.toString());
                    }
                    mSelectBtn.setText(String.format(getString(R.string.select_count), mSelectedImages.size()));
                }
            });
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
