package com.example.dbpractice

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.os.Environment
import android.provider.MediaStore
import android.util.Log
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.dropbox.core.android.Auth
import com.example.dbpractice.DropBoxClient.Companion.getClient
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import androidx.core.content.edit

class MainActivity : AppCompatActivity() {
    private lateinit var ACCESS_TOKEN: String
    private val appKey = "Put Your own ApiKey from dropbox website"
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_main)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }
        DropBoxListener()

    }

    private fun DropBoxListener() {
        val authBtn = findViewById<Button>(R.id.auth_btn)
        val uploadBtn = findViewById<Button>(R.id.upload_btn)
        val downloadBtn = findViewById<Button>(R.id.download_btn)
        val listBtn = findViewById<Button>(R.id.listen_files_btn)
        btnListeners(authBtn, uploadBtn, downloadBtn, listBtn)
    }

    private fun btnListeners(
        authbtn: Button?,
        uploadBtn: Button?,
        downloadBtn: Button?,
        listBtn: Button?
    ) {
        authbtn?.setOnClickListener {
            Auth.getOAuth2Token()?.let {
                ACCESS_TOKEN = it
            } ?: Auth.startOAuth2Authentication(this, appKey)
        }
        if (Auth.getOAuth2Token() != null) {
            ACCESS_TOKEN = Auth.getOAuth2Token() ?: ""
        }
        uploadBtn?.setOnClickListener {
            if (::ACCESS_TOKEN.isInitialized) {
                val intent =
                    Intent(
                        Intent.ACTION_PICK,
                        MediaStore.Images.Media.EXTERNAL_CONTENT_URI
                    )
                galleryLauncher.launch(intent)

            }
        }
        downloadBtn?.setOnClickListener {
            //download file
            if (::ACCESS_TOKEN.isInitialized) {
                downLoadFile(this, ACCESS_TOKEN)
            }
        }

        listBtn?.setOnClickListener {
            if (::ACCESS_TOKEN.isInitialized) {
                listFiles( ACCESS_TOKEN)
            }
        }
    }

    override fun onResume() {
        super.onResume()
        if (Auth.getOAuth2Token() != null) {
            ACCESS_TOKEN = Auth.getOAuth2Token()!!
        }
    }

    val galleryLauncher = registerForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->

        if (result.resultCode == RESULT_OK) {
            val data: Intent? = result.data
            val selectedImageUri = data?.data
            uploadFile(this, ACCESS_TOKEN, selectedImageUri)
        }
    }

}

private fun MainActivity.listFiles(
    string: String
) {

    Thread {
        val folders = getClient(token = string).files().listFolder("")
        val listFileTex = findViewById<TextView>(R.id.list_files_text)
        val mutableList = mutableListOf<String>()
        for (file in folders.entries) {
            mutableList.add(file.name)
        }
        mutableList.joinToString(",n")
        runOnUiThread {
            listFileTex.text = mutableList.toString()
        }

    }.start()
}

private fun MainActivity.downLoadFile(
    activity: MainActivity,
    token: String
) {
    Thread {
        val downloadDir = Environment.getExternalStoragePublicDirectory(
            Environment.DIRECTORY_DOWNLOADS
        )
        val file = File(downloadDir, "temp_file.txt")
        val outputSteam = FileOutputStream(file)
        val client = getClient(token)
        // Save relevant metadata information
        val sharedPref = activity.getSharedPreferences("MyAppPrefs", Context.MODE_PRIVATE)
        val metadataPath = sharedPref.getString("metaDataPath", null)
        val metadataRevision = sharedPref.getString("metaDataRevision", null)

        client.files().download(
                metadataPath, metadataRevision
        ).download(outputSteam)

        runOnUiThread {
            Toast.makeText(activity, "File Downloaded", Toast.LENGTH_SHORT).show()
        }
    }.start()
}


fun uploadFile(context: Activity, token: String, selectedImageUri: Uri?) {
    Thread {

        val tempFileToUpload = File.createTempFile("temp_file", ".txt", context.cacheDir)
        tempFileToUpload.writeText("Hello World, This is a test file!")
        val file = File(context.filesDir, tempFileToUpload.name)

        try {
            if (!file.exists()) {
                file.createNewFile()
            }
            val inputStream = FileInputStream(file)
            token.let {
                val sharedPref = context.getSharedPreferences("MyAppPrefs", Context.MODE_PRIVATE)

                val metaData = getClient(token)
                    .files().uploadBuilder(
                        "/MyAppUploads/${file.name}"
                    ).uploadAndFinish(
                        inputStream
                    )
                // Save relevant metadata information
                sharedPref.edit {
                    putString("metaDataPath", metaData.pathLower)
                    putString("metaDataName", metaData.name)
                    putLong("metaDataSize", metaData.size)
                    putString("metaDataRevision", metaData.rev)
                }

                context.runOnUiThread {
                    Toast.makeText(context, "File Uploaded ${metaData.name}", Toast.LENGTH_SHORT)
                        .show()
                }
            }

        } catch (e: Exception) {
            Log.e("MY_TAG", "Error Uploading File:" + e.message.toString())
        }

    }.start()

}

