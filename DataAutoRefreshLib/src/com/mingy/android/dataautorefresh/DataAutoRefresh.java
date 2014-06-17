package com.mingy.android.dataautorefresh;

import java.io.File;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.Timer;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.database.ContentObserver;
import android.database.Cursor;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Environment;
import android.os.Handler;
import android.provider.MediaStore;

/**
 * 模块数据自动更新
 * 
 * */
public class DataAutoRefresh {
    public DataAutoRefresh( Context context, String[] supportSuffix ) throws NullPointerException{
        if( null == context || null == supportSuffix ){
            throw new NullPointerException( NULL_POINTER_EXCEPTION );
        }
        
        mContext = context;
        mSupportSuffix = supportSuffix;
        
        initDataAutoRefresh( );
    }
    
    public void setOnAutoRefreshListener( OnAutoRefreshListener autoRefreshListener ) throws NullPointerException{
        if( null == autoRefreshListener ){
            throw new NullPointerException( NULL_POINTER_EXCEPTION );
        }
        
        mAutoRefreshListener = autoRefreshListener;
    }
    
    // 不在本界面停止后台检索
    public void onPause( ){
        stopTimer( );
    }
    
    // 返回界面恢复后台检索
    public void onResume( ){
        startCheckFileTimer( );
    }
    
    /**
     * 注销广播
     * 
     * */
    public void unregisterDataAutoRefresh( ) throws NullPointerException{
        if( null == mBroadcastReceiver || null == mMediaStoreChangeObserver || null == mContext ){
            throw new NullPointerException( NULL_POINTER_EXCEPTION );
        }
        mContext.unregisterReceiver( mBroadcastReceiver );
        mContext.getContentResolver( ).unregisterContentObserver( mMediaStoreChangeObserver );
        stopTimer( );
    }
    
    private void initDataAutoRefresh( ){
        startMediaFileListener( );
        observerMediaStoreChange( );
        startCheckFileTimer( );
    }
    
    private void observerMediaStoreChange( ){
        if( null == mMediaStoreChangeObserver ){
            mMediaStoreChangeObserver = new MediaStoreChangeObserver( );
        }
        mContext.getContentResolver( ).registerContentObserver( MediaStore.Files.getContentUri("external"), false, mMediaStoreChangeObserver );
    }
    
    /**
     * 监听USB的状态，更新模块的数据信息
     * 
     * */
    private void startMediaFileListener( ){
        if( null != mBroadcastReceiver ){
            return;
        }
        
        IntentFilter intentFilter = new IntentFilter( );
        intentFilter.addAction( Intent.ACTION_MEDIA_SCANNER_FINISHED );
        intentFilter.addAction( Intent.ACTION_MEDIA_MOUNTED );
        intentFilter.addAction( Intent.ACTION_MEDIA_EJECT );
        intentFilter.addDataScheme( "file" );
          
        mBroadcastReceiver = new BroadcastReceiver(){
            @Override
            public void onReceive(Context context,Intent intent){
                String action = intent.getAction( );
                if( Intent.ACTION_MEDIA_SCANNER_FINISHED.equals( action ) ){
                    mTimerWorking = false;
                    startCheckFileTimer( );
                }else if( action.equals( Intent.ACTION_MEDIA_MOUNTED ) ){
                    startSearchFileTimer( );
                }else if( action.equals( Intent.ACTION_MEDIA_EJECT ) ){
                    startSearchFileTimer( );
                }
            }
        };
        mContext.registerReceiver( mBroadcastReceiver, intentFilter );//注册监听函数
    }
    
    /**
     * 媒体数据库变更观察类
     * 
     * */
    class MediaStoreChangeObserver extends ContentObserver{
        public MediaStoreChangeObserver( ) {
            super( new Handler( ) );
        }

        @Override
        public void onChange(boolean selfChange) {
            super.onChange(selfChange);
            startCheckFileTimer( );
        }
    }
    
    private void startCheckFileTimer( ){
        if( mTimerWorking ){
            return;
        }
        
        mCheckFileTimer = new Timer( );
        mCheckFileTimer.schedule( new CheckFileChangeTimerTask( ), CHECK_FILE_TIME_LEN );
        mTimerWorking = true;
    }
    
    private void stopCheckFileTimer( ){
        if( null != mCheckFileTimer ){
            mCheckFileTimer.cancel( );
            mCheckFileTimer = null;
        }
    }
    
    private void startSearchFileTimer( ){
        if( mTimerWorking ){
            return;
        }
        
        mSearchFileTimer = new Timer( );
        mSearchFileTimer.schedule( new SearchFileTimerTask( ), CHECK_FILE_TIME_LEN );
        mTimerWorking = true;
    }
    
    private void stopSearchFileTimer( ){
        if( null != mCheckFileTimer ){
            mCheckFileTimer.cancel( );
            mCheckFileTimer = null;
        }
    }
    
    private void stopTimer( ){
        stopCheckFileTimer( );
        stopSearchFileTimer( );
    }
    
