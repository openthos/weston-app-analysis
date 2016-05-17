package com.dxmtb.westonapp;

import android.net.LocalSocket;
import android.net.LocalSocketAddress;
import android.util.Log;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.concurrent.BlockingQueue;

class SocketThread extends Thread
{
    private boolean keepRunning = true;
    private LocalSocket socket;
    private BlockingQueue blockingQueue;
    private String socketPath;

    final private String TAG = "ServerSocketThread";
    final private String serverHello = "ServerHello";
    final private String clientHello = "ClientHello";

    public SocketThread(BlockingQueue blockingQueue, String socketPath) {
        this.blockingQueue = blockingQueue;
        this.socketPath = socketPath;
    }

    public void stopRun()
    {
        keepRunning = false;
    }

    private byte[] lengthToByte(int length) {
        ByteBuffer b = ByteBuffer.allocate(4);
        b.order(ByteOrder.BIG_ENDIAN); // optional, the initial order of a byte buffer is always BIG_ENDIAN.
        b.putInt(length);
        return b.array();
    }

    private byte[] readLen(InputStream inputStream, int dataLen) throws IOException {
        byte[] buf = new byte[dataLen];
        int toBeRead = dataLen, totalRead = 0, readBytes = 0;
        while (toBeRead > 0 &&
                (readBytes = inputStream.read(buf, totalRead, toBeRead)) != -1) {
            toBeRead -= readBytes;
            totalRead += readBytes;
        }

        if (toBeRead == 0)
            return buf;
        throw new IOException("Data too short");
    }

    private byte[] readMsg(LocalSocket socket) throws IOException {
        InputStream inputStream = socket.getInputStream();
        ByteBuffer b = ByteBuffer.wrap(readLen(inputStream, 4));
        //b.order(ByteOrder.BIG_ENDIAN);
        int dataLen = b.getInt();
        Log.d(TAG, "Msg length: " + dataLen);
        byte[] data = readLen(inputStream, dataLen);
        Log.d(TAG, "Msg received");
        return data;
    }

    private void eventLoop() throws IOException {
        OutputStream outputStream = socket.getOutputStream();
        while (keepRunning) {
            try {
                WestonProto.InputEventProto event = (WestonProto.InputEventProto) this.blockingQueue.take();
                byte[] body = WestonProto.InputEventProto.toByteArray(event);
                Log.d(TAG, "Try to send event " + event + ", length " + body.length);
                outputStream.write(lengthToByte(body.length));
                outputStream.write(body);
                Log.d(TAG, "Send event success");
            } catch (InterruptedException e) {
                e.printStackTrace();
                break;
            } catch (IOException e) {
                e.printStackTrace();
                break;
            }
        }
    }

    @Override
    public void run()
    {
        try
        {
            socket = new LocalSocket();
            Log.d(TAG, "Try to connect weston socket: " + socketPath);
            socket.connect(new LocalSocketAddress(socketPath,
                    LocalSocketAddress.Namespace.FILESYSTEM));
            Log.d(TAG, "Socket connected");
            socket.getOutputStream().write(lengthToByte(clientHello.length()));
            socket.getOutputStream().write(clientHello.getBytes());
            Log.d(TAG, "Sent ClientHello");

            String msg = new String(readMsg(socket));
            Log.d(TAG, "Received hello response");
            if (msg.equals(serverHello)) {
                Log.d(TAG, "Server hand shake success, ready to send events");
                eventLoop();
            } else {
                Log.d(TAG, "Server hand shake FAILURE, wrong msg: " + msg);
            }
            socket.close();
        }
        catch (IOException e)
        {
            e.printStackTrace();
            keepRunning = false;
        }
    }
}