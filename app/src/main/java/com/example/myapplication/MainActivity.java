package com.example.myapplication;

import android.annotation.SuppressLint;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import org.eclipse.paho.client.mqttv3.IMqttDeliveryToken;
import org.eclipse.paho.client.mqttv3.MqttCallback;
import org.eclipse.paho.client.mqttv3.MqttClient;
import org.eclipse.paho.client.mqttv3.MqttConnectOptions;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.eclipse.paho.client.mqttv3.MqttMessage;
import org.eclipse.paho.client.mqttv3.persist.MemoryPersistence;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.regex.Pattern;


public class MainActivity<setIcon> extends AppCompatActivity {
    private String host = "tcp://xxx.xxx.42.38:1883";//mqtt服务器连接
    private String userName = "android";
    private String passWord = "android";
    private String mqtt_sub_topic = "dd";
    private String mqtt_pub_topic = "dd_ESP";
    int num = (int)(Math.random()*100);
    private String mqtt_id = String.valueOf(num);
    private ScheduledExecutorService scheduler;
    private Button btn_1;  //类似于单片机开发里面的   参数初始化
    private Button btn_2;
    private Button btn_3;
    private TextView text_test;
    private TextView wsd;
    private MqttClient client;
    private MqttConnectOptions options;
    private Handler handler;
    private boolean isIconChange =false;
    private  int zt1=0;
    private  int zt2=0;
    int i;
    private  boolean sz =false;

    @SuppressLint("HandlerLea`k")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        //这里是界面打开后 最先运行的地方
        System.out.println("请输入"+(i+1)+"个值");
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main); // 对应界面UI
        //一般先用来进行界面初始化 控件初始化  初始化一些参数和变量。。。。。
        //不恰当比方    类似于 单片机的   main函数
        btn_1 = findViewById(R.id.btn_1); // 寻找xml里面真正的id  与自己定义的id绑定+
        btn_2 = findViewById(R.id.btn_2); // 寻找xml里面真正的id  与自己定义的id绑定
        btn_3 = findViewById(R.id.btn_3);
        btn_1.setOnClickListener(new View.OnClickListener() {
            @Override
           public void onClick(View v) {
                switch (zt1){
                    case 0:
                    publishmessageplus(mqtt_pub_topic,"a");
                   btn_1.setBackgroundResource(R.drawable.no);
                    zt1=1;
                    break;

                    case 1:
                    publishmessageplus(mqtt_pub_topic,"b");
                   btn_1.setBackgroundResource(R.drawable.off);
                    zt1=0;
                    break;
                }
                //更直观的方法   用弹窗：toast
                //在当前activity 显示内容为“hello”的短时间弹窗
                //Toast.makeText(MainActivity.this,"hello" ,Toast.LENGTH_SHORT).show();
            }
        });
        btn_2.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                switch (zt2){
                    case 0:
                        publishmessageplus(mqtt_pub_topic,"c");
                        btn_2.setBackgroundResource(R.drawable.no);
                        zt2=1;
                        break;

                    case 1:
                        publishmessageplus(mqtt_pub_topic,"d");
                        btn_2.setBackgroundResource(R.drawable.off);
                        zt2=0;
                        break;
                }
                //更直观的方法   用弹窗：toast
                //在当前activity 显示内容为“hello”的短时间弹窗
                //Toast.makeText(MainActivity.this,"hello" ,Toast.LENGTH_SHORT).show();
            }
        });
        btn_3.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                publishmessageplus(mqtt_pub_topic,"k");
                //更直观的方法   用弹窗：toast
                //在当前activity 显示内容为“hello”的短时间弹窗
                //Toast.makeText(MainActivity.this,"hello" ,Toast.LENGTH_SHORT).show();
            }
        });
        //到这里  你已经学会了基本的安卓开发
        // 按钮单机事件你会了   图片单机呢？？？
        //  两个控件联动    按钮单机 更改 textview 的内容
        wsd =findViewById(R.id.wsd);
