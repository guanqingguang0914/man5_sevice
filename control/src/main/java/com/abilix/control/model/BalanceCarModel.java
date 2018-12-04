package com.abilix.control.model;

import com.abilix.control.GlobalConfig;
import com.abilix.control.balancecar.Mypoint;
import com.abilix.control.protocol.ProtocolBuilder;
import com.abilix.control.sp.SP;
import com.abilix.control.utils.LogMgr;
import com.abilix.control.utils.Utils;

import java.io.BufferedInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.util.ArrayList;

import static com.abilix.control.protocol.ProtocolSender.sendProtocol;

/**
 * Created by guanqg on 2017/9/20.
 */

public class BalanceCarModel {
    private int  V = 30;
    private int  Vleft = 0;
    private float offset = 0;
    private int Vright = 0;
    private boolean ai_flag = false;
    private float yaw = 0;
    private Thread sendThread;
    private static BalanceCarModel instance;
    private BalanceCarModel(){

    }
    public static BalanceCarModel getInstance(){
        if(instance == null){
            instance = new BalanceCarModel();
        }
        return instance;
    }
    //执行划线开始
    public void start(){
        try{
            //第一步取出数据。
            wheelset(V,V);//先发一个0xoA.
            AI_start();
            Thread.sleep(100);//线程等待100ms.
            offset = yaw;
            byte[] data =  toByteArray("line.bin");
            byte[] length = new  byte[4];
            System.arraycopy(data, 2, length, 0, 4);
            int len = Utils.byteAray2IntLH(length);
            //第三步获取数据。
            ArrayList<Mypoint> list1 = getpointlist(data,len);
            //第四步过滤数据。
            ArrayList<Mypoint> list2 = getpointFilter(list1);
            float[] angle = drection(list2);
//            这里处理下方位问题。
            float[] angle2 =dealangle(angle,offset);
            //第五步发送数据。
            send(angle2);
        }catch (Exception e){
            e.printStackTrace();
        }
    }

    private void send(final float[] data) {
        sendThread = new Thread(new Runnable() {
            @Override
            public void run() {
                for(int i=0;i<data.length;i++){
                    for(int j=0;j<8;j++){
                        //第一步拿到钥值。
                        //float d_yaw=yaw-data[i];
                        float d_yaw = getyaw(yaw,data[i]);
                        //假设相差400度 速度相差25.则比例系数为1/16；
                        float kp = (float)1/8;
                        //那么偏差为：
                        float round = kp*d_yaw;
                        //目前只有P调节。
                        Vleft  = (int) (V +round);
                        Vright = (int) (V -round);
                        wheelset(Vleft,Vright);
                        try {
                            Thread.sleep(30);
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }
                    }//添加返点功能；
                }
                balance();
            }
        });
        sendThread.start();
    }

    private void stop(){
        ai_flag = false;
        sendThread.interrupt();
        LogMgr.e("ai_flag = " + ai_flag);
        balance();
    }

    private void balance() {
        // 默认自平衡。
        byte[] data = new byte[4];
        sendProtocol((byte) 0x01, (byte) 0xA3, (byte) 0x29, data);
    }

    private float[] dealangle(float[] angle, float offset) {
        //偏移量。
        offset = getyaw(offset,angle[0]);
        for(int i =0;i<angle.length;i++ ){
            angle[i] = (angle[i] + offset+360)%360;
        }
        return angle;
    }

    private float getyaw(float yao, float angle) {
        float d_yaw = yao -angle;
        if(d_yaw > 180){
            d_yaw = d_yaw - 360;
        }else if(d_yaw < -180){
            d_yaw = d_yaw +360;
        }
        return d_yaw;
    }

    private float[] drection(ArrayList<Mypoint> pointlist) {
        float[] data = new float[ pointlist.size()-1 ];//方向比点少一个。
        for(int i=0; i< pointlist.size()-1; i++){
            //求两点的向量。
            float angle = 0;
            int x =pointlist.get(i+1).getX()-pointlist.get(i).getX();
            int y = pointlist.get(i+1).getY()-pointlist.get(i).getY();
            //向量分方向。
            if( y >= 0){//一二象限合并。
                angle = (float) Math.acos((float)x/Math.sqrt(x*x+y*y));
            }else if(x <= 0){ //第三象限。
                angle = (float) (Math.PI+(float) Math.acos((float)(-x)/Math.sqrt(x*x+y*y)));
            }else if(x > 0){ //第4象限。
                angle = (float) (2*Math.PI - (float) Math.acos((float)x/Math.sqrt(x*x+y*y)));
            }
            data[i] = (float) (angle*57.3);
        }
        return data;
    }

