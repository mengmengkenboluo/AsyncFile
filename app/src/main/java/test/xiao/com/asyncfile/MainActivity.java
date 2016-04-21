package test.xiao.com.asyncfile;

import android.app.Activity;
import android.os.AsyncTask;
import android.os.Environment;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.Toast;
import org.apache.http.client.ClientProtocolException;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.RandomAccessFile;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;


public class MainActivity extends Activity implements View.OnClickListener{
    private ProgressBar progressBar;
    private Button bnDown;
    private Button bnStop;
    private EditText etUrl;
    private DownAsync downAsync;

    private static final String TAG ="MainActivity";
    //文件存放路径
    private String downFilePath =Environment.getExternalStorageDirectory()
            +File.separator+"beibei";
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        initView();
    }

    /**
     * 初始化View
     */
    private void initView() {
        progressBar = (ProgressBar) findViewById(R.id.progressBar_now);
        etUrl = (EditText) findViewById(R.id.targetURL);
        bnDown = (Button) findViewById(R.id.button_download);
        bnStop = (Button) findViewById(R.id.button_download_stop);
        bnDown.setOnClickListener(this);
        bnStop.setOnClickListener(this);
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()){
            //下载按钮
            case R.id.button_download:
                //一个AsyncTask只能被执行一次，否则会抛异常
                if (downAsync!=null&&!downAsync.isCancelled()){
                    return;
                }
                downAsync = new DownAsync(etUrl.getText().toString());
                downAsync.execute();
                break;
            //暂停下载按钮
            case R.id.button_download_stop:
                //如果AsyncTask正在执行
                if (downAsync!=null && downAsync.getStatus()== AsyncTask.Status.RUNNING){
                    downAsync.cancel(true);
                }
                break;
        }

    }

    /**
     * 1.启动任务执行的输入参数类型
     * 2.后台任务完成的进度值类型
     * 3.后台任务执行完成后返回的结果类型
     */
     private class DownAsync extends AsyncTask<URL,Integer,Integer>{
        private static final String TAG="DownAsync";

        private String path;
        //文件总大小
        private int total;
        public DownAsync(String path){
            this.path = path;
        }
        //执行后台下载任务
        @Override
        protected Integer doInBackground(URL... params) {
            try {
                //总文件位置
                File dir = new File(downFilePath);
                //文件不存在
                if (!dir.exists()){
                    dir.mkdir();
                }
                URL url = new URL(path);

                HttpURLConnection httpURLConnection = (HttpURLConnection) url.openConnection();
                httpURLConnection.setConnectTimeout(5*1000);
                httpURLConnection.setRequestMethod("GET");
                httpURLConnection.setRequestProperty("Accept","image/gif,image/jpeg,image/pjpeg,"+
                        "application/x-shockwave-flash,application/xaml+xml,"
                        +"application/vnd.ms-xpsdocument,application/x-ms-xnap,"
                        +"application/x-ms-application,application/vnd.ms-excel,"
                        +"application/vnd.ms-powerpoint,application/msword,*/*");
                httpURLConnection.setRequestProperty("Accept","zh-CN");
                httpURLConnection.setRequestProperty("Charset","UTF-8");
                httpURLConnection.setRequestProperty("Connection","Keep-Alive");
                httpURLConnection.setRequestProperty("Accept-Encoding", "identity");

                File file = new File(downFilePath
                        +File.separator+path.substring(path.lastIndexOf("/")+1));
                //已经获取到的数据
                long readSize = file.length();
                Log.d(TAG,"readSize"+readSize);
                httpURLConnection.setRequestProperty("Range","byte="+readSize+"-");
                InputStream inputStream = httpURLConnection.getInputStream();
                OutputStream outputStream = null;
                RandomAccessFile currentFile = null;
                //获取文件总大小，用于计算进度
                total = httpURLConnection.getContentLength();
                if (!file.exists()){
                    try {
                        outputStream = new FileOutputStream(file);
                        //  当且仅当不存在具有此抽象路径名指定名称的文件时
                        // ，不可分地创建一个新的空文件。
                        file.createNewFile();
                        byte buffer [] = new byte[1024];
                        int inputsize = -1;
                        //已下载大小
                        int count = 0;
                        while ((inputsize=inputStream.read(buffer)) !=-1){
                            outputStream.write(buffer,0,inputsize);
                            count +=inputsize;
                            //更新进度
                            this.publishProgress((int)(count/(float)total)/100);
                            //一旦任务被取消则退出循环，否则一直执行，直到结束
                            if (isCancelled()){
                                outputStream.flush();
                                return count;
                            }

                        }
                        outputStream.flush();
                    } catch (FileNotFoundException e) {
                        e.printStackTrace();
                    } catch (ClientProtocolException e) {
                        e.printStackTrace();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
                //文件存在
                else if (readSize!=total){
                    try {
                        currentFile = new RandomAccessFile(file,"rw");
                        //跳过已经获取过的数据
                        currentFile.seek(readSize);
                        byte buffer[] = new byte[1024];
                        int inputSize =-1;
                        int count = (int) readSize;


                        //跳过已经获取过的数据
                        inputStream.skip(readSize);
                        while ((inputSize=inputStream.read(buffer)) !=-1){
                            currentFile.write(buffer,0,inputSize);
                            count +=inputSize;
                            this.publishProgress((int)((count/(float)total)*100));
                            if (isCancelled()){
                                return count;
                            }
                        }
                    } catch (IOException e) {
                        e.printStackTrace();
                    }finally {
                        if (inputStream !=null){
                            try {
                                inputStream.close();
                            } catch (IOException e) {
                                e.printStackTrace();
                            }
                            if (outputStream !=null){
                                try {
                                    outputStream.close();
                                } catch (IOException e) {
                                    e.printStackTrace();
                                }
                            }
                            if (currentFile !=null){
                                try {
                                    currentFile.close();
                                } catch (IOException e) {
                                    e.printStackTrace();
                                }
                            }
                        }
                    }

                }


            } catch (MalformedURLException e) {
                e.printStackTrace();
            } catch (IOException e) {
                e.printStackTrace();
            }
            Log.d(TAG,"total"+total);
            return total;
        }


        //doBackGround执行前
        @Override
        protected void onPreExecute() {
            Log.d(TAG, "download begin ");
           Toast.makeText(MainActivity.this, "开始下载", Toast.LENGTH_SHORT).show();
        }
        //更新进度条
        @Override
        protected void onProgressUpdate(Integer... values) {
            //更新界面进度条
            progressBar.setProgress(values[0]);
        }
        //doInBackground结束后调用此方法
        @Override
        protected void onPostExecute(Integer result) {
            Log.d(TAG,"result===="+result);
        }
    }
}
