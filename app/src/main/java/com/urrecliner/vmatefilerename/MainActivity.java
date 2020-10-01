package com.urrecliner.vmatefilerename;

import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.graphics.Typeface;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Environment;
import android.text.SpannableString;
import android.text.Spanned;
import android.text.method.ScrollingMovementMethod;
import android.text.style.ForegroundColorSpan;
import android.text.style.RelativeSizeSpan;
import android.text.style.StrikethroughSpan;
import android.text.style.StyleSpan;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.WindowManager;
import android.widget.TextView;
import android.widget.Toast;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.channels.FileChannel;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.attribute.BasicFileAttributeView;
import java.nio.file.attribute.FileTime;
import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Locale;
import java.util.Timer;
import java.util.TimerTask;

public class MainActivity extends AppCompatActivity {

    Activity mActivity;
    Context mContext;
    String srcFolder = "Vmate/sd/DCIM/100HSCAM";
    String dstFolder = "vmate";
    File srcFullPath = new File(Environment.getExternalStorageDirectory(), srcFolder), srcLongFName;
    File cameraFullPath = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DCIM),"");
    File dstFullPath, dstLongFName;
    TextView srcDst, result;
    File[] srcFiles = null;
    long [] sizes;
    DecimalFormat formatterKb = new DecimalFormat("###,###Kb");
    DecimalFormat formatterMb = new DecimalFormat("###,###Mb");
    DecimalFormat formatterGb = new DecimalFormat("###,###.## Gb");
    String srcShortName, dstShortName;
    static SharedPreferences sharedPref;
    static SharedPreferences.Editor editor;
    static float timeZone;
    static boolean firstTime, renameFlag, deleteFlag;
    final SimpleDateFormat sdfDateTime = new SimpleDateFormat("YYYYMMdd_HHmmss", Locale.getDefault());
    boolean nowRunning = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        mActivity = this;
        mContext = this;
        askPermission();
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        srcDst = findViewById(R.id.srcDst);
        result = findViewById(R.id.result);
        sharedPref = getApplicationContext().getSharedPreferences("vmate", MODE_PRIVATE);
        editor = sharedPref.edit();
        timeZone = sharedPref.getFloat("timeZone",9f);
        firstTime = sharedPref.getBoolean("firstTime",true);
        renameFlag = sharedPref.getBoolean("rename", false);
        deleteFlag = sharedPref.getBoolean("delete", false);

        result.setMovementMethod(new ScrollingMovementMethod());
        if (firstTime) {
            firstTime = false;
            editor.putBoolean("firstTime", false).apply();
            Intent intent = new Intent(this, HelpActivity.class);
            startActivity(intent);
        }
        if (timeZone == -99f) {
            Intent intent = new Intent(this, SetActivity.class);
            startActivity(intent);
        }
        ActionBar actionBar = getSupportActionBar();
        assert actionBar != null;
//        actionBar.setIcon(R.mipmap.vmate);
//        actionBar.setDisplayHomeAsUpEnabled(true);
//        actionBar.setDisplayShowTitleEnabled(true);
//        actionBar.setDisplayUseLogoEnabled(true);
        actionBar.setTitle("VMate Files");
        actionBar.setSubtitle("Rename/Copy");
