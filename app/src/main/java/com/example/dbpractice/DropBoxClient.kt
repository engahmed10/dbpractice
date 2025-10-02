package com.example.dbpractice

import com.dropbox.core.DbxRequestConfig
import com.dropbox.core.v2.DbxClientV2

class DropBoxClient {


    companion object {
        fun getClient(token: String): DbxClientV2 {
            val config = DbxRequestConfig
                .newBuilder("dropbox/dboxInstance")
                .build()
            val client = DbxClientV2(config,
                token)
            return client
        }
    }


}