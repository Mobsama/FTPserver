package com.mob.ftpserver;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import android.Manifest;
import android.content.Context;
import android.content.DialogInterface;
import android.os.Bundle;
import android.os.Environment;
import android.os.IBinder;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import org.apache.ftpserver.FtpServer;
import org.apache.ftpserver.FtpServerFactory;
import org.apache.ftpserver.ftplet.Authority;
import org.apache.ftpserver.ftplet.FtpException;
import org.apache.ftpserver.listener.ListenerFactory;
import org.apache.ftpserver.usermanager.impl.BaseUser;
import org.apache.ftpserver.usermanager.impl.WritePermission;

import java.io.File;
import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;

public class MainActivity extends AppCompatActivity {

    LinearLayout mLLPath;
    ArrayAdapter<String> adapter;
    TextView mTVFTPState,mTVSharePath;
    Switch mSwitchFTP;
    EditText mETPort,mETUser,mETPass;
    CheckBox mCBWrite;

    FtpServer mServer;

    final String rootPath = Environment.getExternalStorageDirectory().getPath();
    String currPath;
    File[] currList;
    List<String> list;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        ActivityCompat.requestPermissions(this,new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE},1);


        setContentView(R.layout.activity_main);
        mLLPath = findViewById(R.id.ll_path);
        mLLPath.setOnClickListener(pathChoose);
        mTVSharePath = findViewById(R.id.tv_share_path);
        mTVSharePath.setText(rootPath);
        mTVFTPState = findViewById(R.id.tv_ftp_state);
        mSwitchFTP = findViewById(R.id.switch_ftp);
        mETPort = findViewById(R.id.et_port);
        mETUser = findViewById(R.id.et_user);
        mETPass = findViewById(R.id.et_pass);
        mCBWrite = findViewById(R.id.cb_write);
        mETUser.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) { }

            @Override
            public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) { }

            @Override
            public void afterTextChanged(Editable editable) {
                if(!mETUser.getText().toString().isEmpty()){
                    mETPass.setEnabled(true);
                }else{
                    mETPass.setEnabled(false);
                }
            }
        });

        mSwitchFTP.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton compoundButton, boolean b) {
                if(b){
                    int port = Integer.parseInt(mETPort.getText().toString());
                    String addr = "null";
                    try {
                        addr = getLocalIPAddress();
                    } catch (SocketException e) {
                        e.printStackTrace();
                    }
                    if(addr.equals("null")){
                        Toast.makeText(MainActivity.this,"请连接局域网",Toast.LENGTH_LONG).show();
                        mSwitchFTP.setChecked(false);
                    }else{
                        if(port < 1024 || port > 65535){
                            Toast.makeText(MainActivity.this,"请填写正确的端口号",Toast.LENGTH_LONG).show();
                            mSwitchFTP.setChecked(false);
                        }else{
                            startFTPServer();
                            mETUser.setEnabled(false);
                            mETPass.setEnabled(false);
                            mETPort.setEnabled(false);
                            mLLPath.setEnabled(false);
                            mCBWrite.setEnabled(false);
                            mTVFTPState.setText("FTP=ftp://"+addr+":"+mETPort.getText().toString());
                        }
                    }
                }else{
                    stopFTPServer();
                    mTVFTPState.setText("FTP服务没有启动");
                    mETUser.setEnabled(true);
                    if(!mETPass.getText().toString().isEmpty()){
                        mETPass.setEnabled(true);
                    }
                    mETPort.setEnabled(true);
                    mLLPath.setEnabled(true);
                    mCBWrite.setEnabled(true);
                }
            }
        });
    }

    View.OnClickListener pathChoose = new View.OnClickListener() {
        @Override
        public void onClick(View view) {
            final AlertDialog.Builder builder = new AlertDialog.Builder(MainActivity.this);
            View mView = LayoutInflater.from(MainActivity.this).inflate(R.layout.file_choose,null);
            final ListView mLVFile = mView.findViewById(R.id.lv_file);
            currPath = rootPath;
            currList = new File(rootPath).listFiles();
            list = new ArrayList<>();
            assert currList != null;
            for(File f : currList){
                if(f.getName().charAt(0)=='.') continue;
                list.add(f.getName());
            }
            adapter = new ArrayAdapter<>(
                    MainActivity.this, android.R.layout.simple_list_item_1, list);
            mLVFile.setAdapter(adapter);
            mLVFile.setOnItemClickListener(new AdapterView.OnItemClickListener() {
                @Override
                public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {
                    if(i==0 && mLVFile.getItemAtPosition(i).toString().equals("...")){
                        currPath = currPath.substring(0,currPath.lastIndexOf(File.separator));
                    }else{
                        currPath = currPath+File.separator+mLVFile.getItemAtPosition(i).toString();
                    }
                    currList = new File(currPath).listFiles();
                    assert currList != null;
                    list.clear();
                    if(!currPath.equals(rootPath)){
                        list.add("...");
                    }
                    for(File f : currList){
                        if(f.getName().charAt(0)=='.') continue;
                        list.add(f.getName());
                    }
                    adapter.notifyDataSetChanged();
                }
            });
            builder.setTitle("请选择路径").setView(mView).setPositiveButton("确定", new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialogInterface, int i) {
                    mTVSharePath.setText(currPath);
                }
            }).setNegativeButton("取消", new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialogInterface, int i) {

                }
            }).show();
        }
    };

    @Override
    public boolean dispatchTouchEvent(MotionEvent ev) {
        if (ev.getAction() == MotionEvent.ACTION_DOWN) {
            View v = getCurrentFocus();
            if (isShouldHideKeyboard(v, ev)) {
                hideKeyboard(v.getWindowToken());
            }
        }
        return super.dispatchTouchEvent(ev);
    }

    /***
     *检测点击位置，判断是否需要隐藏键盘
     * @param v
     * @param event
     * @return
     */
    private boolean isShouldHideKeyboard(View v, MotionEvent event) {
        if (v instanceof EditText) {
            int[] l = {0, 0};
            v.getLocationInWindow(l);
            int left = l[0],
                top = l[1],
                bottom = top + v.getHeight(),
                right = left + v.getWidth();
            return !(event.getX() > left) || !(event.getX() < right)
                    || !(event.getY() > top) || !(event.getY() < bottom);
        }
        return false;
    }

    /***
     * 隐藏键盘
     * @param token
     */
    private void hideKeyboard(IBinder token) {
        if (token != null) {
            InputMethodManager im = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
            im.hideSoftInputFromWindow(token, InputMethodManager.HIDE_NOT_ALWAYS);
        }
    }
    /**
     * 获取内网IP地址
     * @return
     * @throws SocketException
     */
    public static String getLocalIPAddress() throws SocketException {
        for(Enumeration<NetworkInterface> en = NetworkInterface.getNetworkInterfaces(); en.hasMoreElements();){
            NetworkInterface intf = en.nextElement();
            for(Enumeration<InetAddress> enumIpAddr = intf.getInetAddresses(); enumIpAddr.hasMoreElements();){
                InetAddress inetAddress = enumIpAddr.nextElement();
                if(!inetAddress.isLoopbackAddress() && (inetAddress instanceof Inet4Address)){
                    return inetAddress.getHostAddress();
                }
            }
        }
        return "null";
    }

    private void startFTPServer(){
      new Thread(new Runnable() {
          @Override
          public void run() {
              ListenerFactory factory = new ListenerFactory();
              factory.setPort(Integer.parseInt(mETPort.getText().toString()));
              FtpServerFactory serverFactory = new FtpServerFactory();
              serverFactory.addListener("default", factory.createListener());

              BaseUser user = new BaseUser();
              if(mETUser.getText().toString().isEmpty()){
                  user.setName("anonymous");
              }else{
                  user.setName(mETUser.getText().toString());
                  user.setPassword(mETPass.getText().toString());
              }
              user.setHomeDirectory(mTVSharePath.getText().toString());
              if(mCBWrite.isChecked()){
                  List<Authority> authorities = new ArrayList<Authority>();
                  authorities.add(new WritePermission());
                  user.setAuthorities(authorities);
              }
              try {
                  serverFactory.getUserManager().save(user);
                  if(mServer != null) {
                      mServer.stop();
                  }
                  mServer = serverFactory.createServer();
                  mServer.start();
              } catch (FtpException e) {
                  e.printStackTrace();
              }
          }
      }).start();
    }

    private void stopFTPServer(){
        new Thread(new Runnable() {
            @Override
            public void run() {
                if(mServer!=null){
                    mServer.stop();
                }
                mServer = null;
            }
        }).start();
    }

}