//**********************************************************//
        Mqtt_init();
        startReconnect();

        handler = new Handler() {
            @SuppressLint("SetTextI18n")
            public void handleMessage(Message msg) {
                super.handleMessage(msg);
                switch (msg.what){
                    case 1: //开机校验更新回传
                        break;
                    case 2:  // 反馈回传

                        break;
                    case 3:  //MQTT 收到消息回传   UTF8Buffer msg=new UTF8Buffer(object.toString());
                        sz= isInteger(msg.obj.toString());
                       if(sz){
                           wsd.setText("温度："+msg.obj.toString().substring(0,msg.obj.toString().length()-2)+"℃"+"湿度："+msg.obj.toString().substring(msg.obj.toString().length() -2,msg.obj.toString().length())+"%");
                           msg.obj.toString().substring(2);
                       }
                        else {

                       }
                        break;
                    case 30:  //连接失败
                        Toast.makeText(MainActivity.this,"连接失败" , Toast.LENGTH_SHORT).show();
                        break;
                    case 31:   //连接成功
                        Toast.makeText(MainActivity.this,"连接成功" ,Toast.LENGTH_SHORT).show();
                        try {
                            client.subscribe(mqtt_sub_topic,1);
                        } catch (MqttException e) {
                            e.printStackTrace();
                        }
                        break;
                    default:
                        break;
                }
            }
        };
    }

    private void Mqtt_init()
    {
        try {
            //host为主机名，test为clientid即连接MQTT的客户端ID，一般以客户端唯一标识符表示，MemoryPersistence设置clientid的保存形式，默认为以内存保存
            client = new MqttClient(host, mqtt_id,
                    new MemoryPersistence());
            //MQTT的连接设置
            options = new MqttConnectOptions();
            //设置是否清空session,这里如果设置为false表示服务器会保留客户端的连接记录，这里设置为true表示每次连接到服务器都以新的身份连接
            options.setCleanSession(false);
            //设置连接的用户名
            options.setUserName(userName);
            //设置连接的密码
            options.setPassword(passWord.toCharArray());
            // 设置超时时间 单位为秒
            options.setConnectionTimeout(10);
            // 设置会话心跳时间 单位为秒 服务器会每隔1.5*20秒的时间向客户端发送个消息判断客户端是否在线，但这个方法并没有重连的机制
            options.setKeepAliveInterval(20);
            //设置回调
            client.setCallback(new MqttCallback() {
                @Override
                public void connectionLost(Throwable cause) {
                    //连接丢失后，一般在这里面进行重连
                    System.out.println("connectionLost----------");
                    //startReconnect();
                }
                @Override
                public void deliveryComplete(IMqttDeliveryToken token) {
                    //publish后会执行到这里
                    System.out.println("deliveryComplete---------"
                            + token.isComplete());
                }
                @Override
                public void messageArrived(String topicName, MqttMessage message)
                        throws Exception {
                    //subscribe后得到的消息会执行到这里面
                    System.out.println("messageArrived----------");
                    Message msg = new Message();
                    msg.what = 3;   //收到消息标志位
                    msg.obj = message.toString();
                    handler.sendMessage(msg);    // hander 回传
                }
            });
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    public static boolean isInteger(String str) {
        Pattern pattern = Pattern.compile("^[-\\+]?[\\d]*$");
        return pattern.matcher(str).matches();
    }
    private void Mqtt_connect() {
        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    if(!(client.isConnected()) )  //如果还未连接
                    {
                        client.connect(options);
                        Message msg = new Message();
                        msg.what = 31;
                        handler.sendMessage(msg);
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                    Message msg = new Message();
                    msg.what = 30;
                    handler.sendMessage(msg);
                }
            }
        }).start();
    }
    private void startReconnect() {
        scheduler = Executors.newSingleThreadScheduledExecutor();
        scheduler.scheduleAtFixedRate(new Runnable() {
            @Override
            public void run() {
                if (!client.isConnected()) {
                    Mqtt_connect();
                }
            }
        }, 0 * 1000, 10 * 1000, TimeUnit.MILLISECONDS);
    }
    private void publishmessageplus(String topic,String message2)
    {
        if (client == null || !client.isConnected()) {
            return;
        }
        MqttMessage message = new MqttMessage();
        message.setQos(1);
        message.setPayload(message2.getBytes());
        try {
            client.publish(topic,message);
        } catch (MqttException e) {

            e.printStackTrace();
        }
    }





}
