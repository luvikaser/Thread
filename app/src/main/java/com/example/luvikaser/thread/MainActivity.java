package com.example.luvikaser.thread;

import android.app.Activity;
import android.os.AsyncTask;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Message;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import java.lang.ref.WeakReference;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

public class MainActivity extends AppCompatActivity {
    private static final int MAX_PROGRESS = 10;
    private static final int TIME_SLEEPING = 1000;
    private static final int MIN_POOL_SIZE = 2;
    private static final int MAX_POOL_SIZE = 2;
    private static final long KEEP_ALIVE_TIME = 60L;
    private static final TimeUnit TIME_UNIT = TimeUnit.SECONDS;
    private static final int MSG_SET_DATA = 1234;
    private ProgressBar mProgressBar1;
    private ProgressBar mProgressBar2;

    private Button mButtonAsyncTask;
    private Button mButtonHandlerThread;
    private Button mButtonThreadPoolExecutor;

    private static HandlerThread mHandlerThread = null;
    private static Handler mHandler = null;
    private static Handler mHandlerUI = null;

    private static ThreadPoolExecutor mExecutor = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mProgressBar1 = (ProgressBar) findViewById(R.id.progressBar1);
        mProgressBar2 = (ProgressBar) findViewById(R.id.progressBar2);

        mButtonAsyncTask = (Button) findViewById(R.id.asyncTask);
        mButtonHandlerThread = (Button) findViewById(R.id.handlerThread);
        mButtonThreadPoolExecutor = (Button) findViewById(R.id.threadPoolExecutor);

        mHandlerUI = new Handler(){
            @Override
            public void handleMessage(Message msg) {
                switch (msg.what){
                    case MSG_SET_DATA:
                        ((ProgressBar)msg.obj).setProgress(msg.arg1);
                        break;
                    default:
                        super.handleMessage(msg);
                }
            }
        };

        mButtonAsyncTask.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                mProgressBar1.setMax(MAX_PROGRESS);
                mProgressBar2.setMax(MAX_PROGRESS);

                new MyTask(mProgressBar1).execute(MAX_PROGRESS);
                new MyTask(mProgressBar2).execute(MAX_PROGRESS);

            }});

        mButtonHandlerThread.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View view) {
                mProgressBar1.setMax(MAX_PROGRESS);
                mProgressBar2.setMax(MAX_PROGRESS);

                mHandlerThread = new HandlerThread("myHandlerThread");
                mHandlerThread.start();

                mHandler = new Handler(mHandlerThread.getLooper()){
                    @Override
                    public void handleMessage(Message msg) {
                        switch (msg.what){
                            case MSG_SET_DATA:
                                for (int i = 0; i <= MAX_PROGRESS; ++i) {
                                    try {
                                        Message msg1 = mHandlerUI.obtainMessage(MSG_SET_DATA);
                                        msg1.arg1 = i;
                                        msg1.obj = ((WeakReference<ProgressBar>)msg.obj).get();
                                        msg1.sendToTarget();

                                        Thread.sleep(TIME_SLEEPING);
                                    } catch (InterruptedException e) {
                                        e.printStackTrace();
                                    }
                                }
                                break;
                            default:
                                super.handleMessage(msg);
                        }
                    }
                };

                mHandler.post(new MyRunnable(mProgressBar1));
                Message msg = mHandler.obtainMessage(MSG_SET_DATA);
                msg.obj = new WeakReference<ProgressBar>(mProgressBar2);
                msg.sendToTarget();
            }
        });

        mButtonThreadPoolExecutor.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                mProgressBar1.setMax(MAX_PROGRESS);
                mProgressBar2.setMax(MAX_PROGRESS);

                mExecutor = new ThreadPoolExecutor(MIN_POOL_SIZE, MAX_POOL_SIZE, KEEP_ALIVE_TIME, TIME_UNIT, new LinkedBlockingQueue<Runnable>());

                mExecutor.execute(new MyRunnable(mProgressBar1));
                mExecutor.execute(new MyRunnable(mProgressBar2));
            }
        });

    }

    private class MyRunnable implements Runnable {
        private ProgressBar progressBar = null;
        public MyRunnable(ProgressBar mProgressBar) {
            WeakReference<ProgressBar> weakProgressBar = new WeakReference<ProgressBar>(mProgressBar);
            if (weakProgressBar != null)
                progressBar = weakProgressBar.get();
        }

        @Override
        public void run() {
            if (progressBar != null) {
                for (int i = 0; i <= MAX_PROGRESS; ++i) {
                    try {
                        Message msg = mHandlerUI.obtainMessage(MSG_SET_DATA);
                        msg.arg1 = i;
                        msg.obj = progressBar;
                        msg.sendToTarget();

                        Thread.sleep(TIME_SLEEPING);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }
            }
        }
    }

    private static  class MyTask extends AsyncTask<Integer, Integer, Void>{
        private ProgressBar mProgressBar = null;

        public MyTask(ProgressBar progressBar){
            WeakReference<ProgressBar> weakProgressBar = new WeakReference<ProgressBar>(progressBar);
            if (weakProgressBar != null){
                this.mProgressBar = weakProgressBar.get();
            }
        }
        @Override
        protected Void doInBackground(Integer... integers) {
            if (mProgressBar == null)
                return null;
            for(int i = 0; i <= integers[0]; ++i)
                try {
                    publishProgress(i);
                    Thread.sleep(TIME_SLEEPING);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            return null;
        }

        @Override
        protected void onProgressUpdate(Integer... values) {
            mProgressBar.setProgress(values[0]);
            super.onProgressUpdate(values);
        }

    }

    @Override
    protected void onStop() {
        super.onStop();
        if (mHandlerThread != null) {
            mHandlerThread.quitSafely();
            mHandlerThread = null;
        }

        if (mExecutor != null){
            mExecutor.shutdownNow();
        }
    }

}