    private ArrayList<Mypoint> getpointFilter(ArrayList<Mypoint> pointlist1) {
        // 目前只做简单的过滤，小于25的过滤掉，倍数补点。
        int devide = 1600;// 25*25 阈值
        ArrayList<Mypoint> pointlist2 = new ArrayList<Mypoint>();
        for (int i = 0; i < pointlist1.size(); i++) {

            // 第一条数据无条件赋值。
            if (i == 0) {
                pointlist2.add(pointlist1.get(0));
            }
            int X1 = pointlist1.get(i).getX();
            int Y1 = pointlist1.get(i).getY();
            // 取最后一个值。
            int X2 = pointlist2.get(pointlist2.size() - 1).getX();
            int Y2 = pointlist2.get(pointlist2.size() - 1).getY();
            int dis = (X1 - X2) * (X1 - X2) + (Y1 - Y2) * (Y1 - Y2);
            if (dis > devide) {
                int num = (int) Math.sqrt(dis / devide);
                for (int j = 1; j <= num; j++) {
                    int x = (int) (X2 + (float) j / num * (X1 - X2));
                    int y = (int) (Y2 + (float) j / num * (Y1 - Y2));
                    Mypoint mypoint = new Mypoint(x, y);
                    pointlist2.add(mypoint);
                }
            }
        }
        return pointlist2;
    }

    private ArrayList<Mypoint> getpointlist(byte[] data, int length) {
        ArrayList<Mypoint> pointlistInit = new ArrayList<Mypoint>();
        byte[] bindata = new byte[length];
        System.arraycopy(data, 6, bindata, 0, length);
        // 一个点4个字节。
        for (int i = 0; i < length - 3; i += 4) {
            int x = Utils.byte2int_2byteLH(bindata, i);
            int y = Utils.byte2int_2byteLH(bindata, i + 2);
            Mypoint mypoint = new Mypoint(x, y);
            pointlistInit.add(mypoint);
        }
        return pointlistInit;
    }

    private byte[] toByteArray(String filename) throws Exception {
        filename = GlobalConfig.KNOW_ROBOT_PATH + File.separator + filename;
        File f = new File(filename);
        if (!f.exists()) {
            throw new FileNotFoundException(filename);
        }
        ByteArrayOutputStream bos = new ByteArrayOutputStream((int) f.length());
        BufferedInputStream in = null;
        try {
            in = new BufferedInputStream(new FileInputStream(f));
            int buf_size = 1024;
            byte[] buffer = new byte[buf_size];
            int len = 0;
            while ((len = in.read(buffer, 0, buf_size)) != -1) {
                bos.write(buffer, 0, len);
            }
            return bos.toByteArray();
        } catch (Exception e) {
            e.printStackTrace();
            throw e;
        } finally {
            try {
                in.close();
            } catch (Exception e) {
                e.printStackTrace();
            }
            bos.close();
        }
    }

    private void AI_start() {
        ai_flag = true;
        new Thread(new Runnable() {
            @Override
            public void run() {
                while (ai_flag) {
                    ai_num();
                }
            }
        }).start();
    }

    private void ai_num() {
        try {
            byte[] cmd = ProtocolBuilder.buildProtocol((byte) 0x01, (byte) 0xA3, (byte) 0x2C, null);
            byte[] ai_receive = SP.request(cmd);
            if (ai_receive == null) {
                LogMgr.e("SP.request Error! null");
                return;
            }
            for (int j = 0; j < 20; j++) {
                if ((ai_receive[j] & 0xff) == 0xf0
                        && (ai_receive[j + 1] & 0xff) == 0x2B) {
                    byte[] temp  = new byte[4];
                    yaw  =	Utils.byte2float(ai_receive, j+6) + 180;
                    System.arraycopy(ai_receive, j+6, temp, 0, 4);
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void wheelset(int left, int right) {
        byte[] data = new byte[7];
        data[0] = 0x0A;// 左右轮速度控制。
        data[1] = 0x00;// 无超声控制。
        left = left + 100;
        if (left > 200) {
            left = 200;
        }
        right = right + 100;
        if (right > 200) {
            right = 200;
        }
        data[2] = (byte) left;// 0~200
        data[3] = (byte) right;// 0~200.
        sendProtocol((byte) 0x01, (byte) 0xA3, (byte) 0x29, data);
    }
    //执行划线结束

}
