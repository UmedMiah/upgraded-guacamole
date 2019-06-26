package dev.bmcreations.guacamole.ui.library

import android.content.Context
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.Transformations
import androidx.lifecycle.ViewModel
import androidx.paging.LivePagedListBuilder
import dev.bmcreations.guacamole.auth.TokenProvider
import dev.bmcreations.musickit.networking.extensions.uiScope
import dev.bmcreations.musickit.networking.Outcome
import dev.bmcreations.musickit.networking.api.models.*
import dev.bmcreations.musickit.networking.api.music.repository.MusicRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.jetbrains.anko.AnkoLogger
import org.jetbrains.anko.info
import androidx.paging.PagedList
import dev.bmcreations.musickit.networking.NetworkState
import dev.bmcreations.musickit.networking.api.music.sources.RecentlyAddedDataFactory
import java.util.concurrent.Executors


class LibraryViewModel(context: Context): ViewModel(), AnkoLogger {

    private val musicRepo by lazy {
        MusicRepository.getInstance(TokenProvider.with(context))
    }

    var recentsNetworkState: LiveData<NetworkState>? = null
    var recentlyAdded: LiveData<PagedList<RecentlyAddedEntity>>? = null

    private var _playlists: List<LibraryPlaylist> = listOf()
    set(value) {
        field = value
        playlists.value = value
    }
    val playlists = MutableLiveData<List<LibraryPlaylist>>()

    val selected: MutableLiveData<LibraryResult?> = MutableLiveData()

    init {
        initializePlaylists()
        initializeRecents()

        selected.value = null
    }

    private fun initializePlaylists() {
        uiScope.launch(Dispatchers.IO) {
            musicRepo.getAllLibraryPlaylists().let {
                when (it) {
                    is Outcome.Success -> {
                        uiScope.launch { _playlists = it.data }
                    }
                    else -> {}
                }
            }
        }
    }

    private fun initializeRecents() {
        val recentFactory = RecentlyAddedDataFactory().apply {
            this.provideMusicRepository(musicRepo)
        }
        recentsNetworkState = Transformations.switchMap(recentFactory.mutableLiveData) { source -> source.networkState }
        val pagedListConfig = PagedList.Config.Builder()
            .setEnablePlaceholders(false)
            .setInitialLoadSizeHint(10)
            .setPageSize(10).build()

        recentlyAdded = LivePagedListBuilder(recentFactory, pagedListConfig)
            .setFetchExecutor(Executors.newFixedThreadPool(5))
            .build()
    }

    fun getLibraryAlbumById(id: String) {
        musicRepo.let { repo ->
            uiScope.launch(Dispatchers.IO) {
                val outcome = repo.getLibraryAlbumById(id)
                when (outcome) {
                    is Outcome.Success -> {
                        uiScope.launch { selected.value = Album(outcome.data) }
                    }
                    is Outcome.Failure -> {
                        info { outcome.e.localizedMessage }
                    }
                }
            }
        }
    }

    fun refreshLibraryPlaylists() {
        initializePlaylists()
    }

    fun getLibraryPlaylistById(id: String) {
        musicRepo.let { repo ->
            uiScope.launch(Dispatchers.IO) {
                val outcome = repo.getLibraryPlaylistById(id)
                when (outcome) {
                    is Outcome.Success -> {
                        uiScope.launch { selected.value = Playlist(outcome.data) }
                    }
                    is Outcome.Failure -> {
                        info { outcome.e.localizedMessage }
                    }
                }
            }
        }
    }

    fun getLibraryPlaylistWithTracksById(id: String) {
        musicRepo.let { repo ->
            uiScope.launch(Dispatchers.IO) {
                val outcome = repo.getLibraryPlaylistWithTracksById(id)
                when (outcome) {
                    is Outcome.Success -> {
                        uiScope.launch { selected.value = Playlist(outcome.data) }
                    }
                    is Outcome.Failure -> {
                        info { outcome.e.localizedMessage }
                    }
                }
            }
        }
    }
}