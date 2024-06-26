package com.ssafy.final_pennant_preset

import android.annotation.SuppressLint
import android.app.Notification
import android.app.NotificationManager.IMPORTANCE_HIGH
import android.content.ContentUris
import android.content.Context
import android.graphics.BitmapFactory
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.os.Bundle
import android.os.Environment
import android.provider.MediaStore
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.SeekBar
import android.widget.TextView
import android.widget.Toast
import androidx.core.net.toFile
import androidx.core.net.toUri
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import androidx.fragment.app.activityViewModels
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.exoplayer2.ExoPlayer
import com.google.android.exoplayer2.MediaItem
import com.google.android.exoplayer2.Player
import com.google.android.exoplayer2.ui.PlayerNotificationManager
import com.google.android.exoplayer2.ui.PlayerNotificationManager.NotificationListener
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.android.material.navigation.NavigationBarView
import com.ssafy.final_pennant.R
import com.ssafy.final_pennant.databinding.FragmentSongBinding
import com.ssafy.final_pennant_preset.config.ApplicationClass
import com.ssafy.final_pennant_preset.dto.MusicDTO
import com.ssafy.final_pennant_preset.dto.MusicFileViewModel
import com.ssafy.final_pennant_preset.dto.PlayListDTO
import java.io.File
import java.util.concurrent.TimeUnit


private const val TAG = "fragment_song_싸피"

class fragment_song : Fragment() {

    private var _binding: FragmentSongBinding? = null
    private val binding: FragmentSongBinding
        get() = _binding!!
    private lateinit var songlistadapter: SongListAdapter
    final var notificationId = 5

    val musicviewmodel: MusicFileViewModel by activityViewModels()
    val mainActivityViewModel: MainActivityViewModel by activityViewModels()

    var uri: Uri = MediaStore.Audio.Media.EXTERNAL_CONTENT_URI
    private lateinit var player: ExoPlayer
//    lateinit var playerNotificationManager: PlayerNotificationManager

    private var list = mutableListOf<MusicDTO>()
    lateinit var mediaItem: MediaItem

    private val updateSeekRunnable = Runnable {
        updateSeek()
    }

    override fun onAttach(context: Context) {
        player = MainActivity.getPlayer(context)
        super.onAttach(context)
    }
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        requireActivity().supportFragmentManager.popBackStack(
            null,
            FragmentManager.POP_BACK_STACK_INCLUSIVE
        )
        requireActivity().findViewById<BottomNavigationView>(R.id.bottomNavView).apply {
            menu.findItem(R.id.btnTotalFile).isCheckable = false
            menu.findItem(R.id.btnPlayList).isCheckable = false
            menu.findItem(R.id.btnCurrentList).isCheckable = false
            menu.findItem(R.id.btnConnectServer).isCheckable = false
            labelVisibilityMode = NavigationBarView.LABEL_VISIBILITY_UNLABELED
        }

        musicviewmodel.playerNotificationManager =
            PlayerNotificationManager.Builder(requireActivity(), notificationId, "MS Office")
                .setNotificationListener(object : PlayerNotificationManager.NotificationListener {
                    override fun onNotificationPosted(
                        notificationId: Int,
                        notification: Notification,
                        ongoing: Boolean
                    ) {
                        super.onNotificationPosted(notificationId, notification, ongoing)
                        if (ongoing) {
                            Log.d(TAG, "onNotificationPosted: 재생 중이다")
                            Log.d(TAG, "onNotificationPosted: ${notification.actions}")
                        } else {
                            Log.d(TAG, "onNotificationPosted: 멈췄다")
                        }
                    }

                    override fun onNotificationCancelled(
                        notificationId: Int,
                        dismissedByUser: Boolean
                    ) {
                        super.onNotificationCancelled(notificationId, dismissedByUser)
                    }
                })
                .setChannelImportance(IMPORTANCE_HIGH)
                .setSmallIconResourceId(R.drawable.music_ssafy_office)
                .setChannelDescriptionResourceId(R.string.app_name)
                .setPreviousActionIconResourceId(R.drawable.img_skipprevious)
                .setPauseActionIconResourceId(R.drawable.img_pause)
                .setPlayActionIconResourceId(R.drawable.img_play)
                .setNextActionIconResourceId(R.drawable.img_skipnext)
                .setChannelNameResourceId(R.string.app_name)
                .build()


