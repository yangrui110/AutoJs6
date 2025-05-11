package org.autojs.autojs.ui.home;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.floatingactionbutton.FloatingActionButton;
import org.autojs.autojs.ui.floating.FloatyWindowManger;
import org.autojs.autojs.ui.main.MainActivity;
import org.autojs.autojs6.R;

public class NewHomeActivity extends AppCompatActivity implements WebRTCService.WebRTCListener {

    private static final String TAG = "NewHomeActivity";
    
    private RecyclerView mRecentProjectsRecyclerView;
    private FloatingActionButton mFab;
    private Button mBtnStartWebRTC;
    private TextView mTvWebRTCStatus;
    private TextView mTvConnectionInfo;
    private boolean mIsWebRTCRunning = false;
    private WebRTCService mWebRTCService;
    private Handler mMainHandler;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_new_home);

        // 设置工具栏
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        
        // 初始化Handler
        mMainHandler = new Handler(Looper.getMainLooper());

        // 初始化视图
        initViews();
        // 设置点击事件
        setupClickListeners();
        // 加载最近项目
        loadRecentProjects();
        // 初始化WebRTC服务
        initWebRTCService();
    }

    private void initViews() {
        mRecentProjectsRecyclerView = findViewById(R.id.rv_recent_projects);
        mFab = findViewById(R.id.fab);
        mBtnStartWebRTC = findViewById(R.id.btn_start_webrtc);
        mTvWebRTCStatus = findViewById(R.id.tv_webrtc_status);
        mTvConnectionInfo = findViewById(R.id.tv_connection_info);
        
        // 设置RecyclerView
        mRecentProjectsRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        // TODO: 设置适配器
    }

    private void setupClickListeners() {
        // 新建脚本按钮
        findViewById(R.id.btn_new_script).setOnClickListener(v -> {
            // TODO: 实现新建脚本功能
            Toast.makeText(this, "新建脚本功能", Toast.LENGTH_SHORT).show();
        });

        // 打开脚本按钮
        findViewById(R.id.btn_open_script).setOnClickListener(v -> {
            // TODO: 实现打开脚本功能
            Toast.makeText(this, "打开脚本功能", Toast.LENGTH_SHORT).show();
        });

        // 启动WebRTC被控端按钮
        mBtnStartWebRTC.setOnClickListener(v -> {
            if (!mIsWebRTCRunning) {
                startWebRTCService();
                mBtnStartWebRTC.setText("停止WebRTC服务");
                mIsWebRTCRunning = true;
                updateStatus("正在连接...");
            } else {
                stopWebRTCService();
                mBtnStartWebRTC.setText("启动WebRTC服务");
                mIsWebRTCRunning = false;
                updateStatus("已断开连接");
                updateConnectionInfo("连接信息: 无");
            }
        });

        // 悬浮按钮
        mFab.setOnClickListener(v -> {
            // TODO: 实现悬浮按钮功能
            Toast.makeText(this, "悬浮按钮功能", Toast.LENGTH_SHORT).show();
        });
    }

    private void loadRecentProjects() {
        // TODO: 实现加载最近项目列表
    }

    private void initWebRTCService() {
        // 初始化WebRTC服务
        mWebRTCService = new WebRTCService(this);
        mWebRTCService.setListener(this);
        
        // 设置WebRTC信令服务器地址
        // 优先从SharedPreferences中获取保存的服务器地址
        String serverUrl = getSharedPreferences("webrtc_prefs", MODE_PRIVATE)
                .getString("server_url", "ws://192.168.0.104:7070/websocket/webrtc");
        
        // 确保URL格式正确
        if (!serverUrl.startsWith("ws://") && !serverUrl.startsWith("wss://")) {
            serverUrl = "ws://" + serverUrl;
        }
        
        mWebRTCService.setServerUrl(serverUrl);
        
        // 设置默认的房间ID和客户端ID
        String roomId = getSharedPreferences("webrtc_prefs", MODE_PRIVATE)
                .getString("room_id", "room-1996");
        String clientId = getSharedPreferences("webrtc_prefs", MODE_PRIVATE)
                .getString("client_id", "client-2000");
        
        mWebRTCService.setRoomId(roomId);
        mWebRTCService.setClientId(clientId);
        
        // 更新UI显示
        updateConnectionInfo("服务器: " + serverUrl);
    }

    private void startWebRTCService() {
        try {
            // 启动WebRTC服务
            Toast.makeText(this, "正在请求屏幕捕获权限", Toast.LENGTH_SHORT).show();
            updateStatus("正在请求屏幕捕获权限...");
            
            // 请求屏幕捕获权限
            mWebRTCService.requestScreenCapturePermission(this);
        } catch (Exception e) {
            Toast.makeText(this, "启动WebRTC服务失败: " + e.getMessage(), Toast.LENGTH_LONG).show();
            updateStatus("连接失败: " + e.getMessage());
        }
    }

    private void stopWebRTCService() {
        try {
            // 停止WebRTC服务
            Toast.makeText(this, "WebRTC服务已停止", Toast.LENGTH_SHORT).show();
            // Intent intent = new Intent(this, WebRTCBackgroundService.class);
            // stopService(intent);
            
            // 或者直接在此Activity中处理WebRTC逻辑
            mWebRTCService.stop();
        } catch (Exception e) {
            Toast.makeText(this, "停止WebRTC服务失败: " + e.getMessage(), Toast.LENGTH_LONG).show();
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_new_home, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        int id = item.getItemId();
        
        if (id == R.id.action_settings) {
            // 打开设置页面
            Toast.makeText(this, "设置功能", Toast.LENGTH_SHORT).show();
            return true;
        } else if (id == R.id.action_about) {
            // 打开关于页面
            Toast.makeText(this, "关于功能", Toast.LENGTH_SHORT).show();
            return true;
        } else if (id == R.id.action_webrtc_settings) {
            // 打开WebRTC设置页面
            openWebRTCSettings();
            return true;
        }
        
        return super.onOptionsItemSelected(item);
    }

    private void openWebRTCSettings() {
        // 创建一个对话框，让用户输入WebRTC信令服务器地址
        android.app.AlertDialog.Builder builder = new android.app.AlertDialog.Builder(this);
        builder.setTitle("WebRTC设置");
        
        // 创建一个LinearLayout作为对话框的内容
        android.widget.LinearLayout layout = new android.widget.LinearLayout(this);
        layout.setOrientation(android.widget.LinearLayout.VERTICAL);
        layout.setPadding(50, 30, 50, 30);
        
        // 添加服务器地址输入框
        final android.widget.TextView serverUrlLabel = new android.widget.TextView(this);
        serverUrlLabel.setText("信令服务器地址");
        layout.addView(serverUrlLabel);
        
        final android.widget.EditText serverUrlInput = new android.widget.EditText(this);
        serverUrlInput.setText(getSharedPreferences("webrtc_prefs", MODE_PRIVATE)
                .getString("server_url", "ws://192.168.0.104:7070/websocket/webrtc"));
        layout.addView(serverUrlInput);
        
        // 添加房间ID输入框
        final android.widget.TextView roomIdLabel = new android.widget.TextView(this);
        roomIdLabel.setText("房间ID");
        roomIdLabel.setPadding(0, 30, 0, 0);
        layout.addView(roomIdLabel);
        
        final android.widget.EditText roomIdInput = new android.widget.EditText(this);
        roomIdInput.setText(getSharedPreferences("webrtc_prefs", MODE_PRIVATE)
                .getString("room_id", "room-" + Math.floor(Math.random() * 10000)));
        layout.addView(roomIdInput);
        
        // 添加客户端ID输入框
        final android.widget.TextView clientIdLabel = new android.widget.TextView(this);
        clientIdLabel.setText("客户端ID");
        clientIdLabel.setPadding(0, 30, 0, 0);
        layout.addView(clientIdLabel);
        
        final android.widget.EditText clientIdInput = new android.widget.EditText(this);
        clientIdInput.setText(getSharedPreferences("webrtc_prefs", MODE_PRIVATE)
                .getString("client_id", "client-" + Math.floor(Math.random() * 10000)));
        layout.addView(clientIdInput);
        
        builder.setView(layout);
        
        builder.setPositiveButton("保存", (dialog, which) -> {
            // 保存设置
            String serverUrl = serverUrlInput.getText().toString().trim();
            String roomId = roomIdInput.getText().toString().trim();
            String clientId = clientIdInput.getText().toString().trim();
            
            if (serverUrl.isEmpty() || roomId.isEmpty() || clientId.isEmpty()) {
                Toast.makeText(this, "所有字段不能为空", Toast.LENGTH_SHORT).show();
                return;
            }
            
            // 确保URL格式正确
            if (!serverUrl.startsWith("ws://") && !serverUrl.startsWith("wss://")) {
                serverUrl = "ws://" + serverUrl;
            }
            
            // 保存到SharedPreferences
            getSharedPreferences("webrtc_prefs", MODE_PRIVATE)
                    .edit()
                    .putString("server_url", serverUrl)
                    .putString("room_id", roomId)
                    .putString("client_id", clientId)
                    .apply();
            
            // 应用新的设置
            mWebRTCService.setServerUrl(serverUrl);
            mWebRTCService.setRoomId(roomId);
            mWebRTCService.setClientId(clientId);
            
            // 更新UI
            updateConnectionInfo("服务器: " + serverUrl);
            
            Toast.makeText(this, "设置已保存", Toast.LENGTH_SHORT).show();
        });
        
        builder.setNegativeButton("取消", (dialog, which) -> dialog.cancel());
        
        builder.show();
    }
    
    // 更新状态文本
    private void updateStatus(String status) {
        mMainHandler.post(() -> {
            mTvWebRTCStatus.setText("状态: " + status);
        });
    }
    
    // 更新连接信息文本
    private void updateConnectionInfo(String info) {
        mMainHandler.post(() -> {
            mTvConnectionInfo.setText(info);
        });
    }
    
    // WebRTCListener接口实现方法
    
    @Override
    public void onStateChanged(int state) {
        switch (state) {
            case WebRTCService.STATE_NEW:
                updateStatus("初始化");
                break;
            case WebRTCService.STATE_CONNECTING:
                updateStatus("正在连接...");
                break;
            case WebRTCService.STATE_CONNECTED:
                updateStatus("已连接");
                break;
            case WebRTCService.STATE_DISCONNECTED:
                updateStatus("已断开连接");
                break;
            case WebRTCService.STATE_FAILED:
                updateStatus("连接失败");
                break;
        }
    }
    
    @Override
    public void onError(String errorMessage) {
        mMainHandler.post(() -> {
            Toast.makeText(this, errorMessage, Toast.LENGTH_LONG).show();
        });
    }
    
    @Override
    public void onSignalingConnected() {
        updateStatus("信令服务器已连接");
    }
    
    @Override
    public void onSignalingDisconnected() {
        updateStatus("信令服务器已断开");
    }
    
    @Override
    public void onConnected() {
        updateStatus("WebRTC已连接");
        updateConnectionInfo("连接信息: 已建立对等连接");
    }
    
    @Override
    public void onPeerConnected(String peerId) {
        mMainHandler.post(() -> {
            Toast.makeText(this, "客户端已连接: " + peerId, Toast.LENGTH_SHORT).show();
            updateConnectionInfo("连接信息: 客户端 " + peerId + " 已连接");
        });
    }
    
    @Override
    public void onPeerDisconnected(String peerId) {
        mMainHandler.post(() -> {
            Toast.makeText(this, "客户端已断开: " + peerId, Toast.LENGTH_SHORT).show();
            updateConnectionInfo("连接信息: 客户端 " + peerId + " 已断开");
        });
    }
    
    @Override
    public void onRemoteCommand(String command) {
        // 处理远程命令
        mMainHandler.post(() -> {
            Toast.makeText(this, "收到远程命令: " + command, Toast.LENGTH_SHORT).show();
            
            // 在这里处理接收到的远程命令
            // 例如执行脚本、模拟点击等
            // executeCommand(command);
        });
    }
    
    @Override
    public void onScreenCapturePermissionGranted() {
        // 屏幕捕获权限已授予
        mMainHandler.post(() -> {
            Toast.makeText(this, "屏幕共享权限已获取", Toast.LENGTH_SHORT).show();
            updateStatus("屏幕共享权限已获取");
        });
    }
    
    @Override
    public void onScreenCaptureStopped() {
        // 屏幕捕获已停止
        mMainHandler.post(() -> {
            Toast.makeText(this, "屏幕共享已停止", Toast.LENGTH_SHORT).show();
            updateStatus("屏幕共享已停止");
            
            // 如果需要，这里可以更新UI状态
            if (mIsWebRTCRunning) {
                mBtnStartWebRTC.setText("启动WebRTC服务");
                mIsWebRTCRunning = false;
            }
        });
    }
    
    // 执行远程命令
    private void executeCommand(String command) {
        // 在实际实现中，解析并执行命令
        // 可以使用AutoJs的API执行脚本或模拟用户操作
    }
    
    @Override
    protected void onDestroy() {
        super.onDestroy();
        // 确保在Activity销毁时停止WebRTC服务
        if (mWebRTCService != null) {
            if (mIsWebRTCRunning) {
                stopWebRTCService();
            }
            // 释放WebRTC服务的所有资源
            mWebRTCService.release();
            mWebRTCService = null;
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        
        // 处理屏幕捕获权限请求结果
        if (requestCode == WebRTCService.CAPTURE_PERMISSION_REQUEST_CODE) {
            if (resultCode == RESULT_OK && data != null) {
                // 权限获取成功，处理结果
                mWebRTCService.handleScreenCapturePermissionResult(resultCode, data);
                
                // 获取权限后启动服务
                Toast.makeText(this, "WebRTC服务正在启动", Toast.LENGTH_SHORT).show();
                mWebRTCService.start();
            } else {
                // 权限被拒绝，更新UI
                Toast.makeText(this, "屏幕捕获权限被拒绝", Toast.LENGTH_LONG).show();
                updateStatus("屏幕捕获权限被拒绝");
                mBtnStartWebRTC.setText("启动WebRTC服务");
                mIsWebRTCRunning = false;
            }
        }
    }
} 