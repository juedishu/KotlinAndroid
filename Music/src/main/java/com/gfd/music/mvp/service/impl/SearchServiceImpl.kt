package com.gfd.music.mvp.service.impl

import android.content.Context
import com.gfd.music.api.Api
import com.gfd.music.db.SearHistoryTable
import com.gfd.music.db.ext.database
import com.gfd.music.entity.HotSearch
import com.gfd.music.entity.SearchData
import com.gfd.music.entity.SongSearch
import com.gfd.music.mvp.service.SearchService
import com.google.gson.Gson
import com.lzy.okgo.OkGo
import com.lzy.okgo.callback.StringCallback
import com.lzy.okgo.model.Response
import com.orhanobut.logger.Logger
import org.jetbrains.anko.db.StringParser
import org.jetbrains.anko.db.delete
import org.jetbrains.anko.db.insert
import org.jetbrains.anko.db.select
import java.net.URLEncoder
import java.util.*
import javax.inject.Inject
import kotlin.collections.ArrayList

/**
 * @Author : 郭富东
 * @Date ：2018/8/23 - 16:44
 * @Email：878749089@qq.com
 * @description：
 */
class SearchServiceImpl @Inject constructor() : SearchService {

    override fun getHotSearch(callback: SearchService.IGetSearchCallBack) {
        OkGo.get<String>(Api.getHotWord())
                .tag(this)
                .execute(object : StringCallback() {
                    override fun onSuccess(response: Response<String>) {
                        val json = response.body().toString()
                        Logger.e("热门标签：$json")
                        val hotSearchDto = Gson().fromJson(json, HotSearch::class.java)
                        val data = ArrayList<String>()
                        if (hotSearchDto.result != null) {
                            hotSearchDto.result.forEach {
                                data.add(it.word)
                            }
                            data.removeAt(data.size - 1)
                            data.removeAt(data.size - 1)
                            data.removeAt(data.size - 1)
                        }
                        callback.onHotSearch(data)
                    }
                })

    }

    override fun getSearchHistory(context: Context, callback: SearchService.IGetSearchCallBack) {
        context.database.use {
            val historyData = select(SearHistoryTable.TABLE_NAME, SearHistoryTable.NAME).parseList(StringParser)
            Collections.reverse(historyData)
            callback.onSearchHistory(historyData)
        }
    }

    override fun search(context: Context, keyword: String, callback: SearchService.IGetSearchCallBack) {
        //将搜索关键字保存到数据库
        context.database.use {
            insert(SearHistoryTable.TABLE_NAME, SearHistoryTable.NAME to keyword)
        }
        /* val searchUrl = Api.getSearch(keyword)
         Logger.e("搜索URL:$searchUrl")
         OkGo.get<String>(searchUrl)
                 .tag(this)
                 .execute(object:StringCallback(){
                     override fun onSuccess(response: Response<String>) {
                         val json = response.body().toString()
                         Logger.e("搜索音乐返回的数据：$json")
                     }
                 })*/
        val encode = URLEncoder.encode(keyword, "UTF-8")
        Logger.e("关键字：$encode")
        OkGo.post<String>(Api.getSongSearch(encode))
                .tag(this)
                .execute(object : StringCallback() {
                    override fun onSuccess(response: Response<String>) {
                        val json = response.body().toString()
                        Logger.e("搜索音乐结果数据：$json")
                        val searchData = Gson().fromJson(json, SongSearch::class.java).data
                        val musicList = ArrayList<SearchData>()
                        if (searchData != null && searchData.isNotEmpty()) {
                            searchData.forEach {
                                musicList.add(SearchData(it.name, it.pic,it.id, it.singer,it.url))
                            }
                        }
                        callback.onSearchResult(musicList)
                    }

                })
    }

    override fun deleteHistory(context: Context, callback: SearchService.IGetSearchCallBack) {
        context.database.use {
            delete(SearHistoryTable.TABLE_NAME)
        }

    }


}