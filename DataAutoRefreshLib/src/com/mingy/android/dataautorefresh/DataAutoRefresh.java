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
 * ģ�������Զ�����
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
    
    // ���ڱ�����ֹͣ��̨����
    public void onPause( ){
        stopTimer( );
    }
    
    // ���ؽ���ָ���̨����
    public void onResume( ){
        startCheckFileTimer( );
    }
    
    /**
     * ע���㲥
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
     * ����USB��״̬������ģ���������Ϣ
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
        mContext.registerReceiver( mBroadcastReceiver, intentFilter );//ע���������
    }
    
    /**
     * ý�����ݿ����۲���
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
     * ��ý����л�ȡָ����׺���ļ��б�
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
            System.out.println("Cursor ��ȡʧ��!");
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
     * �õ�ý�����µ��ļ�
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
     * ȫ��ɨ���ļ�
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
     * ȫ��ɨ���ļ�
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
     * �ļ��������ķ���
     * 
     * */
    private void searchFile(ArrayList<String> filePathList, String[] keywords, File filepath) {
        // �ж�SD���Ƿ����
        if (Environment.getExternalStorageState().equals(Environment.MEDIA_MOUNTED)) {
            File[] files = filepath.listFiles();
            if (null != files && files.length > 0) {
                for (File file : files) {
                    if (file.isDirectory()) {
                        // ���Ŀ¼�ɶ���ִ�У�һ��Ҫ�ӣ���Ȼ��ҵ���
                        if (file.canRead()) {
                            searchFile(filePathList, keywords, file); // �����Ŀ¼���ݹ����
                        }
                    } else {
                        // �ж����ļ���������ļ����ж�
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
     * ģ��ˢ�½ӿ�
     * 
     * */
    public interface OnAutoRefreshListener{
        public void onDataRefresh( ArrayList<String> fileList );// ��������������ӵ�ģ��
    }
    
    private static final int CHECK_FILE_TIME_LEN = 5 * 1000;// ���ý���ʱ����
    private static final String NULL_POINTER_EXCEPTION = "���Ϊ�գ�";
    
    private boolean mTimerWorking = false;
    private Context mContext = null;
    private String[] mSupportSuffix = null;
    private BroadcastReceiver mBroadcastReceiver = null;
    private MediaStoreChangeObserver mMediaStoreChangeObserver = null;
    private OnAutoRefreshListener mAutoRefreshListener = null;
    private Timer mCheckFileTimer = null;
    private Timer mSearchFileTimer = null;
}
