package org.autojs.autojs.ui.home;

import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.floatingactionbutton.FloatingActionButton;
import org.autojs.autojs.ui.floating.FloatyWindowManger;
import org.autojs.autojs.ui.main.MainActivity;
import org.autojs.autojs6.R;

public class NewHomeActivity extends AppCompatActivity {

    private RecyclerView mRecentProjectsRecyclerView;
    private FloatingActionButton mFab;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_new_home);

        // 设置工具栏
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        // 初始化视图
        initViews();
        // 设置点击事件
        setupClickListeners();
        // 加载最近项目
        loadRecentProjects();
    }

    private void initViews() {
        mRecentProjectsRecyclerView = findViewById(R.id.rv_recent_projects);
        mFab = findViewById(R.id.fab);
        
        // 设置RecyclerView
        mRecentProjectsRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        // TODO: 设置适配器
    }

    private void setupClickListeners() {
        // 新建脚本按钮
        findViewById(R.id.btn_new_script).setOnClickListener(v -> {
            // TODO: 实现新建脚本功能
        });

        // 打开脚本按钮
        findViewById(R.id.btn_open_script).setOnClickListener(v -> {
            // TODO: 实现打开脚本功能
        });

        // 悬浮按钮
        mFab.setOnClickListener(v -> {
            // TODO: 实现悬浮按钮功能
        });
    }

    private void loadRecentProjects() {
        // TODO: 实现加载最近项目列表
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
            // TODO: 打开设置页面
            return true;
        } else if (id == R.id.action_about) {
            // TODO: 打开关于页面
            return true;
        }
        
        return super.onOptionsItemSelected(item);
    }
} 