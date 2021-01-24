package com.dingyi.MyLuaApp.activitys;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.widget.PopupMenu;

import android.animation.LayoutTransition;
import android.content.Intent;
import android.graphics.Rect;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.dingyi.MyLuaApp.R;
import com.dingyi.MyLuaApp.bean.ProjectInfo;
import com.dingyi.MyLuaApp.builder.LuaBuilder;
import com.dingyi.MyLuaApp.builder.LuaBuilderCache;
import com.dingyi.MyLuaApp.builder.ProgressBarBuilderOut;
import com.dingyi.MyLuaApp.builder.task.ITask;
import com.dingyi.MyLuaApp.builder.task.lua.CompileLuaTask;
import com.dingyi.MyLuaApp.builder.task.lua.CreateApkTask;
import com.dingyi.MyLuaApp.builder.task.lua.InitBuildCacheTask;
import com.dingyi.MyLuaApp.builder.task.lua.MergeAXMLTask;
import com.dingyi.MyLuaApp.databinding.ActivityEditorBinding;
import com.dingyi.MyLuaApp.dialogs.FileListDialog;
import com.dingyi.MyLuaApp.utils.EditorUtil;
import com.dingyi.MyLuaApp.utils.LogUtilsKt;
import com.dingyi.MyLuaApp.utils.PluginUtil;
import com.dingyi.MyLuaApp.utils.ProjectUtilKt;
import com.dingyi.MyLuaApp.utils.ReflectionUtilsKt;
import com.dingyi.MyLuaApp.utils.TextUtilsKt;
import com.dingyi.MyLuaApp.utils.ViewUtilsKt;
import com.google.android.material.tabs.TabLayout;

import java.util.Objects;
import java.util.concurrent.atomic.AtomicInteger;

public class EditorActivity extends BaseActivity {

    public ActivityEditorBinding binding;

    EditorUtil util;

    ProjectInfo info;

    PluginUtil pluginUtil;

    public FileListDialog dialog = new FileListDialog();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        binding = ActivityEditorBinding.inflate(LayoutInflater.from(this));

        setContentView(binding.getRoot());
        setSupportActionBar(binding.toolbar);

        info = getIntent().getParcelableExtra("info");

        util = new EditorUtil(this);
        initListener();
        util.initParentView(binding.parent).openProject(info);

