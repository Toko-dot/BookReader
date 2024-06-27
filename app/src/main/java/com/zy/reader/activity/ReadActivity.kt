package com.zy.reader.activity

import android.os.Bundle
import android.text.TextUtils
import android.view.View
import android.widget.SeekBar
import android.widget.SeekBar.OnSeekBarChangeListener
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.databinding.DataBindingUtil
import androidx.recyclerview.widget.LinearLayoutManager
import com.zy.reader.R
import com.zy.reader.adapter.ChapterListAdapter
import com.zy.reader.utils.PageFactory.Listener
import com.zy.reader.databinding.ActivityReadBinding
import com.zy.reader.utils.BookUtils
import com.zy.reader.utils.PageConfig
import com.zy.reader.utils.PageFactory
import kotlin.concurrent.thread


class ReadActivity : AppCompatActivity() {

    private val binding: ActivityReadBinding by lazy {
        DataBindingUtil.setContentView(this, R.layout.activity_read)
    }

    private lateinit var bookPath: String

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val path = intent.getStringExtra("bookPath")
        if (TextUtils.isEmpty(path)) {
            finish()
            return
        }
        bookPath = path!!


        PageFactory.init(this@ReadActivity, object : Listener {

            override fun onCacheBookSuccess() {
                PageFactory.startRead(0)
                binding.llOpenBookStatus.visibility = View.GONE
            }

            override fun onCacheBookFail() {

            }

            override fun onMeasureFinish() {
                PageFactory.openBook(bookPath, binding.pageWidget)
            }

            override fun onError(what: Int) {
                when (what) {
                    0 -> {
                        Toast.makeText(this@ReadActivity, "读取异常!", Toast.LENGTH_SHORT).show()
                    }

                    1 -> {
                        Toast.makeText(this@ReadActivity, "显示异常!", Toast.LENGTH_SHORT).show()
                    }

                    1 -> {
                        Toast.makeText(this@ReadActivity, "加载章节异常!", Toast.LENGTH_SHORT)
                            .show()
                    }
                }
            }

            override fun onShowOrHideOptionLayout(type: Int) {
                if (type == 0) {
                    if (binding.llOptionBottom.visibility == View.VISIBLE) {
                        binding.llOptionBottom.visibility = View.GONE
                    } else {
                        binding.llOptionBottom.visibility = View.VISIBLE
                    }
                } else {
                    binding.llOptionBottom.visibility = View.GONE
                }

            }
        })

        binding.tvPreChapter.setOnClickListener {
            PageFactory.preChapter()
        }


        binding.tvNextChapter.setOnClickListener {
            PageFactory.nextChapter()
        }

        binding.sbSeek.setOnSeekBarChangeListener(object : OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {

            }

            override fun onStartTrackingTouch(seekBar: SeekBar?) {

            }

            override fun onStopTrackingTouch(seekBar: SeekBar?) {
                seekBar?.progress?.let {
                    PageFactory.seekToWithProgress(it)
                }

            }

        })


        binding.tvChapterList.setOnClickListener {
            binding.llChapterList.visibility = View.VISIBLE
            if (binding.rvChapters.childCount == 0)
                thread {
                    val chapterList = BookUtils.getChapterList()
                    runOnUiThread {
                        binding.rvChapters.layoutManager = LinearLayoutManager(this);
                        binding.rvChapters.adapter =
                            ChapterListAdapter(chapterList) {
                                PageFactory.startRead(it.startPos);
                                binding.llChapterList.visibility = View.INVISIBLE
                            }
                    }
                }
        }

        binding.chapterListBack.setOnClickListener {
            binding.llChapterList.visibility = View.INVISIBLE
        }

        binding.rbPageAniNone.setOnCheckedChangeListener { buttonView, isChecked ->
            if (isChecked) {
                PageFactory.changePageAnimationType(
                    PageConfig.PAGE_ANIMATION_TYPE_NONE
                )
            }
        }

        binding.rbPageAniCover.setOnCheckedChangeListener { buttonView, isChecked ->
            if (isChecked) {
                PageFactory.changePageAnimationType(
                    PageConfig.PAGE_ANIMATION_TYPE_COVER
                )
            }
        }

        binding.rbModule1.setOnCheckedChangeListener { buttonView, isChecked ->
            if (isChecked) {
                PageFactory.changePageModule(
                    PageConfig.PAGE_MODULE_TYPE_1
                )
            }
        }

        binding.rbModule2.setOnCheckedChangeListener { buttonView, isChecked ->
            if (isChecked) {
                PageFactory.changePageModule(
                    PageConfig.PAGE_MODULE_TYPE_2
                )
            }
        }

        binding.rbModule3.setOnCheckedChangeListener { buttonView, isChecked ->
            if (isChecked) {
                PageFactory.changePageModule(
                    PageConfig.PAGE_MODULE_TYPE_3
                )
            }
        }



        when(PageConfig.pageModule){
            PageConfig.PAGE_MODULE_TYPE_1->{
                binding.rbModule1.isChecked=true;
            }
            PageConfig.PAGE_MODULE_TYPE_2->{
                binding.rbModule2.isChecked=true;
            }
            PageConfig.PAGE_MODULE_TYPE_3->{
                binding.rbModule3.isChecked=true;
            }
        }


        if (PageConfig.pageAnimationType == PageConfig.PAGE_ANIMATION_TYPE_COVER) {
            binding.rbPageAniCover.isChecked = true
        } else {
            binding.rbPageAniNone.isChecked = true
        }


        binding.rbTextSizeMin.setOnCheckedChangeListener { buttonView, isChecked ->
            if (isChecked) {
                PageFactory.changeTextSize(
                    PageConfig.TEXT_SIZE_MIN
                )
            }
        }

        binding.rbTextSizeMid.setOnCheckedChangeListener { buttonView, isChecked ->
            if (isChecked) {
                PageFactory.changeTextSize(
                    PageConfig.TEXT_SIZE_MID
                )
            }
        }

        binding.rbTextSizeMax.setOnCheckedChangeListener { buttonView, isChecked ->
            if (isChecked) {
                PageFactory.changeTextSize(
                    PageConfig.TEXT_SIZE_MAX
                )
            }
        }

        binding.rbTextSetting.setOnCheckedChangeListener { buttonView, isChecked ->

            if (isChecked){
                binding.lTextSetting.visibility=View.VISIBLE
            }else{
                binding.lTextSetting.visibility=View.GONE
            }

        }

        binding.rbAniSetting.setOnCheckedChangeListener { buttonView, isChecked ->

            if (isChecked){
                binding.lAniSetting.visibility=View.VISIBLE
            }else{
                binding.lAniSetting.visibility=View.GONE
            }
        }

        binding.rbModule.setOnCheckedChangeListener { buttonView, isChecked ->
            if (isChecked){
                binding.lModuleSetting.visibility=View.VISIBLE
            }else{
                binding.lModuleSetting.visibility=View.GONE
            }
        }


        when (PageConfig.textSize) {
            PageConfig.TEXT_SIZE_MIN -> {
                binding.rbTextSizeMin.isChecked = true
            }

            PageConfig.TEXT_SIZE_MID -> {
                binding.rbTextSizeMid.isChecked = true
            }

            PageConfig.TEXT_SIZE_MAX -> {
                binding.rbTextSizeMax.isChecked = true
            }
        }


        binding.llOpenBookStatus.visibility = View.VISIBLE

    }


    override fun onBackPressed() {
        if (binding.llChapterList.visibility == View.VISIBLE) {
            binding.llChapterList.visibility = View.INVISIBLE
        } else {
            super.onBackPressed()
        }


    }

}

