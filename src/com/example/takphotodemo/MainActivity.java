package com.example.takphotodemo;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;

import android.app.Activity;
import android.content.ContentUris;
import android.content.Intent;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.provider.DocumentsContract;
import android.provider.MediaStore;
import android.provider.MediaStore.Images.Media;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.Toast;

public class MainActivity extends Activity implements OnClickListener {
	private final int TAKE_PHOTO = 1;
	private final int CROP_PHOTO = 2;
	private final int GET_ALBUM = 3;
	private Button bt_takePhpto, bt_getPhoFromAlbum;
	private ImageView imageView;
	private Uri imaUri;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);
		bt_takePhpto = (Button) findViewById(R.id.bt_takePhoto);
		imageView = (ImageView) findViewById(R.id.imageView);
		bt_getPhoFromAlbum = (Button) findViewById(R.id.bt_getPhotoFromAlbum);
		bt_takePhpto.setOnClickListener(this);
		bt_getPhoFromAlbum.setOnClickListener(this);
	}

	@Override
	public void onClick(View v) {
		switch (v.getId()) {
		case R.id.bt_takePhoto:
			File image = new File(Environment.getExternalStorageDirectory(),
					"image.jpg");
			if (image.exists()) {
				image.delete();
			}
			try {
				image.createNewFile();
				imaUri = Uri.fromFile(image);
				Intent intent = new Intent("android.media.action.IMAGE_CAPTURE");
				intent.putExtra(MediaStore.EXTRA_OUTPUT, imaUri);
				startActivityForResult(intent, TAKE_PHOTO);

			} catch (IOException e) {
				e.printStackTrace();
			}
			break;
		case R.id.bt_getPhotoFromAlbum:
			Intent intent = new Intent("android.intent.action.GET_CONTENT");
			intent.setType("image/*");
			startActivityForResult(intent, GET_ALBUM);

			break;
		default:
			break;
		}

	}

	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		super.onActivityResult(requestCode, resultCode, data);
		switch (requestCode) {
		case TAKE_PHOTO:
			if (resultCode == RESULT_OK) {
				Intent intent = new Intent("com.android.camera.action.CROP");
				intent.setDataAndType(imaUri, "image/*");
				intent.putExtra("scale", true);
				intent.putExtra(MediaStore.EXTRA_OUTPUT, imaUri);
				startActivityForResult(intent, CROP_PHOTO);
			}
			break;
		case CROP_PHOTO:
			try {
				Bitmap bitmap = BitmapFactory.decodeStream(getContentResolver()
						.openInputStream(imaUri));
				imageView.setImageBitmap(bitmap);

			} catch (FileNotFoundException e) {
				e.printStackTrace();
			}
			break;
		case GET_ALBUM:
			if (resultCode == RESULT_OK) {
				// 首先判断系统是否4.4以上
				if (Build.VERSION.SDK_INT >= 19) {
					handleImageOnKitKat(data);
				} else {
					handleImageBeforeLitKat(data);
				}
			}
			break;
		default:
			break;
		}
	}

	public void handleImageBeforeLitKat(Intent data) {
		Uri uri = data.getData();
		String imagePath = getImagePath(uri, null);
		showImage(imagePath);
	}

	public void handleImageOnKitKat(Intent data) {
		String imagePath = null;
		Uri uri = data.getData();
		// 如果是document型的uri，则通过document id来处理
		if (DocumentsContract.isDocumentUri(this, uri)) {
			String docid = DocumentsContract.getDocumentId(uri);
			// 如果uri的authority类型是media
			if ("com.android.providers.media.documents".equals(uri
					.getAuthority())) {
				String id = docid.split(":")[0];
				String selection = MediaStore.Images.Media._ID + "=" + id;
				imagePath = getImagePath(
						MediaStore.Images.Media.EXTERNAL_CONTENT_URI, selection);
				// // 如果uri的authority类型是downloads
			} else if ("com.android.providers.downloads.documents".equals(uri
					.getAuthority())) {
				Uri contentUri = ContentUris.withAppendedId(
						Uri.parse("content://downloads/public_downloads"),
						Long.valueOf(docid));
				imagePath = getImagePath(contentUri, null);
			}
			// 如果不是document类型的uri，那么用普通方法处理
		} else if ("content".equalsIgnoreCase(uri.getScheme())) {
			imagePath = getImagePath(uri, null);
		}

		showImage(imagePath);
	}

	public String getImagePath(Uri uri, String selection) {
		String imagePath = null;
		Cursor cursor = null;
		cursor = getContentResolver().query(uri, null, selection, null, null);
		if (cursor != null) {
			if (cursor.moveToFirst()) {
				imagePath = cursor.getString(cursor.getColumnIndex(Media.DATA));
			}
		}
		cursor.close();

		return imagePath;
	}

	public void showImage(String imagePath) {
		if (imagePath != null) {
			Bitmap bitmap = BitmapFactory.decodeFile(imagePath);
			imageView.setImageBitmap(bitmap);
		} else {
			Toast.makeText(this, "get image failure", Toast.LENGTH_SHORT)
					.show();
		}
	}
}
