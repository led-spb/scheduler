package ru.led.scheduler.library;

import org.json.JSONObject;

import android.media.AudioFormat;
import android.media.AudioRecord;
import android.media.MediaPlayer;
import android.media.MediaRecorder;
import android.net.Uri;
import android.util.Base64;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.InetSocketAddress;

import ru.led.scheduler.ServerThread;

public class MediaLibrary extends BaseLibrary {
    private  DatagramSocket socket;
    private  MediaPlayer mPlayer;
    private  AudioRecord mRecorder;
    private  int mRecordBufferSize;

    public MediaLibrary(ServerThread thread) {
	super(thread);
    }
    
    public JSONObject open(JSONObject params) throws Exception{
        if( mPlayer!=null ) throw new RuntimeException("Media player already playing...");
        mPlayer = MediaPlayer.create(mContext, Uri.parse( params.getString("url")) );
        mPlayer.start();

        return OKResponse();
    }
    
    public JSONObject stop(JSONObject params) throws Exception{
        if( mPlayer==null ) throw new RuntimeException("Media player not playing...");
        mPlayer.stop();

        mPlayer.release();
        mPlayer = null;
        return OKResponse();
    }


    public JSONObject startRecording(JSONObject params) throws Exception {
        int sampleRate = params.optInt("rate", 8000)
            ,channels  = AudioFormat.CHANNEL_IN_MONO
            ,format    = params.optInt("format", AudioFormat.ENCODING_PCM_16BIT);

        mRecordBufferSize = AudioRecord.getMinBufferSize( sampleRate, channels, format  );

        mRecorder = new AudioRecord(
              params.optInt("source", MediaRecorder.AudioSource.DEFAULT)
              ,sampleRate, channels, format, mRecordBufferSize
        );

        if(mRecorder.getState()!= AudioRecord.STATE_INITIALIZED )
            throw new Exception("Media record initialization error");
        mRecorder.startRecording();

        socket = new DatagramSocket();
        final int netBufferSize = 1024;

        new Thread(
                new Runnable() {
                    @Override
                    public void run() {
                        byte[] buffer = new byte[netBufferSize];
                        try {
                            DatagramPacket client_packet = new DatagramPacket(buffer, 128);
                            // wait first connection
                            socket.receive(client_packet );

                            InetAddress clientAddress = client_packet.getAddress();
                            int clientPort = client_packet.getPort();

                            while (mRecorder!=null && mRecorder.getRecordingState() == AudioRecord.RECORDSTATE_RECORDING) {
                                int bytes = mRecorder.read(buffer, 0, netBufferSize);
                                if (bytes > 0) {
                                    DatagramPacket packet = new DatagramPacket(buffer, bytes, clientAddress, clientPort);
                                    socket.send(packet);
                                }
                            }
                        }catch(IOException e){
                        }
                        socket.close();
                    }
                }
        ).start();


        JSONObject result = new JSONObject();

        result.put( "sampleRate", sampleRate  );
        result.put( "bitsPerSample",  format==AudioFormat.ENCODING_PCM_16BIT?16:8 );
        result.put( "serverPort", socket.getLocalPort() );

        return OKResponse(result);
    }

    public JSONObject getRecordBuffer(JSONObject params) throws Exception{
        byte[] buffer = new byte[mRecordBufferSize*2];
        if(mRecorder!=null){
            if(mRecorder.getRecordingState() != AudioRecord.RECORDSTATE_RECORDING )
                throw new Exception("Audio is not recording");

            int bytes = mRecorder.read( buffer, 0, mRecordBufferSize*2 );
            if(bytes<0) bytes=0;
            return OKResponse( Base64.encodeToString(buffer, 0, bytes, Base64.NO_WRAP) );
        }
        return OKResponse();
    }

    public JSONObject stopRecording(JSONObject params) throws Exception {
        if( mRecorder!=null ){
            if( mRecorder.getState() == AudioRecord.RECORDSTATE_RECORDING )
                mRecorder.stop();
            mRecorder.release();
            mRecorder=null;

            socket.close();
        }
        return OKResponse();
    }
}
