package com.zy.reader.activity

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.databinding.DataBindingUtil
import com.zy.reader.R
import com.zy.reader.databinding.ActivityMainBinding
import com.zy.reader.utils.FileUtils

class MainActivity : AppCompatActivity() {
    private val binding: ActivityMainBinding by lazy {
        DataBindingUtil.setContentView(this, R.layout.activity_main)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding.btnImport.setOnClickListener {

            Intent(Intent.ACTION_GET_CONTENT).apply {
                setType("*/*")
                addCategory(Intent.CATEGORY_OPENABLE)
                startActivityForResult(Intent.createChooser(this, "Select a file"), 101)
            }

        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if (requestCode == 101 && resultCode == RESULT_OK) {
            data?.data.let {
                val path = FileUtils.uriToFileApiQ(this, it).path
                Intent(this,ReadActivity::class.java).apply {
                    putExtra("bookPath",path)
                    startActivity(this)
                }
            }
        }

    }
}