    /**
     * 从媒体库中获取指定后缀的文件列表
     * 
     * */
    public ArrayList<String> getSupportFileList( Context context, String[] searchFileSuffix ) {
        ArrayList<String> searchFileList = null;
        if( null == context || null == searchFileSuffix || searchFileSuffix.length == 0 ){
            return null;
        }
        
        String searchPath = "";
        int length = searchFileSuffix.length;
        for( int index = 0; index < length; index++ ){
            searchPath += ( MediaStore.Files.FileColumns.DATA + " LIKE '%" + searchFileSuffix[ index ] + "' " );
            if( ( index + 1 ) < length ){
                searchPath += "or ";
            }
        }
        
        searchFileList = new ArrayList<String>();
        Uri uri = MediaStore.Files.getContentUri("external");
        Cursor cursor = context.getContentResolver().query(
                uri, new String[] { MediaStore.Files.FileColumns.DATA },
                searchPath, null, null);
        
        String filepath = null;
        if (cursor == null) {
            System.out.println("Cursor 获取失败!");
        } else {
            if (cursor.moveToFirst()) {
                do {
                    filepath = cursor.getString(cursor.getColumnIndex(MediaStore.Files.FileColumns.DATA));
                    try {
                        searchFileList.add(new String(filepath.getBytes("UTF-8")));
                    } catch (UnsupportedEncodingException e) {
                        e.printStackTrace();
                    }

                } while (cursor.moveToNext());
            }

            if (!cursor.isClosed()) {
                cursor.close();
            }
        }

        return searchFileList;
    }
    
    /**
     * 得到媒体库更新的文件
     * 
     * */
    class GetMediaStoreDataTask extends AsyncTask< Void , Void , Void>{
        @Override
        protected Void doInBackground(Void... arg0) {
            ArrayList<String> supportFileList = getSupportFileList( mContext, mSupportSuffix );
            if( null != supportFileList && supportFileList.size( ) > 0 ){
                mAutoRefreshListener.onDataRefresh( supportFileList );
            }
            mTimerWorking = false;
            
            return null;
        }
    }
    
    class CheckFileChangeTimerTask extends java.util.TimerTask{
        @Override
        public void run() {
            new GetMediaStoreDataTask( ).execute( );
        }
    }
    
    class SearchFileTimerTask extends java.util.TimerTask{
        @Override
        public void run() {
            new SearchFileTask( ).execute( );
        }
    }
    
    /**
     * 全盘扫描文件
     * 
     * */
    class SearchFileTask extends AsyncTask< Void , Void , Void>{
        @Override
        protected Void doInBackground(Void... arg0) {
            ArrayList<String> supportFileList = searchFile( mSupportSuffix );
            if( null != supportFileList && supportFileList.size( ) > 0 ){
                mAutoRefreshListener.onDataRefresh( supportFileList );
            }
            mTimerWorking = false;
            
            return null;
        }
    }
    
    /**
     * 全盘扫描文件
     * 
     * */
    private ArrayList<String> searchFile( String[] searchFileSuffix ){
        ArrayList<String> filePathList = new ArrayList<String>( );
        
        File flashFile = new File(Environment.getExternalStorageDirectory( ).toString( ) + "/");
        searchFile(filePathList, searchFileSuffix, flashFile);
        File sdFile = new File(Environment.getExternalFlashStorageDirectory( ).toString( ) + "/");
        if (null != sdFile) {
            searchFile(filePathList, searchFileSuffix, sdFile);
        }
        
        return filePathList;
    }
    
    /**
     * 文件检索核心方法
     * 
     * */
    private void searchFile(ArrayList<String> filePathList, String[] keywords, File filepath) {
        // 判断SD卡是否存在
        if (Environment.getExternalStorageState().equals(Environment.MEDIA_MOUNTED)) {
            File[] files = filepath.listFiles();
            if (null != files && files.length > 0) {
                for (File file : files) {
                    if (file.isDirectory()) {
                        // 如果目录可读就执行（一定要加，不然会挂掉）
                        if (file.canRead()) {
                            searchFile(filePathList, keywords, file); // 如果是目录，递归查找
                        }
                    } else {
                        // 判断是文件，则进行文件名判断
                        for( String keyword : keywords ){
                            if (file.getName().indexOf(keyword) > -1 || file.getName().indexOf( keyword.toUpperCase()) > -1) {
                                filePathList.add(file.getPath());
                            }
                        }
                    }
                }
            }
        }
    }
    
    /**
     * 模块刷新接口
     * 
     * */
    public interface OnAutoRefreshListener{
        public void onDataRefresh( ArrayList<String> fileList );// 将新增的数据添加到模块
    }
    
    private static final int CHECK_FILE_TIME_LEN = 5 * 1000;// 检查媒体库时间间隔
    private static final String NULL_POINTER_EXCEPTION = "入参为空！";
    
    private boolean mTimerWorking = false;
    private Context mContext = null;
    private String[] mSupportSuffix = null;
    private BroadcastReceiver mBroadcastReceiver = null;
    private MediaStoreChangeObserver mMediaStoreChangeObserver = null;
    private OnAutoRefreshListener mAutoRefreshListener = null;
    private Timer mCheckFileTimer = null;
    private Timer mSearchFileTimer = null;
}
