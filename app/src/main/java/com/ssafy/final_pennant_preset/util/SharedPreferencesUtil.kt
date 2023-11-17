package com.ssafy.final_pennant_preset.util

import android.content.Context
import android.content.SharedPreferences
import com.ssafy.final_pennant_preset.DTO.MusicDTO
import com.ssafy.final_pennant_preset.config.ApplicationClass
import java.lang.StringBuilder

class SharedPreferencesUtil(context: Context) {

    private val KEY_SONG_LIST_NAME = "songListName"
    private val KEY_CUR_SONG_LIST = "curSongList"

    private var preferences: SharedPreferences =
        context.getSharedPreferences(ApplicationClass.SHARED_PREFERENCES_NAME, Context.MODE_PRIVATE)

    fun addUserCookie(cookies: HashSet<String>) {
        val editor = preferences.edit()
        editor.putStringSet(ApplicationClass.COOKIES_KEY_NAME, cookies)
        editor.apply()
    }

    fun getUserCookie(): MutableSet<String>? {
        return preferences.getStringSet(ApplicationClass.COOKIES_KEY_NAME, HashSet())
    }

    fun getString(key:String): String? {
        return preferences.getString(key, null)
    }

    fun putID(value:String) {
        val editor = preferences.edit()
        editor.putString("userID", value)
        editor.apply()
    }

    fun getID(): String? {
        return preferences.getString("userID", null)
    }

    /**
     * param: 재생목록의 이름, 해당하는 재생목록의 MutableList<MusicDto>
     * return: 저장 여부(중복된 재생목록이 있는 경우)
     *
     * 재생목록을 저장하는 함수
     * 수정인 경우 덮어씌우고, 새로운 재생목록을 만드는 경우 동일한 이름의 기존 재생목록이 있다면 return false
     *
     * 구분인자
     * || -> MusicDTO 내부의 요소들을 구분
     * && -> List 내의 MusicDTO을 구분
     */
    fun putSongList(playListName: String, songList: MutableList<MusicDTO>, isModify: Boolean) : Boolean {

        if (!isModify) { // 새로 만드는 경우
            if (preferences.contains(playListName)) return false
            // 기존에 저장된 재생목록이 있다면 return false
        }

        val editor = preferences.edit()
        val sb = StringBuilder()

        songList.forEach {
            sb.append("${it.id}||${it.albumId}||${it.artist}||${it.genre}||${it.title}")
            sb.append("&&")
        }
        editor.putString(playListName, sb.toString())
        editor.apply()

        return true
    }

    /**
     * param: 원하는 재생목록의 이름
     * return: 해당하는 재생목록의 MutableList<MusicDto>
     */
    fun getSongList(playListName: String) : MutableList<MusicDTO>{
        val songList = mutableListOf<MusicDTO>()
        val songStr = preferences.getString(playListName, null)

        songStr?.let {
            val songArr = it.split("&&") // 하나의 MusicDTO를 가져옴
            for (song in songArr) {
                var element = song.split("||") // 하나의 MusicDTO 안의 요소들을 가져옴
                songList.add(MusicDTO(element[0].toLong(),
                    element[1], element[2].toLong(), element[3], element[4]
                ))
            }
        }
        return songList
    }

    /**
     * 현재 재생 목록을 저장하는 함수
     */
    fun putCurSongList(songList: MutableList<MusicDTO>) {
        putSongList(KEY_CUR_SONG_LIST, songList, true)
    }


    /**
     * 현재 재생 목록을 반환
     */
    fun getCurSongList() : MutableList<MusicDTO> {
        return getSongList(KEY_CUR_SONG_LIST)
    }


    /**
     * 저장된 재생목록들의 String 값들을 ArrayList로 반환하는 함수
     */
    fun getSongListName() : MutableList<String>{

        val songListNameList = mutableListOf<String>()

        val songListNameStr = preferences.getString(KEY_SONG_LIST_NAME, null)
        songListNameStr?.let {
            val names = it.split("&&")
            for (name in names) {
                songListNameList.add(name)
            }
        }

        return songListNameList
    }

    /**
     * 재생목록의 이름을 저장하는 함수
     */
    fun addSongListName(songListName: String) {
        val editor = preferences.edit()
        var songListName = preferences.getString(KEY_SONG_LIST_NAME, "")
        songListName += "${songListName}&&"
        editor.putString(KEY_SONG_LIST_NAME, songListName)
        editor.apply()
    }

}