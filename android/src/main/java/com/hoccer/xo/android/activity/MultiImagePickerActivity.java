package com.hoccer.xo.android.activity;

import android.app.Activity;
import android.app.LoaderManager;
import android.content.Context;
import android.content.CursorLoader;
import android.content.Intent;
import android.content.Loader;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.*;
import com.hoccer.xo.release.R;
import com.squareup.picasso.Picasso;

import java.util.HashSet;

public class MultiImagePickerActivity extends Activity implements LoaderManager.LoaderCallbacks<Cursor> {

    private ImageAdapter mAdapter;
    private GridView mImageGridView;
    private HashSet<String> mSelectedImages = new HashSet<String>();

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_multi_image_picker);
        mImageGridView = (GridView) findViewById(R.id.gv_images);
        mImageGridView.setClipToPadding(false);
        mAdapter = new ImageAdapter(this);
        mImageGridView.setAdapter(mAdapter);
        Button selectBtn = (Button) findViewById(R.id.selectBtn);
        selectBtn.setOnClickListener(new View.OnClickListener() {

            public void onClick(View v) {
                if (!mSelectedImages.isEmpty()) {
                    String[] images = new String[mSelectedImages.size()];
                    int counter = 0;
                    for (String uri: mSelectedImages) {
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
        getLoaderManager().initLoader(ImageQuery.QUERY_ID, null, this);
//        ThumbnailManager.getInstance(this).setHeightDp(80);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
//        ThumbnailManager.getInstance(this).setHeightDp(200);
//        ThumbnailManager.getInstance(this).clearCache();
    }

    @Override
    public Loader<Cursor> onCreateLoader(int i, Bundle bundle) {
        String[] projection = {MediaStore.Images.Media._ID, MediaStore.Images.Media.DATA};
        return new CursorLoader(this, MediaStore.Images.Media.EXTERNAL_CONTENT_URI, projection, null, null, null);
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
            ImageView imageview;
            CheckBox checkbox;
            int id;
        }

        @Override
        public View newView(Context context, Cursor cursor, ViewGroup viewGroup) {
            final View itemLayout = mInflater.inflate(R.layout.item_multi_image_picker, viewGroup, false);
            final ViewHolder holder = new ViewHolder();
            holder.imageview = (ImageView) itemLayout.findViewById(R.id.thumbImage);
            holder.checkbox = (CheckBox) itemLayout.findViewById(R.id.itemCheckBox);
            itemLayout.setTag(holder);
            return itemLayout;
        }

        @Override
        public void bindView(View view, Context context, Cursor cursor) {
            final ViewHolder holder = (ViewHolder) view.getTag();
            final String id = cursor.getString(ImageQuery.ID);
            final String contentUri = Uri.withAppendedPath(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, "" + id).toString();
            final String dataUri = cursor.getString(ImageQuery.DATA);
            holder.checkbox.setOnCheckedChangeListener(null);
            if (mSelectedImages.contains(contentUri.toString())) {
                holder.checkbox.setChecked(true);
            } else {
                holder.checkbox.setChecked(false);
            }
            holder.checkbox.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
                @Override
                public void onCheckedChanged(CompoundButton compoundButton, boolean selected) {
                    if (selected) {
                        mSelectedImages.add(contentUri.toString());
                    } else {
                        mSelectedImages.remove(contentUri.toString());
                    }
                }
            });

            Picasso.with(mContext)
                    .load(contentUri)
                    .resize(200,200)
                    .centerCrop()
                    .into(holder.imageview);

            holder.imageview.setOnClickListener(new View.OnClickListener() {
                public void onClick(View v) {
                    Intent intent = new Intent();
                    intent.setAction(Intent.ACTION_VIEW);
                    intent.setDataAndType(Uri.parse("file://" + dataUri), "image/*");
                    startActivity(intent);
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
