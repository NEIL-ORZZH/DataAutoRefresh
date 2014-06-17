package com.mingy.android.dataautorefreshdemo;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import android.app.Activity;
import android.os.Bundle;
import android.widget.ListView;
import android.widget.SimpleAdapter;
import android.widget.TextView;

import com.mingy.android.dataautorefresh.DataAutoRefresh;
import com.mingy.android.dataautorefresh.DataAutoRefresh.OnAutoRefreshListener;
import com.yueke.dataautorefreshdemo.R;

public class DataAutoRefreshActivity extends Activity implements OnAutoRefreshListener {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_data_auto_refresh);
        
        findAllView( );
        showContent( );
    }
    
    @Override
    protected void onDestroy() {
        super.onDestroy( );
        
        unRegisterRefreshListener( );
    }
    
    @Override
    protected void onPause() {
        super.onPause();
        if( null != mDataAutoRefresh ){
            mDataAutoRefresh.onPause( );
        }
    }
    
    @Override
    protected void onResume() {
        super.onResume();
        if( null != mDataAutoRefresh ){
            mDataAutoRefresh.onResume( );
        }
    }

    
    @Override
    public void onDataRefresh(ArrayList<String> fileList) {
        // 将媒体库新增的数据添加到模块内
        updateFileList( fileList );
    }
    
    private void updateFileList(ArrayList<String> fileList){
        if( null == fileList ){
            return;
        }
        
        mFilePathList.clear( );
        for( String filePath : fileList ){
            addItem( mFilePathList, filePath );
        }
        
        DataAutoRefreshActivity.this.runOnUiThread( new Runnable() {
            @Override
            public void run() {
                mFileAdapter.notifyDataSetChanged( );
                
                SimpleDateFormat updateTime = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
                mUpdateTxt.setText( "更新时间：" + updateTime.format( new Date( ) ) );
            }
        });
    }
    
    private void findAllView( ){
        mFileList = ( ListView )findViewById( R.id.fileListId );
        mUpdateTxt = ( TextView )findViewById( R.id.updateTxtId );
        
        mDataAutoRefresh = new DataAutoRefresh( this, mSupportSuffix);
        mDataAutoRefresh.setOnAutoRefreshListener( this );
    }
    
    private void showContent( ){
        mFilePathList = new ArrayList<Map<String, String>>( );
        mFileAdapter = new SimpleAdapter(this, mFilePathList,
                android.R.layout.simple_list_item_1, new String[] { MAP_KEY_PATH },
                new int[] { android.R.id.text1 });
        
        mFileList.setAdapter( mFileAdapter );
    }
    
    private void addItem(List<Map<String, String>> data, String filePath) {
        Map<String, String> temp = new HashMap<String, String>();
        temp.put(MAP_KEY_PATH, filePath);
        
        data.add(temp);
    }
    
    private void unRegisterRefreshListener( ){
        if( null != mDataAutoRefresh ){
            mDataAutoRefresh.unregisterDataAutoRefresh( ); 
        }
    }
    
    // 模块支持的数据格式后缀
    private static final String[] mSupportSuffix = new String[]{"mp3","wav"};
    private static final String MAP_KEY_PATH = "filePath";
    private DataAutoRefresh mDataAutoRefresh = null;
    
    private SimpleAdapter mFileAdapter = null;
    private ArrayList<Map<String, String>> mFilePathList = null;
    private TextView mUpdateTxt = null;
    private ListView mFileList = null;
}
