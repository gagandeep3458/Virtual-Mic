package com.cuttingedge.virtualmic

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaRecorder
import androidx.core.app.ActivityCompat
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch
import timber.log.Timber
import java.net.DatagramPacket
import java.net.DatagramSocket
import java.net.InetAddress
import javax.inject.Inject

@HiltViewModel
class HomeViewModel @Inject constructor(@ApplicationContext context: Context) : ViewModel() {

    private val ctx = lazy { context }

    sealed class HomeEvents {
        data class Error(val message: String) : HomeEvents()
        data class InfoMessage(val message: String) : HomeEvents()
        data class NavigateTo(val destination: Int) : HomeEvents()
    }

    private val channel = Channel<HomeEvents>()
    val homeEventsFlow = channel.receiveAsFlow()

    private var samplingRate = Constants.DEFAULT_SAMPLING_RATE
    private var networkPort = Constants.DEFAULT_NETWORK_PORT
    private val channelConfig: Int = AudioFormat.CHANNEL_IN_FRONT
    private val audioFormat: Int = AudioFormat.ENCODING_PCM_16BIT
    var minBufSize = AudioRecord.getMinBufferSize(samplingRate, channelConfig, audioFormat)

    var ip = ""

    private lateinit var audioRecorder: AudioRecord

    private val _appState = MutableLiveData<AppState>()
    val appState: LiveData<AppState> get() = _appState

    fun startStreaming(ipAddress: String) {
        ip = ipAddress
        viewModelScope.launch(Dispatchers.IO) {
            _appState.postValue(AppState.Streaming)
            channel.send(HomeEvents.InfoMessage("Stream Started!"))
            if (ActivityCompat.checkSelfPermission(
                    ctx.value,
                    Manifest.permission.RECORD_AUDIO
                ) == PackageManager.PERMISSION_GRANTED
            ) {
                try {
                    val socket = DatagramSocket()
                    val byteBuff = ByteArray(minBufSize)

                    val destination = InetAddress.getByName(ipAddress)

                    audioRecorder = AudioRecord(
                        MediaRecorder.AudioSource.MIC,
                        samplingRate,
                        channelConfig,
                        audioFormat,
                        minBufSize * 10
                    )

                    audioRecorder.startRecording()

                    while (appState.value == AppState.Streaming) {

                        audioRecorder.read(byteBuff, 0, byteBuff.size)

                        val packet =
                            DatagramPacket(byteBuff, byteBuff.size, destination, networkPort)

                        Timber.d(packet.data.joinToString())

                        socket.send(packet)
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                    audioRecorder.release()
                    _appState.postValue(AppState.NotStreaming)
                    channel.send(HomeEvents.Error("Something went wrong. Please try again."))
                }
            } else {
                channel.send(HomeEvents.Error("Permission is required to send audio!"))
            }
        }
    }

    fun stopStreaming() {
        audioRecorder.release()
        _appState.postValue(AppState.NotStreaming)
        viewModelScope.launch {
            channel.send(HomeEvents.InfoMessage("Stream Stopped!"))
        }
    }
}