//        actionBar.setHomeButtonEnabled(false);
        showSetting();
    }

    void showSetting() {
        int timePos, posStart, posFinish;
        StringBuilder sb = new StringBuilder();
        sb.append("TIME ZONE : ").append(timeZone); timePos = sb.length();
        sb.append("\n").append(sampleTimeShift()); posStart = sb.length();
        if (renameFlag) {
            sb.append("\n\nRENAME VMATE FILES");  posFinish = sb.length();
            sb.append("\ninto ").append(srcFolder);
        }
        else {
            sb.append((deleteFlag) ? "\n\nMOVE":"\n\nCOPY");
            sb.append(" VMATE FILES"); posFinish = sb.length();
            sb.append("\nfrom : ").append(srcFolder).append("\ninto : ").append(cameraFullPath.getName()).append("/").append(dstFolder);
        }
        SpannableString ss = new SpannableString(sb);
        ss.setSpan(new ForegroundColorSpan(Color.BLUE), 0, timePos, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
        ss.setSpan(new ForegroundColorSpan(Color.BLUE), posStart, posFinish, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
        srcDst.setText(ss);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.main_menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        if (id == R.id.play) {
            yesNo4CopyRename();
            return true;
        }
        else if (id == R.id.setting) {
            Intent intent = new Intent(this, SetActivity.class);
            startActivity(intent);
        }
        else if (id == R.id.help) {
            Intent intent = new Intent(this, HelpActivity.class);
            startActivity(intent);
        }
        return super.onOptionsItemSelected(item);
    }

    void yesNo4CopyRename() {

        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        String title = "Click 'OK' to " + ((renameFlag) ? "Rename" : (deleteFlag)? "Move":"Copy");
        title = title + "\n" + sampleTimeShift()+"\n";
        if (!renameFlag && deleteFlag)
            title += "<<< Remarks >>>\nEach files in source will be deleted after copying..";
        builder.setMessage(title);
        builder.setPositiveButton("OK", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                try {
                    new run_fileCopyRename().execute("");
                } catch (Exception e) {
                    Log.e("Err", e.toString());
                }
            }
        });
        builder.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
            }
        });
        AlertDialog dialog = builder.create();
        dialog.show();
    }

    private String sampleTimeShift() {
        if (srcFiles != null && srcFiles.length> 0) {
            for (File src: srcFiles) {
                String sName = src.getName();
                if (sName.indexOf(":") > 0) {
                    long dateTime = src.lastModified();
                    return sName + "\n  => " + sdfDateTime.format(dateTime - (long) (timeZone * 60 * 60 * 1000))
                            + sName.substring(sName.length() - 4);
                }
            }
        }
        return "";
    }

    void listUp_srcFiles() {
        int idx = 0;
        int count = 0;
        srcFiles = srcFullPath.listFiles();
        if (srcFiles == null)
            return;
        Arrays.sort(srcFiles);
        sizes = new long[srcFiles.length];
        StringBuilder sb = new StringBuilder();
        sb.append("\n");
        for (File file: srcFiles) {
            String fileName = file.getName();
            if (!fileName.substring(0,1).equals(".")) {
                if (fileName.indexOf(":") > 0)
                    count++;
                sizes[idx] = file.length() / 1024;
                sb.append(fileName).append("  ");
                sb.append(size2String(sizes[idx]));
                sb.append("\n");
            }
            idx++;
        }
        result.setText(sb);
        if (count == 0)
            Toast.makeText(getApplicationContext(), "No target files to rename/copy", Toast.LENGTH_LONG).show();
    }

    String size2String(long siz) {
        float howBig = (float) siz;
        if (howBig < 5000)
            return(formatterKb.format(howBig));
        else if (howBig < 1000000)
            return formatterMb.format(howBig/1024);
        else
            return formatterGb.format(howBig/1024/1024);

    }
    void readyFolder (File dir){
        try {
            if (!dir.exists()) dir.mkdirs();
        } catch (Exception e) {
            Log.e("creating Folder error", dir + "_" + e.toString());
        }
    }

    class run_fileCopyRename extends AsyncTask<String, Integer, Void> {
        @Override
        protected void onPreExecute() {
//            SystemClock.sleep(10);
            nowRunning = true;
        }

        @Override
        protected Void doInBackground(String... inputParams) {

            for (int idx = 0; idx < srcFiles.length; idx++) {
                if (!nowRunning)
                    break;
                srcLongFName = srcFiles[idx];
                srcShortName = srcLongFName.getName();
                if (!srcShortName.substring(0, 1).equals(".")) {
                    long longDate = srcFiles[idx].lastModified()- (long) (timeZone *60*60*1000);
                    dstShortName = sdfDateTime.format(longDate)+srcShortName.substring(srcShortName.length()-4);
                    dstLongFName = new File (dstFullPath, dstShortName);
                    publishProgress(idx);
                    if (srcShortName.indexOf(":") > 0) {
                        if (renameFlag) {
                            dstLongFName.delete();
                            srcLongFName.renameTo(dstLongFName);
                        } else {
                            FileChannel srcChannel = null;
                            FileChannel dstChannel = null;
                            try {
                                srcChannel = new FileInputStream(srcLongFName).getChannel();
                                dstChannel = new FileOutputStream(dstLongFName).getChannel();
                                dstChannel.transferFrom(srcChannel, 0, srcChannel.size());
                            } catch (IOException e) {
                                e.printStackTrace();
                            }
                            try {
                                srcChannel.close();
                                dstChannel.close();
                            } catch (IOException e) {
                                e.printStackTrace();
                            }
                            if (deleteFlag)
                                srcLongFName.delete();
                        }
                        Path path = Paths.get(dstLongFName.toString());
                        FileTime stamp = FileTime.fromMillis(longDate);
                        try {
                            Files.getFileAttributeView(path, BasicFileAttributeView.class).setTimes(stamp, stamp, stamp);
//                            Files.setAttribute(path, "lastAccessTime", stamp);
//                            Files.setAttribute(path, "lastModifiedTime", stamp);
//                            Files.setAttribute(path, "basic:creationTime", stamp);
                        } catch (IOException ex) {
                            ex.printStackTrace();
                        }

                    }
                }
            }
            return null;
        }

        @Override
        protected void onProgressUpdate(Integer... values) {
            int currIdx = values[0];
            srcFiles[currIdx] = dstLongFName;
            result.setText(update_SrcFiles(currIdx));
        }

        @Override
        protected void onPostExecute(final Void statistics) {
            result.setText(update_SrcFiles(-1));
            String s = (renameFlag) ? "Renaming " : "Copying" + " Completed";
            Toast.makeText(mContext, s, Toast.LENGTH_LONG).show();
            listUp_srcFiles();
        }
    }

    private SpannableString update_SrcFiles(int currIdx) {
        int sPos = 0, fPos = 0;
        StringBuilder sb = new StringBuilder();
        sb.append("\n");
        for (int idx = 0; idx < srcFiles.length; idx++) {
            String nowShortName = srcFiles[idx].getName();
            if (!nowShortName.substring(0,1).equals(".")) {
                if (currIdx == idx)
                    sPos = sb.length();
                sb.append(nowShortName).append(" ");
                sb.append(size2String(sizes[idx]));
                if (currIdx == idx)
                    fPos = sb.length();
                sb.append("\n");
            }
        }
        SpannableString ss = new SpannableString(sb);
        ss.setSpan(new ForegroundColorSpan(Color.BLUE), sPos, fPos, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
        if (sPos > 0) {
            ss.setSpan(new ForegroundColorSpan(Color.GRAY), 0, sPos, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
            ss.setSpan(new StrikethroughSpan(), 0, sPos, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
        }
        ss.setSpan(new StyleSpan(Typeface.BOLD), sPos, fPos, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
        ss.setSpan(new RelativeSizeSpan(1.2f), sPos, fPos, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
        return ss;
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (renameFlag)
            dstFullPath = srcFullPath;
        else
            dstFullPath = new File(cameraFullPath, dstFolder);
        readyFolder(srcFullPath);
        readyFolder(dstFullPath);
        listUp_srcFiles();
        showSetting();
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
        nowRunning = false;
    }

    // ↓ ↓ ↓ P E R M I S S I O N   RELATED /////// ↓ ↓ ↓ ↓  BEST CASE 20/09/27 with no lambda
    private final static int ALL_PERMISSIONS_RESULT = 101;
    ArrayList permissionsToRequest;
    ArrayList<String> permissionsRejected = new ArrayList<>();
    String [] permissions;

    private void askPermission() {
        try {
            PackageInfo info = getPackageManager().getPackageInfo(getApplicationContext().getPackageName(), PackageManager.GET_PERMISSIONS);
            permissions = info.requestedPermissions;//This array contain
        } catch (Exception e) {
            Log.e("Permission", "Not done", e);
        }

        permissionsToRequest = findUnAskedPermissions();
        if (permissionsToRequest.size() != 0) {
            requestPermissions((String[]) permissionsToRequest.toArray(new String[0]),
//            requestPermissions(permissionsToRequest.toArray(new String[permissionsToRequest.size()]),
                    ALL_PERMISSIONS_RESULT);
        }
    }

    private ArrayList findUnAskedPermissions() {
        ArrayList <String> result = new ArrayList<String>();
        for (String perm : permissions) if (hasPermission(perm)) result.add(perm);
        return result;
    }
    private boolean hasPermission(String permission) {
        return (checkSelfPermission(permission) != PackageManager.PERMISSION_GRANTED);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        if (requestCode == ALL_PERMISSIONS_RESULT) {
            for (Object perms : permissionsToRequest) {
                if (hasPermission((String) perms)) {
                    permissionsRejected.add((String) perms);
                }
            }
            if (permissionsRejected.size() > 0) {
                if (shouldShowRequestPermissionRationale(permissionsRejected.get(0))) {
                    String msg = "These permissions are mandatory for the application. Please allow access.";
                    showDialog(msg);
                }
                if (shouldShowRequestPermissionRationale(permissionsRejected.get(0))) {
                    String msg = "These permissions are mandatory for the application. Please allow access.";
                    showDialog(msg);
                }
            }
        }
    }
    private void showDialog(String msg) {
        showMessageOKCancel(msg,
                new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        MainActivity.this.requestPermissions(permissionsRejected.toArray(
                                new String[0]), ALL_PERMISSIONS_RESULT);
                    }
                });
    }
    private void showMessageOKCancel(String message, DialogInterface.OnClickListener okListener) {
        new AlertDialog.Builder(mActivity)
                .setMessage(message)
                .setPositiveButton("OK", okListener)
                .setNegativeButton("Cancel", null)
                .create()
                .show();
    }

// ↑ ↑ ↑ ↑ P E R M I S S I O N    RELATED /////// ↑ ↑ ↑

}