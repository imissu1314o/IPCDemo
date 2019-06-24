package com.sc.ipcdemo;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.os.Handler;
import android.os.Message;
import android.os.SystemClock;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.net.Socket;
import java.text.SimpleDateFormat;
import java.util.Date;

public class MainActivity extends AppCompatActivity implements View.OnClickListener {
    private static final int MESSAGE_RECEIVE_NEW_MSG=1;
    private static final int MESSAGE_SOCKET_CONNECTED=2;

    private Button mSendButton;
    private TextView mMessageTextView;
    private EditText mMessageEditText;

    private PrintWriter mPrintWriter;
    private Socket mClientSocket;

    @SuppressLint( "HandlerLeak" )
    private Handler mHandler =new Handler(  ){
        @Override
        public void handleMessage(Message msg){
            switch (msg.what){
                case MESSAGE_RECEIVE_NEW_MSG:{
                    mMessageTextView.setText( mMessageTextView.getText().toString()+(String)msg.obj );break;
                }
                case MESSAGE_SOCKET_CONNECTED:{
                    mSendButton.setEnabled( true ); //?
                    break;
                }
                default:break;

            }
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate( savedInstanceState );
        setContentView( R.layout.activity_main );
        initView();
        initEvent();
        Intent service=new Intent( this,TCPServerService.class );
        startService( service );
        new Thread(  ){
            @Override
            public void run(){
                connectTCPServer();
            }
        }.start();





    }

    @Override
    protected void onDestroy() {
        if(mClientSocket!=null){
            try {
                mClientSocket.shutdownInput();
                mClientSocket.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        super.onDestroy();
    }

    private void initView(){
        mMessageEditText=(EditText)findViewById( R.id.msg );
        mSendButton=(Button)findViewById( R.id.send );
        mMessageTextView=(TextView)findViewById( R.id.msg_container );
    }
    private void initEvent(){
        mSendButton.setOnClickListener( this );
    }
    @SuppressLint( "SimpleDateFormat" )
    private String formatDateTime(long time){
        return new SimpleDateFormat( "(HH:mm:ss)" ).format( new Date(time) );
    }

    private void connectTCPServer(){
        Socket socket=null;


//        Socket client = null;
//        try {
//            client = new Socket("120.77.223.235", 5400);
//            System.out.println("远程主机地址：" + client.getRemoteSocketAddress());
//            OutputStream outToServer = client.getOutputStream();
//            DataOutputStream out = new DataOutputStream(outToServer);
//
//            out.writeUTF("Hello from " + client.getLocalSocketAddress());
//            InputStream inFromServer = client.getInputStream();
//            DataInputStream in = new DataInputStream(inFromServer);
//            System.out.println("服务器响应： " + in.readUTF());
//            client.close();
//        } catch (IOException e) {
//            e.printStackTrace();
//        }


        while(socket==null) {
            try {
                socket = new Socket( "192.168.1.229", 24 );

                mClientSocket = socket;
                mPrintWriter=new PrintWriter( new BufferedWriter( new OutputStreamWriter( socket.getOutputStream() ) ),true );
                mHandler.sendEmptyMessage( MESSAGE_SOCKET_CONNECTED );
                System.out.println("connection server success");

            } catch (IOException e) {
                SystemClock.sleep(1000); //?
                System.out.println("connection tcp server failed,retry...");
                e.printStackTrace();
            }
        }

        try {
            //接受服务器消息
            BufferedReader br=new BufferedReader( new InputStreamReader( socket.getInputStream() ) );
            while(!this.isFinishing()){
                String msg=br.readLine();
                System.out.println("receive:"+msg);
                if(msg!=null){
                    String time=formatDateTime( System.currentTimeMillis() );
                    final String showedMsg="server "+time+":"+msg+"\n";
                    mHandler.obtainMessage(MESSAGE_RECEIVE_NEW_MSG,showedMsg).sendToTarget();
                }
            }
            System.out.println( "quit..." );
            mPrintWriter.close();
            br.close();
            socket.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }


    @Override
    public void onClick(View v) {
        switch (v.getId()){
            case R.id.send:
                final String msg=mMessageEditText.getText().toString();
                if(!msg.isEmpty()&&mPrintWriter!=null){
                    mPrintWriter.print( msg );
                    mMessageEditText.setText( "" );
                    String time=formatDateTime( System.currentTimeMillis() );
                    final String showedMsg="self "+time+":"+msg+"\n";
                    mMessageTextView.setText( mMessageTextView.getText().toString()+" "+showedMsg );
                }
                break;
             default:break;
        }
    }
}