        musicviewmodel.playerNotificationManager.setUseNextAction(true)
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        _binding = FragmentSongBinding.inflate(inflater, container, false)

        musicviewmodel.selectedPlaylistName = ApplicationClass.sSharedPreferences.getCurSongList()
        musicviewmodel.selectedMusicPosition =
            ApplicationClass.sSharedPreferences.getSelectedSongPosition()
        if (musicviewmodel.selectedPlaylistName == "") {
            if (!musicviewmodel.selectedMusic.equals(MusicDTO(-1, "", -1, "", ""))) {
                //전체 곡 목록에서 한 곡 재생시킨 경우
                list = mutableListOf<MusicDTO>() //빈 재생목록 연결

                binding.trackTextView.text = musicviewmodel.selectedMusic.title
                binding.artistTextView.text = musicviewmodel.selectedMusic.artist

//                var musicImg = MediaMetadataRetriever()
//                musicImg.setDataSource(
//                    requireContext(),
//                    ContentUris.withAppendedId(uri, musicviewmodel.selectedMusic.id)
//                )
//                var insertImg = musicImg.embeddedPicture
//                if (insertImg != null) {
//                    var bitmap = BitmapFactory.decodeByteArray(insertImg, 0, insertImg.size)
//                    binding.coverImageView.setImageBitmap(bitmap)
//                }
//                else{
//                    binding.coverImageView.setImageResource(R.drawable.music_ssafy_office)
//                }
            }
        } else {
            //재생목록이 있는 경우
            list =
                ApplicationClass.sSharedPreferences.getSongList(musicviewmodel.selectedPlaylistName)

            if (musicviewmodel.selectedMusicPosition != -1 && musicviewmodel.selectedMusicPosition < list.size) {
                musicviewmodel.selectedMusic = list.get(musicviewmodel.selectedMusicPosition)
                Log.d(TAG, "onCreateView: ${musicviewmodel.selectedMusic}")
                binding.trackTextView.text = musicviewmodel.selectedMusic.title
                binding.artistTextView.text = musicviewmodel.selectedMusic.artist

                var musicImg = MediaMetadataRetriever()
                musicImg.setDataSource(
                    requireContext(),
                    ContentUris.withAppendedId(uri, musicviewmodel.selectedMusic.id)
                )
                var insertImg = musicImg.embeddedPicture
                if (insertImg != null) {
                    var bitmap = BitmapFactory.decodeByteArray(insertImg, 0, insertImg.size)
                    binding.coverImageView.setImageBitmap(bitmap)
                }
            } else {
                //선택한 곡이 없는 경우
                binding.trackTextView.text = "재생할 곡을 선택해주세요"
                binding.artistTextView.text = ""
                binding.coverImageView.setImageResource(R.drawable.music_ssafy_office)
            }
        }
        musicviewmodel.selectedPlayList = PlayListDTO(musicviewmodel.selectedPlaylistName, list)
        songlistadapter = SongListAdapter(musicviewmodel.selectedPlayList.songlist)

        return binding.root
    }

    @SuppressLint("SuspiciousIndentation")
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

