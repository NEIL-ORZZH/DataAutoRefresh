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
        // �õ�ģ�����Ѿ����ڵ������б�
        return null;
    }

    @Override
    public void onDataRefresh(ArrayList<String> dataList) {
        // ��ý���������������ӵ�ģ����
    }

    @Override
    public void onDataScan( ){
        // ȫ��ɨ��ģ����֧�ֵ�����
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
    
    // ģ��֧�ֵ����ݸ�ʽ��׺
    private static final String[] mSupportSuffix = new String[]{"mp3","wav"};
    private DataAutoRefresh mDataAutoRefresh = null;
}