        pluginUtil=new PluginUtil(this);
        LayoutTransition transition = new LayoutTransition();
        transition.enableTransitionType(LayoutTransition.CHANGING);
        binding.editorParent.setLayoutTransition(transition);
        binding.symbolView.setUtil(util);
        binding.horizontalView.setHorizontalScrollBarEnabled(false);
        getSupportActionBar().setTitle(info.getName());
        getSupportActionBar().setSubtitle(util.getNowOpenPathName());
        startToolBarAnim();




    }

    private void initTableLayoutLongClick() {


        for (int i=0;i<=binding.tabLayout.getTabCount();i++) {
            int finalI = i;
            if (binding.tabLayout.getTabAt(i)!=null) {
                binding.tabLayout.getTabAt(i).view.setOnLongClickListener((view) -> {
                    longClickTableLayout(binding.tabLayout.getTabAt(finalI));
                    return true;
                });
            }
        }
    }

    private void deleteTab(TabLayout.Tab tab) {
        String text=binding.tabLayout.getTabAt(binding.tabLayout.getSelectedTabPosition()).getText().toString();
        int pos=binding.tabLayout.getSelectedTabPosition(); //这里需要对pos计算下

        if (tab.getPosition()<pos) { //不需要等于判断 等于就不会走select的if了
            pos=pos-1;
        }

        binding.tabLayout.removeTab(tab);

        if (binding.tabLayout.getTabAt(binding.tabLayout.getSelectedTabPosition()).getText().toString().equals(text)) {
            binding.tabLayout.getTabAt(pos).select();//如果删除的不是选择的标签 就重新选择已经选择的标签
        }

        if (binding.tabLayout.getTabCount()==0) {
            util.openFile(ProjectUtilKt.getDefaultPath(info.getPath()));
        }
        TextUtilsKt.showSnackBar(binding.getRoot(), getString(R.string.delete_tag));
    }

    private void longClickTableLayout(TabLayout.Tab tab) {
        PopupMenu menu=new PopupMenu(this,tab.view, Gravity.NO_GRAVITY,R.attr.popupMenuStyle,R.style.BasePopMenuStyle);

        menu.getMenuInflater().inflate(R.menu.editor_tab_menu,menu.getMenu());

        menu.setOnMenuItemClickListener(new PopupMenu.OnMenuItemClickListener() {
            @Override
            public boolean onMenuItemClick(MenuItem item) {
                switch (item.getItemId()) {
                    case R.id.save:
                        util.save(tab.getText().toString());
                        TextUtilsKt.showSnackBar(binding.getRoot(), R.string.save_toast);
                        break;
                    case R.id.copy:
                        TextUtilsKt.copyText(EditorActivity.this,tab.getText().toString());
                        TextUtilsKt.showSnackBar(binding.getRoot(),R.string.copy_succeesful);
                        break;
                    case R.id.close:
                        util.remove(tab.getText().toString());
                        deleteTab(tab);
                        break;
                }
                return false;
            }
        });

        menu.show();

    }

    private void initListener() {
        util.addOpenFileListener(file -> {
            for (int i = 0; i < binding.tabLayout.getTabCount(); i++) {
                if (Objects.requireNonNull(Objects.requireNonNull(binding.tabLayout.getTabAt(i)).getText()).toString().equals(util.getNowOpenPathName())) {
                    Objects.requireNonNull(binding.tabLayout.getTabAt(i)).select();
                    return;
                }
            }
            binding.tabLayout.addTab(binding.tabLayout.newTab().setText(util.getNowOpenPathName()), true);
            initTableLayoutLongClick();
        });




        binding.tabLayout.addOnTabSelectedListener(new TabLayout.OnTabSelectedListener() {
            @Override
            public void onTabSelected(TabLayout.Tab tab) {
                util.select((String) tab.getText());
                getSupportActionBar().setSubtitle(tab.getText());
            }

            @Override
            public void onTabUnselected(TabLayout.Tab tab) {

            }

            @Override
            public void onTabReselected(TabLayout.Tab tab) {

            }
        });

        binding.fab.setOnClickListener((v) -> {
            dialog.init(this, util).show();
        });

        inputMethodListener();

        initTableLayoutLongClick();
    }

    private void startToolBarAnim() {
        try {
            TextView titleView = (TextView) ReflectionUtilsKt.getPrivateField(binding.toolbar, "mTitleTextView");
            TextView subTitleView = (TextView) ReflectionUtilsKt.getPrivateField(binding.toolbar, "mSubtitleTextView");
            subTitleView.setVisibility(View.GONE);
            LayoutTransition transition = new LayoutTransition();
            transition.enableTransitionType(LayoutTransition.CHANGING);
            ((ViewGroup) titleView.getParent()).setLayoutTransition(transition);
            new Handler(Looper.getMainLooper()).postDelayed(() -> subTitleView.setVisibility(View.VISIBLE), 200);
        } catch (Exception e) {
            LogUtilsKt.e(e.toString());
        }
    }

    private void inputMethodListener() {


        Rect rect = new Rect();
        AtomicInteger displayHeight = new AtomicInteger();

        getWindow().getDecorView().post(() -> {
            getWindow().getDecorView().getWindowVisibleDisplayFrame(rect);
            displayHeight.set(rect.bottom);
        });

        getWindow().getDecorView().getViewTreeObserver().addOnGlobalLayoutListener(() -> {
            getWindow().getDecorView().getWindowVisibleDisplayFrame(rect);
            if (rect.bottom == displayHeight.get()) {
                binding.fab.show();
            } else {
                binding.fab.hide();
            }

        });
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.editor_menu, menu);
        ViewUtilsKt.foreachSetMenuIconColor(menu, themeUtil.getImageColorFilter());
        if (!(info.getType()==ProjectUtilKt.GRADLE_PROJECT)) {
            menu.findItem(R.id.asyc).setEnabled(false);
        }
        return true;
    }


    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if (!TextUtilsKt.binSlideOutToRight(keyCode, this, binding.getRoot())) {
            return super.onKeyDown(keyCode, event);
        }
        return true;
    }


    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        switch (item.getItemId()) {
            case R.id.undo:
                util.undo();
                break;
            case R.id.save:
                util.save();
                TextUtilsKt.showSnackBar(binding.getRoot(), R.string.save_toast);
                break;
            case R.id.redo:
                util.redo();
                break;
            case R.id.goto_menu:
                util.gotoLine();
                break;
            case R.id.permission:
                pluginUtil.runPlugin("com.dingyi.plugin.built.permissionHelper",new Object[]{ProjectUtilKt.getProjectInfoPath(info)});
                break;
            case R.id.run:
                util.save();
                ProjectUtilKt.runProject(this, info);
                break;
            case R.id.build:
                util.save();
                ProjectUtilKt.build(info,this);
                break;
            case R.id.error:
                if (!util.canCheckError()) {
                    TextUtilsKt.showSnackBar(binding.getRoot(),R.string.no_check_error);
                    break;
                }
                String errorMsg = util.getError();
                if (errorMsg.length() == 0) {
                    TextUtilsKt.showSnackBar(binding.getRoot(), R.string.no_error);
                } else {
                    TextUtilsKt.showSnackBar(binding.getRoot(), errorMsg);
                }
                break;
            case R.id.find:
                util.search();
                break;
            case R.id.format:
                util.format();
                break;
            default:
                //TextUtilsKt.showSnackBar(binding.getRoot(),R.string.code_ing);
        }
        return true;
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (resultCode==100) { //刷新 init.lua
            util.refresh("init.lua");
            TextUtilsKt.showSnackBar(binding.getRoot(),R.string.permission_successful);
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        util.save();
    }

    @Override
    protected void onResume() {
        super.onResume();

    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        util.save();
    }
}