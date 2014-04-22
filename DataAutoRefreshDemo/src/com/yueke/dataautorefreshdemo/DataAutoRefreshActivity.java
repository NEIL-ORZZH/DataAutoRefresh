package com.yueke.dataautorefreshdemo;

import java.util.ArrayList;

import android.app.Activity;
import android.os.Bundle;

import com.yueke.dataautorefresh.DataAutoRefresh;
import com.yueke.dataautorefresh.DataAutoRefresh.OnAutoRefreshListener;

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
    public ArrayList<String> onGetExistDataList() {
        // 得到模块内已经存在的数据列表
        return null;
    }

    @Override
    public void onDataRefresh(ArrayList<String> dataList) {
        // 将媒体库新增的数据添加到模块内
    }

    @Override
    public void onDataScan( ){
        // 全盘扫描模块所支持的数据
    }
    
    private void findAllView( ){
        mDataAutoRefresh = new DataAutoRefresh( this, mSupportSuffix);
        mDataAutoRefresh.setOnAutoRefreshListener( this );
    }
    
    private void showContent( ){
        
    }
    
    private void unRegisterRefreshListener( ){
        if( null != mDataAutoRefresh ){
            mDataAutoRefresh.unregisterDataAutoRefresh( ); 
        }
    }
    
    // 模块支持的数据格式后缀
    private static final String[] mSupportSuffix = new String[]{"mp3","wav"};
    private DataAutoRefresh mDataAutoRefresh = null;
}