//        player = MainActivity.getPlayer(requireContext())
//        binding.playerView.showTimeoutMs = 0
        binding.playerView.player = player

        musicviewmodel.playerNotificationManager.setPlayer(player)

        binding.playListRecyclerView.apply {
            adapter = adapter
            this.layoutManager = LinearLayoutManager(requireActivity())
            addItemDecoration(CustomItemDecoration(requireContext()))
        }

        if (musicviewmodel.selectedMusicPosition != -1 && musicviewmodel.selectedPlaylistName != "" && musicviewmodel.selectedMusic.id.toInt() != -1) {
            //재생할 곡 player 연결

            ApplicationClass.sSharedPreferences.putSelectedSongPosition(musicviewmodel.selectedMusicPosition)

            Log.d(TAG, "onViewCreated: ${musicviewmodel.selectedMusic}")
            binding.trackTextView.text = musicviewmodel.selectedMusic.title
            binding.artistTextView.text = musicviewmodel.selectedMusic.artist

            var musicImg = MediaMetadataRetriever()

            musicImg.setDataSource(
                requireContext(),
                ContentUris.withAppendedId(uri, musicviewmodel.selectedMusic.id)
            )
            var insertImg = musicImg.embeddedPicture
            if (insertImg != null) {
                var bitmap = BitmapFactory.decodeByteArray(insertImg, 0, insertImg.size)
                binding.coverImageView.setImageBitmap(bitmap)
            }
            else{
                binding.coverImageView.setImageResource(R.drawable.music_ssafy_office)
            }

            mediaItem = MediaItem.fromUri("${uri}/${musicviewmodel.selectedMusic.id}")

            if(musicviewmodel.checkSameSong){
                if (player.isPlaying) player.stop()
                player.setMediaItem(mediaItem, musicviewmodel.isPlayingOn)
            }
            else player.setMediaItem(mediaItem,0)

            musicviewmodel.checkSameSong = true

            binding.playControlImageView.setImageResource(R.drawable.img_pause)

        }
        else{

            //한 곡 재생인 경우
            if(musicviewmodel.downloadedUri != ""){
                Log.d(TAG, "onViewCreated: 여기로 와주세요 제발 ")
                mediaItem = MediaItem.fromUri(getDownloadFileToUri()!!)
                var musicImg = MediaMetadataRetriever()
                musicImg.setDataSource(requireContext(), getDownloadFileToUri())
                var insertImg = musicImg.embeddedPicture
                if (insertImg != null) {
                    var bitmap = BitmapFactory.decodeByteArray(insertImg, 0, insertImg.size)
                    binding.coverImageView.setImageBitmap(bitmap)
                }
                else{
                    binding.coverImageView.setImageResource(R.drawable.music_ssafy_office)
                }
            }
            else if(!musicviewmodel.selectedMusic.equals(MusicDTO(-1, "", -1, "", ""))){
                mediaItem = MediaItem.fromUri("${uri}/${musicviewmodel.selectedMusic.id}")
                val filePath = getFilePathUri(("${uri}/${musicviewmodel.selectedMusic.id}").toUri())
                val file = File(filePath)
                var musicImg = MediaMetadataRetriever()
                musicImg.setDataSource(requireContext(), file.toUri())

                var insertImg = musicImg.embeddedPicture
                if (insertImg != null) {
                    var bitmap = BitmapFactory.decodeByteArray(insertImg, 0, insertImg.size)
                    binding.coverImageView.setImageBitmap(bitmap)
                }
                else{
                    binding.coverImageView.setImageResource(R.drawable.music_ssafy_office)
                }
            }
        }

        player.setMediaItem(mediaItem, 0)
        binding.playControlImageView.setImageResource(R.drawable.img_pause)
        player.prepare()
        player.play()
        musicviewmodel.isPlaying = true

        Log.d(TAG, "onViewCreated: ${ApplicationClass.sSharedPreferences.getUID()}")

        initPlayView()
        initPlayListButton()
        initPlayControlButtons()
        initSeekBar()
        initRecyclerView()
    }

    private fun getFilePathUri(uri: Uri) : String{

        var columnIndex = 0
        val proj = arrayOf(MediaStore.Images.Media.DATA)
        var cursor = requireActivity().contentResolver.query(uri, proj, null, null, null)

        if (cursor!!.moveToFirst()){
            columnIndex = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DATA)
        }

        return cursor.getString(columnIndex)
    }


    override fun onDetach() {
        super.onDetach()
//        player.stop()
//        player.release()
    }

    inner class SongListAdapter(val songList: MutableList<MusicDTO>) :
        RecyclerView.Adapter<SongListAdapter.SongListViewHolder>() {

        var uri: Uri = MediaStore.Audio.Media.EXTERNAL_CONTENT_URI

        inner class SongListViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
            var title = itemView.findViewById<TextView>(R.id.tvNowPlayingSongTitle)
            var artist = itemView.findViewById<TextView>(R.id.tvNowPlayingSongArtist)
            var genre = itemView.findViewById<TextView>(R.id.tvNowPlayingSongGenre)
            var img = itemView.findViewById<ImageView>(R.id.tvNowPlayingSongAlbumImage)

            fun bind(music: MusicDTO) {
                title.text = music.title
                artist.text = music.artist
                genre.text = music.genre

                var musicImg = MediaMetadataRetriever()
                musicImg.setDataSource(requireContext(), ContentUris.withAppendedId(uri, music.id))
                var insertImg = musicImg.embeddedPicture

                if (insertImg != null) {
                    var bitmap = BitmapFactory.decodeByteArray(insertImg, 0, insertImg.size)
                    img.setImageBitmap(bitmap)
                }
                else{
                    binding.coverImageView.setImageResource(R.drawable.music_ssafy_office)
                }

            }
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): SongListViewHolder {
            val view =
                LayoutInflater.from(parent.context)
                    .inflate(R.layout.now_playing_songs_item, parent, false)
            return SongListViewHolder(view).apply {

                //곡 클릭시 해당 곡으로 새로 실행

                itemView.setOnClickListener {
                    Toast.makeText(
                        requireContext(),
                        "${songList[layoutPosition].title}을/를 재생합니다.",
                        Toast.LENGTH_SHORT
                    ).show()
                    ApplicationClass.sSharedPreferences.putSelectedSongPosition(layoutPosition)
                    musicviewmodel.selectedMusicPosition = layoutPosition
                    musicviewmodel.selectedMusic =
                        musicviewmodel.selectedPlayList.songlist.get(layoutPosition)

                    //player view 전환
                    binding.trackTextView.text = musicviewmodel.selectedMusic.title
                    binding.artistTextView.text = musicviewmodel.selectedMusic.artist
                    var musicImg = MediaMetadataRetriever()
                    musicImg.setDataSource(
                        requireContext(),
                        ContentUris.withAppendedId(uri, musicviewmodel.selectedMusic.id)
                    )
                    var insertImg = musicImg.embeddedPicture
                    if (insertImg != null) {
                        var bitmap = BitmapFactory.decodeByteArray(insertImg, 0, insertImg.size)
                        binding.coverImageView.setImageBitmap(bitmap)
                    }
                    else{
                        binding.coverImageView.setImageResource(R.drawable.music_ssafy_office)
                    }

                    binding.playListGroup.isVisible = binding.playerViewGroup.isVisible.also {
                        binding.playerViewGroup.isVisible = binding.playListGroup.isVisible
                    }

                    mediaItem =
                        MediaItem.fromUri("${uri}/${musicviewmodel.selectedPlayList.songlist[musicviewmodel.selectedMusicPosition].id}")
                    player.setMediaItem(mediaItem, 0)
                    player.prepare()
                }

            }
        }

        override fun onBindViewHolder(holder: SongListAdapter.SongListViewHolder, position: Int) {
            holder.bind(songList.get(position))
        }

        override fun getItemCount(): Int {
            return songList.size
        }
    }

    @SuppressLint("ClickableViewAccessibility")
    private fun initSeekBar() {
        binding.playerSeekBar.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
            }

            override fun onStartTrackingTouch(seekBar: SeekBar?) {
            }

            override fun onStopTrackingTouch(seekBar: SeekBar) {
                player.seekTo(seekBar.progress * 1000L)
            }
        })
    }

    private fun initPlayControlButtons() {

        // 재생 or 일시정지 버튼
        binding.playControlImageView.setOnClickListener {
            if (player.isPlaying) {
                Log.d(TAG, "initPlayControlButtons: player.isPlaying")
                player.pause()
                musicviewmodel.isPlaying = false
                binding.playControlImageView.setImageResource(R.drawable.img_play)
            } else {
                Log.d(TAG, "initPlayControlButtons: player.isNotPlaying")
                player.play()
                binding.playControlImageView.setImageResource(R.drawable.img_pause)
            }
        }

        if(musicviewmodel.selectedMusicPosition==-1&&musicviewmodel.selectedPlaylistName==""&&musicviewmodel.selectedPlayList.equals(PlayListDTO("", mutableListOf<MusicDTO>()))){
            //한 곡 재생인 경우 데이터 재바인딩 과정 불필요

            if(musicviewmodel.downloadedUri!=""){
                binding.trackTextView.text = musicviewmodel.selectedMusic.title
                binding.artistTextView.text = musicviewmodel.selectedMusic.artist

//                var musicImg = MediaMetadataRetriever()
//                Log.d(TAG, "onCreateView: ${musicviewmodel.selectedMusic.id}")
//                musicImg.setDataSource(
//                    requireContext(),
//                    ContentUris.withAppendedId(uri, musicviewmodel.selectedMusic.id)
//                )
//                var insertImg = musicImg.embeddedPicture
//                if (insertImg != null) {
//                    var bitmap = BitmapFactory.decodeByteArray(insertImg, 0, insertImg.size)
//                    binding.coverImageView.setImageBitmap(bitmap)
//                }
//                else{
//                    binding.coverImageView.setImageResource(R.drawable.music_ssafy_office)
//                }
            }

            else{
                Log.d(TAG, "initPlayControlButtons: 설마 여긴 아니지?")
                binding.skipNextImageView.setOnClickListener {
                    player.setMediaItem(mediaItem, 0) // startPositionsMs=0 초 부터 시작
                    player.play()
                }
                binding.skipPrevImageView.setOnClickListener {
                    player.setMediaItem(mediaItem, 0) // startPositionsMs=0 초 부터 시작
                    player.play()
                }
            }
        }
        else{
            binding.skipNextImageView.setOnClickListener {
                musicviewmodel.selectedMusicPosition = musicviewmodel.selectedMusicPosition + 1
                musicviewmodel.selectedMusicPosition =
                    musicviewmodel.selectedMusicPosition % musicviewmodel.selectedPlayList.songlist.size

                musicviewmodel.selectedMusic =
                    musicviewmodel.selectedPlayList.songlist[musicviewmodel.selectedMusicPosition]
                binding.trackTextView.text = musicviewmodel.selectedMusic.title
                binding.artistTextView.text = musicviewmodel.selectedMusic.artist

                var musicImg = MediaMetadataRetriever()
                Log.d(TAG, "onCreateView: ${musicviewmodel.selectedMusic.id}")
                musicImg.setDataSource(
                    requireContext(),
                    ContentUris.withAppendedId(uri, musicviewmodel.selectedMusic.id)
                )
                var insertImg = musicImg.embeddedPicture
                if (insertImg != null) {
                    var bitmap = BitmapFactory.decodeByteArray(insertImg, 0, insertImg.size)
                    binding.coverImageView.setImageBitmap(bitmap)
                }
                else{
                    binding.coverImageView.setImageResource(R.drawable.music_ssafy_office)
                }

                if (binding.playListGroup.isVisible) {
                    binding.playListGroup.isVisible = !binding.playListGroup.isVisible
                }

                ApplicationClass.sSharedPreferences.putSelectedSongPosition(musicviewmodel.selectedMusicPosition)
                playMusic(musicviewmodel.selectedMusicPosition)

            }

            binding.skipPrevImageView.setOnClickListener {
                musicviewmodel.selectedMusicPosition = musicviewmodel.selectedMusicPosition - 1
                if (musicviewmodel.selectedMusicPosition < 0) {
                    musicviewmodel.selectedMusicPosition =
                        musicviewmodel.selectedPlayList.songlist.size - 1
                }

                musicviewmodel.selectedMusic =
                    musicviewmodel.selectedPlayList.songlist[musicviewmodel.selectedMusicPosition]
                binding.trackTextView.text = musicviewmodel.selectedMusic.title
                binding.artistTextView.text = musicviewmodel.selectedMusic.artist

                var musicImg = MediaMetadataRetriever()
                Log.d(TAG, "onCreateView: ${musicviewmodel.selectedMusic.id}")
                musicImg.setDataSource(
                    requireContext(),
                    ContentUris.withAppendedId(uri, musicviewmodel.selectedMusic.id)
                )
                var insertImg = musicImg.embeddedPicture
                if (insertImg != null) {
                    var bitmap = BitmapFactory.decodeByteArray(insertImg, 0, insertImg.size)
                    binding.coverImageView.setImageBitmap(bitmap)
                }
                else{
                    binding.coverImageView.setImageResource(R.drawable.music_ssafy_office)
                }

                ApplicationClass.sSharedPreferences.putSelectedSongPosition(musicviewmodel.selectedMusicPosition)
                playMusic(musicviewmodel.selectedMusicPosition)
            }
        }
    }

    private fun initPlayView() {

        player.addListener(object : Player.Listener {
            // 미디어 아이템이 바뀔 때
            override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) {
                super.onMediaItemTransition(mediaItem, reason)

                Log.d(TAG, "onMediaItemTransition: ${mediaItem?.mediaId}")
                val newIndex: String = mediaItem?.mediaId ?: return
            }

            // 재생, 재생완료, 버퍼링 상태 ...
            override fun onPlaybackStateChanged(state: Int) {
                super.onPlaybackStateChanged(state)

                updateSeek()
            }
        })
    }

    private fun updateSeek() {
        val player = this.player ?: return
        val duration = if (player.duration >= 0) player.duration else 0 // 전체 음악 길이
        val position = player.currentPosition

        updateSeekUi(duration, position)

        val state = player.playbackState

        view?.removeCallbacks(updateSeekRunnable)
        // 재생 중 일때 (대 중이 아니거나, 재생이 끝나지 않은 경우)
        if (state != Player.STATE_IDLE && state != Player.STATE_ENDED) {
            view?.postDelayed(updateSeekRunnable, 100) // 1초에 한번씩 실행
        }

    }

    private fun updateSeekUi(duration: Long, position: Long) {
        binding.playListSeekBar.max = (duration / 1000).toInt() // 총 길이를 설정. 1000으로 나눠 작게
        binding.playListSeekBar.progress = (position / 1000).toInt() // 동일하게 1000으로 나눠 작게

        binding.playerSeekBar.max = (duration / 1000).toInt()
        binding.playerSeekBar.progress = (position / 1000).toInt()

        musicviewmodel.isPlayingOn = player.currentPosition
        Log.d(TAG, "updateSeekUi: 시간 업데이트? : ${musicviewmodel.isPlayingOn}")
        binding.playTimeTextView.text = String.format(
            "%02d:%02d",
            TimeUnit.MINUTES.convert(position, TimeUnit.MILLISECONDS), // 현재 분
            (position / 1000) % 60 // 분 단위를 제외한 현재 초
        )
        binding.totalTimeTextView.text = String.format(
            "%02d:%02d",
            TimeUnit.MINUTES.convert(duration, TimeUnit.MILLISECONDS), // 전체 분
            (duration / 1000) % 60 // 분 단위를 제외한 초
        )

        if (binding.playTimeTextView.text.equals(binding.totalTimeTextView.text) && !binding.playTimeTextView.text.equals(
                "00:00"
            )
        ) {
            Log.d(TAG, "updateSeekUi: ")

            if(musicviewmodel.downloadedUri==""){
                musicviewmodel.selectedMusicPosition = musicviewmodel.selectedMusicPosition + 1
                musicviewmodel.selectedMusicPosition =
                    musicviewmodel.selectedMusicPosition % musicviewmodel.selectedPlayList.songlist.size

                musicviewmodel.selectedMusic =
                    musicviewmodel.selectedPlayList.songlist[musicviewmodel.selectedMusicPosition]
                binding.trackTextView.text = musicviewmodel.selectedMusic.title
                binding.artistTextView.text = musicviewmodel.selectedMusic.artist

                ApplicationClass.sSharedPreferences.putSelectedSongPosition(musicviewmodel.selectedMusicPosition)

                var musicImg = MediaMetadataRetriever()
                Log.d(TAG, "onCreateView: ${musicviewmodel.selectedMusic.id}")
                musicImg.setDataSource(
                    requireContext(),
                    ContentUris.withAppendedId(uri, musicviewmodel.selectedMusic.id)
                )
                var insertImg = musicImg.embeddedPicture
                if (insertImg != null) {
                    var bitmap = BitmapFactory.decodeByteArray(insertImg, 0, insertImg.size)
                    binding.coverImageView.setImageBitmap(bitmap)
                }
                else{
                    binding.coverImageView.setImageResource(R.drawable.music_ssafy_office)
                }
                playMusic(musicviewmodel.selectedMusicPosition)
                musicviewmodel.downloadedUri=""
            }
            else{
                MainActivity.getPlayer(requireContext()).stop()
            }

            if (binding.playListGroup.isVisible) {
                binding.playListGroup.isVisible = !binding.playListGroup.isVisible
            }

        }
    }

    private fun updatePlayerView(music: MusicDTO) {
        binding.trackTextView.text = music.title
        binding.artistTextView.text = music.artist

        var uri: Uri = MediaStore.Audio.Media.EXTERNAL_CONTENT_URI
        var musicImg = MediaMetadataRetriever()
        musicImg.setDataSource(requireContext(), ContentUris.withAppendedId(uri, music.id))
        var insertImg = musicImg.embeddedPicture
        if (insertImg != null) {
            var bitmap = BitmapFactory.decodeByteArray(insertImg, 0, insertImg.size)
            binding.coverImageView.setImageBitmap(bitmap)
        }
        else{
            binding.coverImageView.setImageResource(R.drawable.music_ssafy_office)
        }

    }

    private fun initRecyclerView() {
        songlistadapter = SongListAdapter(musicviewmodel.selectedPlayList.songlist)

        binding.playListRecyclerView.adapter = songlistadapter
        binding.playListRecyclerView.layoutManager = LinearLayoutManager(requireActivity())
    }

    private fun initPlayListButton() {
        binding.playListImageView.setOnClickListener {
            // 강의 와는 다르게 구현
            binding.playListGroup.isVisible = binding.playerViewGroup.isVisible.also {
                binding.playerViewGroup.isVisible = binding.playListGroup.isVisible
            }
        }
    }
    fun getDownloadFileToUri(): Uri? {
        val file =
            File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS).toString() + mainActivityViewModel.downloadFile)
        Log.d(TAG, "getDownloadFileToUri: $file")
        if (file.isFile) return file.toUri()
        return null
    }

    fun getCurFile(): File {
        return File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS).toString() + mainActivityViewModel.downloadFile)
    }
