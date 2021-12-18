package com.dingyi.myluaapp.ui.editior.activity

import android.os.Bundle
import android.view.*
import android.widget.TextView
import androidx.appcompat.app.ActionBarDrawerToggle
import androidx.core.view.GravityCompat
import androidx.drawerlayout.widget.DrawerLayout
import androidx.lifecycle.lifecycleScope
import com.dingyi.myluaapp.R
import com.dingyi.myluaapp.base.BaseActivity
import com.dingyi.myluaapp.common.kts.addLayoutTransition
import com.dingyi.myluaapp.common.kts.addDrawerListener
import com.dingyi.myluaapp.common.kts.convertObject
import com.dingyi.myluaapp.common.kts.getJavaClass
import com.dingyi.myluaapp.common.kts.getPrivateField
import com.dingyi.myluaapp.databinding.ActivityEditorBinding
import com.dingyi.myluaapp.ui.editior.MainViewModel
import com.dingyi.myluaapp.ui.editior.adapter.EditorDrawerPagerAdapter
import com.dingyi.myluaapp.ui.editior.adapter.EditorPagerAdapter
import com.google.android.material.tabs.TabLayout
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch


/**
 * @author: dingyi
 * @date: 2021/11/3 15:38
 * @description:
 **/
class EditorActivity : BaseActivity<ActivityEditorBinding, MainViewModel>() {


    override fun getViewModelClass(): Class<MainViewModel> {
        return getJavaClass()
    }

    override fun getViewBindingInstance(): ActivityEditorBinding {
        return ActivityEditorBinding.inflate(layoutInflater)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        //先不启用过渡动画
        postponeEnterTransition()

        super.onCreate(savedInstanceState)

        viewModel.initProjectController(intent.getStringExtra("project_path") ?: "")

        setSupportActionBar(viewBinding.toolbar)

        initView()


        //反射获取控件和启用过渡动画
        viewBinding.toolbar.getPrivateField<TextView>("mTitleTextView").transitionName =
            "project_name_transition"

        startPostponedEnterTransition()


        viewModel.refreshOpenedFile()


    }


    private fun initView() {

        viewBinding.editorPage.adapter = EditorPagerAdapter(this, viewModel)
        viewBinding.drawerPage.adapter = EditorDrawerPagerAdapter(this).apply {
            notifyDataSetChanged()
        }

        viewBinding.editorTab.apply {
            bindEditorPager(viewBinding.editorPage)
            projectPath = viewModel.controller.projectPath
            onSelectFile {
                viewModel.controller.selectOpenedFile(it)
            }
        }


        listOf(viewBinding.container).forEach { it.addLayoutTransition() }

        supportActionBar?.let { actionBar ->
            viewBinding.editorTab.bindActionBar(actionBar)
            actionBar.setDisplayHomeAsUpEnabled(true)
            val toggle = ActionBarDrawerToggle(this, viewBinding.drawer, 0, 0)

            viewBinding.drawer.addDrawerListener(toggle)
            toggle.syncState()
        }
        viewBinding.drawer.apply {
            setScrimColor(0)
            drawerElevation = 0f
            addDrawerListener(
                onDrawerClosed = {
                    setDrawerLockMode(DrawerLayout.LOCK_MODE_UNLOCKED)
                },
                onDrawerOpened = {
                    setDrawerLockMode(DrawerLayout.LOCK_MODE_LOCKED_CLOSED)
                    setDrawerLockMode(DrawerLayout.LOCK_MODE_LOCKED_OPEN)
                },
                onDrawerSlide = { _, slideOffset ->
                    viewBinding.main.x = viewBinding.drawerPage.width * slideOffset
                }
            )
        }

    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.editor_toolbar, menu)
        return super.onCreateOptionsMenu(menu)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            android.R.id.home -> {
                viewBinding.drawer.apply {
                    if (isDrawerOpen(GravityCompat.START))
                        closeDrawer(GravityCompat.START)
                    else
                        openDrawer(GravityCompat.START)
                }
            }
        }
        return super.onOptionsItemSelected(item)
    }

    override fun observeViewModel() {

        viewModel.appTitle.observe(this) {
            supportActionBar?.title = it

        }

        viewModel.openFiles.observe(this) {
            val list = it.first
            val visibility = if (list.isNotEmpty()) View.VISIBLE else View.GONE
            viewBinding.editorPage.visibility = visibility
            viewBinding.editorTab.visibility = visibility
            viewBinding.editorToastOpenFile.visibility =
                if (list.isEmpty()) View.VISIBLE else View.GONE
            if (list.isNotEmpty()) viewBinding.editorTab.postOpenedFiles(it.first, it.second)
            viewBinding.editorPage.adapter?.notifyDataSetChanged()
        }

    }

    override fun onKeyUp(keyCode: Int, event: KeyEvent?): Boolean {
        return when (keyCode) {
            KeyEvent.KEYCODE_BACK, KeyEvent.KEYCODE_ESCAPE -> {
                when {
                    (viewBinding.drawer.isDrawerOpen(GravityCompat.START)) -> {
                        viewBinding.drawer.closeDrawers()
                        true
                    }
                    else -> super.onKeyUp(keyCode, event)
                }
            }
            else -> super.onKeyUp(keyCode, event)
        }
    }
}