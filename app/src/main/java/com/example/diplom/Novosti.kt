// src/main/java/com/example/diplom/Novosti.kt
package com.example.diplom

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ProgressBar
import android.widget.Toast
import androidx.fragment.app.Fragment
// import androidx.fragment.app.activityViewModels // Удалите этот импорт, если он все еще здесь
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import com.example.diplom.adapters.PostAdapter
import com.example.diplom.viewmodels.NewsViewModel

class Novosti : Fragment() {

    private lateinit var recyclerView: RecyclerView
    private lateinit var progressBar: ProgressBar
    private lateinit var swipeRefreshLayout: SwipeRefreshLayout

    private val postAdapter = PostAdapter()

    private lateinit var newsViewModel: NewsViewModel

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_novosti, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        recyclerView = view.findViewById(R.id.recyclerView)
        progressBar = view.findViewById(R.id.progressBar)
        swipeRefreshLayout = view.findViewById(R.id.swipeRefreshLayout)

        // Получаем ViewModel из класса Application
        newsViewModel = (requireActivity().application as MyApplication).getNewsViewModel()

        setupRecyclerView()
        Log.d("NovostiFragment", "onViewCreated: View фрагмента создана. Получение ViewModel.")
        setupObservers()
        Log.d("NovostiFragment", "setupObservers: Наблюдатели настроены.")
        setupSwipeRefresh()
    }

    private fun setupRecyclerView() {
        recyclerView.apply {
            adapter = postAdapter
            layoutManager = LinearLayoutManager(context)

            addOnScrollListener(object : RecyclerView.OnScrollListener() {
                override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
                    super.onScrolled(recyclerView, dx, dy)
                    val layoutManager = recyclerView.layoutManager as LinearLayoutManager
                    val visibleItemCount = layoutManager.childCount
                    val totalItemCount = layoutManager.itemCount
                    val firstVisibleItemPosition = layoutManager.findFirstVisibleItemPosition()

                    if ((visibleItemCount + firstVisibleItemPosition) >= totalItemCount - 5 && firstVisibleItemPosition >= 0) {
                        newsViewModel.loadMorePosts()
                    }
                }
            })
        }
    }

    private fun setupObservers() {
        Log.d("NovostiFragment", "setupObservers: Настройка наблюдателей для постов и isLoading.")
        newsViewModel.posts.observe(viewLifecycleOwner) { posts ->
            postAdapter.submitList(posts)
            Log.d("NovostiFragment", "Observed ${posts.size} posts from ViewModel and submitted to adapter.")
            if (posts.isNotEmpty()) {
                progressBar.visibility = View.GONE
            }
        }

        newsViewModel.isLoading.observe(viewLifecycleOwner) { isLoading ->
            if (!swipeRefreshLayout.isRefreshing) {
                progressBar.visibility = if (isLoading) View.VISIBLE else View.GONE
            }
            if (!isLoading) {
                swipeRefreshLayout.isRefreshing = false
            }
        }

        newsViewModel.errorMessage.observe(viewLifecycleOwner) { message ->
            message?.let {
                Toast.makeText(context, it, Toast.LENGTH_LONG).show()
                // !!! ИСПРАВЛЕНИЕ ЗДЕСЬ: Вызываем метод ViewModel для очистки ошибки
                newsViewModel.clearErrorMessage()
            }
        }
    }

    private fun setupSwipeRefresh() {
        swipeRefreshLayout.setOnRefreshListener {
            Log.d("NovostiFragment", "SwipeRefresh triggered.")
            newsViewModel.refreshPosts()
        }
    }
}