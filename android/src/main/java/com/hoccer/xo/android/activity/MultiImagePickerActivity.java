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
import android.widget.*;
import com.hoccer.xo.android.util.DisplayUtils;
import com.hoccer.xo.android.view.SquaredImageView;
import com.hoccer.xo.android.view.SquaredRelativeLayout;
import com.hoccer.xo.release.R;
import com.squareup.picasso.Picasso;
import org.apache.log4j.Logger;

import java.util.HashSet;

public class MultiImagePickerActivity extends Activity implements LoaderManager.LoaderCallbacks<Cursor> {

    private static final Logger LOG = Logger.getLogger(MultiImagePickerActivity.class);
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
                    intent.putExtra("IMAGES", images);
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
    protected void onDestroy() {
        super.onDestroy();
    }

    @Override
    public Loader<Cursor> onCreateLoader(int i, Bundle bundle) {
        String[] projection = {MediaStore.Images.Media._ID, MediaStore.Images.Media.DATA, MediaStore.Images.Media.DATE_ADDED};
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

    class ImageAdapter extends CursorAdapter {
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
            final String id = cursor.getString(ImageQuery.ID);
            final String contentUri = Uri.withAppendedPath(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, "" + id).toString();
            final String dataUri = cursor.getString(ImageQuery.DATA);

            if (mSelectedImages.contains(contentUri.toString())) {
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
                    intent.setDataAndType(Uri.parse("file://" + dataUri), "image/*");
                    startActivity(intent);
                    return true;
                }
            });

            holder.thumbnailImageView.setOnClickListener(new View.OnClickListener() {
                public void onClick(View v) {
                    if (!holder.squaredRelativeLayout.isSelected()) {
                        holder.squaredRelativeLayout.setSelected(true);
                        holder.squaredRelativeLayout.setVisibility(View.VISIBLE);
                        mSelectedImages.add(contentUri.toString());
                    } else {
                        holder.squaredRelativeLayout.setSelected(false);
                        holder.squaredRelativeLayout.setVisibility(View.GONE);
                        mSelectedImages.remove(contentUri.toString());
                    }
                    mSelectBtn.setText(String.format(getString(R.string.select_count), mSelectedImages.size()));
                }
            });

            Picasso.with(mContext)
                    .load(contentUri)
                    .placeholder(R.drawable.ic_img_placeholder)
                    .error(R.drawable.ic_img_placeholder_error)
                    .resize(200, 200)
                    .centerCrop()
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
