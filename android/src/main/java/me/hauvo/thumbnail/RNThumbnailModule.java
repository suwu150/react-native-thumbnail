
package me.hauvo.thumbnail;

import com.facebook.react.bridge.ReactApplicationContext;
import com.facebook.react.bridge.ReactContextBaseJavaModule;
import com.facebook.react.bridge.ReactMethod;
import com.facebook.react.bridge.Callback;
import com.facebook.react.bridge.Promise;
import com.facebook.react.bridge.Arguments;
import com.facebook.react.bridge.WritableMap;
import android.media.ThumbnailUtils;
import android.net.Uri;
import android.provider.MediaStore;
import android.provider.MediaStore.Video.Thumbnails;
import android.graphics.Bitmap;
import android.os.Environment;
import android.util.Log;
import android.media.MediaMetadataRetriever;
import 	android.graphics.Matrix;
import android.webkit.URLUtil;

import java.io.FileInputStream;
import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.util.UUID;
import java.io.File;
import java.io.OutputStream;
import java.io.FileOutputStream;


public class RNThumbnailModule extends ReactContextBaseJavaModule {

  private final ReactApplicationContext reactContext;

  public RNThumbnailModule(ReactApplicationContext reactContext) {
    super(reactContext);
    this.reactContext = reactContext;
  }

  @Override
  public String getName() {
    return "RNThumbnail";
  }

  @ReactMethod
  public void get(String filePath, Promise promise) {
    Log.e("filePath", filePath);
    MediaMetadataRetriever retriever = new MediaMetadataRetriever();
    if (filePath == null || filePath.isEmpty()) {
      Log.d("RNThumbnailModule", "getRetrieverThumbnail filePath is invalid");
      if (promise != null) {
        promise.reject("E_RNThumnail_ERROR", "filePath is invalid");
      }
      return;
    }
    if (URLUtil.isFileUrl(filePath)) {
      String decodedPath;
      try {
        decodedPath = URLDecoder.decode(filePath, "UTF-8");
      } catch (UnsupportedEncodingException e) {
        decodedPath = filePath;
      }
      Log.d("RNThumbnailModule", "getRetrieverThumbnail decodedPath:" + decodedPath);
      retriever.setDataSource(decodedPath.replace("file://", ""));
    } else if (filePath.startsWith("content://")) {
      Uri retrieverUri = Uri.parse(filePath);
      Log.d("RNThumbnailModule", "getRetrieverThumbnail retrieverUri:" + retrieverUri);
      try {
        retriever.setDataSource(this.reactContext.getBaseContext(), retrieverUri);
      } catch (RuntimeException e) {
        Log.d("RNThumbnailModule", "getRetrieverThumbnail RuntimeException e:" + e.getMessage());
      }
    }

    Bitmap image = retriever.getFrameAtTime(1000000, MediaMetadataRetriever.OPTION_CLOSEST_SYNC);
    String fullPath = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES).getAbsolutePath() + "/thumb";
    retriever.release();

    try {
      File dir = new File(fullPath);
      if (!dir.exists()) {
        dir.mkdirs();
      }

      OutputStream fOut = null;
      // String fileName = "thumb-" + UUID.randomUUID().toString() + ".jpeg";
      String fileName = "thumb-" + UUID.randomUUID().toString() + ".jpeg";
      File file = new File(fullPath, fileName);
      file.createNewFile();
      fOut = new FileOutputStream(file);

      // 100 means no compression, the lower you go, the stronger the compression
      image.compress(Bitmap.CompressFormat.JPEG, 100, fOut);
      fOut.flush();
      fOut.close();

      // MediaStore.Images.Media.insertImage(reactContext.getContentResolver(), file.getAbsolutePath(), file.getName(), file.getName());

      WritableMap map = Arguments.createMap();

      map.putString("path", "file://" + fullPath + '/' + fileName);
      map.putDouble("width", image.getWidth());
      map.putDouble("height", image.getHeight());

      promise.resolve(map);

    } catch (Exception e) {
      Log.e("E_RNThumnail_ERROR", e.getMessage());
      promise.reject("E_RNThumnail_ERROR", e);
    }
  }
}