//    private fun setMusicList(modelList: List<MusicModel>) {
//        player ?: return
//        context?.let {
//            player?.addMediaItems(modelList.map { musicModel ->
//                MediaItem.Builder()
//                    .setMediaId(musicModel.id.toString()) // 미디어 아이디를 musicModel id로
//                    .setUri(musicModel.streamUrl)
//                    .build()
//                /*
//                미디어 아이템에 2가지 태그 지정 가능
//                미디어 id, 뷰에 태그 지정했듯 미디어 아이템에 태그 지정 가능
//                 */
//            })
//
//            player?.prepare()
//        }
//    }

    private fun playMusic(position: Int) {
//        model.updateCurrentPosition(musicModel)
//        player?.seekTo(model.currentPosition, 0) // positionsMs=0 초 부터 시작
//        player?.play()

        var mediaItem =
            MediaItem.fromUri("${MediaStore.Audio.Media.EXTERNAL_CONTENT_URI}/${musicviewmodel.selectedPlayList.songlist[position].id}")

        binding.playerViewGroup.visibility = View.VISIBLE

        player?.setMediaItem(mediaItem, 0) // startPositionsMs=0 초 부터 시작
        player?.play()
    }

//    override fun onStop() {
//        super.onStop()
//
//        player?.pause()
//        view?.removeCallbacks(updateSeekRunnable)
//    }

//    override fun onDestroy() {
//        super.onDestroy()
//
//        _binding = null
//        player?.release()
//        view?.removeCallbacks(updateSeekRunnable)
//    }
}
