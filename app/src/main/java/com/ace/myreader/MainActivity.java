package com.ace.kernellog;

import android.Manifest;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Handler;
import android.os.SystemClock;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.text.method.ScrollingMovementMethod;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import java.io.DataInputStream;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;

public class MainActivity extends AppCompatActivity {

    TextView textView;
    ScrollView scrollView;
    Thread thread;
    private Handler handler;
    String oldLine;
    StringBuilder logInfo;
    boolean freshLog;
    long lastClickTime,clickTime;
    final String logPath = "/sdcard/";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        textView = (TextView)findViewById(R.id.textView);
        textView.setText("No permissions");
        textView.setMovementMethod(ScrollingMovementMethod.getInstance());
        scrollView = (ScrollView)findViewById(R.id.SCROLLER_ID);
        logInfo =new StringBuilder();
        freshLog = false;
        oldLine = "";
        lastClickTime = 0l;

        textView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                clickTime = System.currentTimeMillis();
                if(clickTime - lastClickTime < 500 ) {
                    logInfo.append("\n\n\n\n\n");
                    handler.post(showUI);
                }
                lastClickTime = clickTime;
            }
        });

        thread = new Thread() {
            public void run() {
                while (true) {
                    getlog();
                    if(freshLog) {
                        handler.post(showUI);
                        Log.d("kernellog", "get new message");
                        freshLog = false;
                    }
                    try {
                        sleep(600);
                    } catch (InterruptedException e) {
                        Log.e("kernellog","some error occured");
                        e.printStackTrace();
                    }
                }
            }
        };
        handler = new Handler();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        menu.add(1,1,1,"clear");
        menu.add(1,2,2,"save");
        return super.onCreateOptionsMenu(menu);
    }

    public boolean onOptionsItemSelected(MenuItem item) {
        if(item.getItemId() == 1) {
            logInfo.delete(0, logInfo.length());
            textView.setText("");
        } else if (item.getItemId() ==2 ) {
            // check file read permission
            if (Build.VERSION.SDK_INT >= 23) {
                int REQUEST_CODE_CONTACT = 101;
                String[] permissions = {Manifest.permission.READ_EXTERNAL_STORAGE};
                //check if permission allowed
                for (String str : permissions) {
                    if (this.checkSelfPermission(str) != PackageManager.PERMISSION_GRANTED) {
                        this.requestPermissions(permissions, REQUEST_CODE_CONTACT);
                        Toast.makeText(this,"Can not get write sdcard permissions!",Toast.LENGTH_SHORT).show();
                        return false;
                    }
                }
            }

            String path = logPath + "log_" + SystemClock.elapsedRealtime();
            File file = new File(path);
            try {
                if(!file.exists()) {
                    file.createNewFile();
                }
                FileWriter fw = new FileWriter(path);
                fw.write(logInfo.toString());
                fw.close();
            } catch (Exception e) {
                e.printStackTrace();
            } finally {
                Toast.makeText(this,"write file to "+path,Toast.LENGTH_SHORT).show();
            }
        }
        return false;
    }
    @Override
    protected void onStart() {
        super.onStart();

        thread.start();
    }

    @Override
    protected void onStop() {
        super.onStop();

        thread.interrupt();
    }

    Runnable showUI = new Runnable() {
        @Override
        public void run() {
            textView.setText(logInfo.toString());

            Log.d("reader","1= "+scrollView.getScrollY()+",2 = "+scrollView.getHeight()+",3 = "+textView.getMeasuredHeight());
            if(scrollView.getScrollY() + scrollView.getHeight() - textView.getMeasuredHeight() + 500 > 0 )
                handler.post(jumpButtom);
        }
    };

    Runnable jumpButtom = new Runnable() {
        @Override
        public void run() {
            scrollView.smoothScrollTo(0,textView.getBottom());
        }
    };

    protected String getlog() {
        String line;
        String templine = "";
        boolean flag = false;

        try {
            //Process process = Runtime.getRuntime().exec("sh");
            Process process = Runtime.getRuntime().exec("dmesg");
            InputStream inputStream = process.getInputStream();
            DataInputStream dataInputStream = new DataInputStream(inputStream);

//            OutputStream outputStream = process.getOutputStream();
//            DataOutputStream dataOutputStream = new DataOutputStream(outputStream);
//            dataOutputStream.writeBytes("dmesg");
//            dataOutputStream.flush();
            if(oldLine.equals(""))
                flag = true;

            while ((line = dataInputStream.readLine()) != null) {
                if(line.equals(oldLine)) {
                    flag = true;
                    templine = line;
                    continue;
                }
                if(flag) {
                    templine = line;
//                    if(logInfo.length() > 2000)
//                        logInfo.delete(0,1000);
                    logInfo.append(line + "\n");
                }
            }
            if (!oldLine.equals(templine)) {
                freshLog = true;
                oldLine = templine;
            }


//            BufferedReader bufferedReader = new BufferedReader(
//                    new InputStreamReader(process.getInputStream()));
//
//            StringBuilder log=new StringBuilder();
//            String line;
//            while ((line = bufferedReader.readLine()) != null) {
//                log.append(line);
//            }
//            textView.setText(log.toString());
        } catch (IOException e) {
            e.printStackTrace();
        }
//        Log.d("kernellog","length=" + logInfo.length());
        return logInfo.toString();
    }
}
