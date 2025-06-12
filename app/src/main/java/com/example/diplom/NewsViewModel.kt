// src/main/java/com/example/diplom/viewmodels/NewsViewModel.kt
package com.example.diplom.viewmodels

import android.app.Application
import android.net.Uri
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.android.volley.Request
import com.android.volley.toolbox.JsonObjectRequest
import com.example.diplom.models.PostItem
import com.example.diplom.network.VolleySingleton
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.launch
import java.util.*
import kotlin.collections.ArrayList

class NewsViewModel(application: Application) : AndroidViewModel(application) {

    private val _posts = MutableLiveData<List<PostItem>>(emptyList())
    val posts: LiveData<List<PostItem>> = _posts

    private val _isLoading = MutableLiveData(false)
    val isLoading: LiveData<Boolean> = _isLoading

    private val _errorMessage = MutableLiveData<String?>(null)
    val errorMessage: LiveData<String?> = _errorMessage

    // Это публичный метод для очистки сообщения об ошибке
    fun clearErrorMessage() {
        _errorMessage.value = null // Эта строка корректна ВНУТРИ ViewModel
    }
    // ----------------------------------------------------

    private val vkCommunityId = "-85060840"
    private val vkAccessToken = "vk1.a.Ub5Zz8fAfZb_VG-QLzF7Vjug7S-ZA9MqXshTKS5zQMWC_-CDfrDQxc0FMW0IuHdtVCiXmm2mMSEltfktmzqHVXYkw6D4Q0xqB61r757rPo9dwl-MNEQf48vAWvhSuwmCXOQgR8ucjzLczON_7cVp7WTfqf-NFoZzAbcSjeW4NeUcW9rRBn2y92QKrro_adWQwGxyYk0e8LbyIoTp5DGP-w" // Ваш токен
    private val apiVersion = "5.199"

    private val postsPerPage = 100
    private var currentOffset = 0
    private val loadedPostsInMemory = ArrayList<PostItem>()
    private var academicYearStartTimestamp: Long = 0

    private var allPostsFetchedUntilDate = false

    init {
        academicYearStartTimestamp = getAcademicYearStartTimestamp()
    }

    fun refreshPosts() {
        if (_isLoading.value == true) return
        Log.d("NewsViewModel", "Refreshing posts...")

        currentOffset = 0
        loadedPostsInMemory.clear()
        allPostsFetchedUntilDate = false
        _errorMessage.value = null // Очистка ошибки при старте новой загрузки

        fetchWallPosts()
    }

    fun loadMorePosts() {
        if (_isLoading.value == true || allPostsFetchedUntilDate) return
        Log.d("NewsViewModel", "Loading more posts (offset: $currentOffset)...")
        fetchWallPosts()
    }

    private fun fetchWallPosts() {
        _isLoading.value = true

        val url = Uri.parse("https://api.vk.com/method/wall.get").buildUpon()
            .appendQueryParameter("owner_id", vkCommunityId)
            .appendQueryParameter("access_token", vkAccessToken)
            .appendQueryParameter("v", apiVersion)
            .appendQueryParameter("count", postsPerPage.toString())
            .appendQueryParameter("offset", currentOffset.toString())
            .appendQueryParameter("extended", "1")
            .build().toString()

        val jsonObjectRequest = JsonObjectRequest(
            Request.Method.GET, url, null,
            { response ->
                Log.d("NewsViewModel", "VK API Response: $response")
                viewModelScope.launch {
                    try {
                        if (response.has("error")) {
                            val errorObject = response.getJSONObject("error")
                            val errorCode = errorObject.optInt("error_code")
                            val errorMessage = errorObject.optString("error_msg", "Неизвестная ошибка VK API")
                            Log.e("NewsViewModel", "VK API Error: Code $errorCode, Message: $errorMessage")
                            _errorMessage.postValue("Ошибка VK API: $errorMessage")
                            allPostsFetchedUntilDate = true
                        } else {
                            val wallResponse = response.getJSONObject("response")
                            val itemsJsonArray = wallResponse.getJSONArray("items").toString()

                            val listType = object : TypeToken<List<PostItem>>() {}.type
                            val fetchedPosts: List<PostItem> = Gson().fromJson(itemsJsonArray, listType)

                            if (fetchedPosts.isEmpty()) {
                                Log.d("NewsViewModel", "No more posts from VK API.")
                                allPostsFetchedUntilDate = true
                            }

                            var foundOldEnoughPost = false
                            val postsToAdd = ArrayList<PostItem>()
                            for (post in fetchedPosts) {
                                if (post.date < academicYearStartTimestamp) {
                                    foundOldEnoughPost = true
                                    break
                                }
                                postsToAdd.add(post)
                            }

                            loadedPostsInMemory.addAll(postsToAdd)
                            _posts.postValue(loadedPostsInMemory.sortedByDescending { it.date })
                            Log.d("NewsViewModel", "fetchWallPosts: _posts updated with ${loadedPostsInMemory.size} items. isLoading set to false.")

                            if (foundOldEnoughPost) {
                                Log.d("NewsViewModel", "Reached academic year start date. All relevant posts loaded.")
                                allPostsFetchedUntilDate = true
                            } else {
                                currentOffset += postsPerPage
                                Log.d("NewsViewModel", "Next offset: $currentOffset")
                            }
                        }
                    } catch (e: Exception) {
                        Log.e("NewsViewModel", "Parsing or processing error", e)
                        _errorMessage.postValue("Ошибка обработки данных: ${e.localizedMessage}")
                        allPostsFetchedUntilDate = true
                    } finally {
                        _isLoading.postValue(false)
                    }
                }
            },
            { error ->
                Log.e("NewsViewModel", "Volley error: ${error.message}")
                _errorMessage.postValue("Ошибка загрузки новостей: ${error.message}")
                allPostsFetchedUntilDate = true
                _isLoading.postValue(false)
            }
        )
        VolleySingleton.getInstance(getApplication<Application>().applicationContext).addToRequestQueue(jsonObjectRequest)
    }

    private fun getAcademicYearStartTimestamp(): Long {
        val calendar = Calendar.getInstance()
        val currentMonth = calendar.get(Calendar.MONTH)

        if (currentMonth < Calendar.SEPTEMBER) {
            calendar.add(Calendar.YEAR, -1)
        }

        calendar.set(Calendar.MONTH, Calendar.SEPTEMBER)
        calendar.set(Calendar.DAY_OF_MONTH, 1)
        calendar.set(Calendar.HOUR_OF_DAY, 0)
        calendar.set(Calendar.MINUTE, 0)
        calendar.set(Calendar.SECOND, 0)
        calendar.set(Calendar.MILLISECOND, 0)

        return calendar.timeInMillis / 1000
    }
}