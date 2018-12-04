package com.abilix.moco.gaitx.kernal.execute;

/**
 * Created by wanghang on 17-7-1.
 */

public class RollFilter {
    private float[] accValues;
    private float[] gyroValues;
    float gyroX, gyroY, gyroZ;
    float accelX, accelY, accelZ;
    float Angle_Balance, Gyro_Balance, Gyro_Turn;
    float Acceleration_Z;
    double K1 = 0.02;
    float angleR, angle_dot;
    double Q_angle = 0.001;
    double Q_gyro = 0.003;
    double R_angle = 0.5;
    //double dt=0.010;
    double dt = 0.1;   //0.0025
    char C_0 = 1;
    float Q_bias, Angle_err;
    float PCt_0, PCt_1, E;
    float K_0, K_1, t_0, t_1;
    public float[] Pdot = {0, 0, 0, 0};
    public float[][] PP_new = {{1, 0}, {0, 1}};

    public float Get_Angle(int way, float gyroX, float gyroZ, float accelY, float accelZ) {
        float Accel_Y;
        float Accel_Angle;
        float Accel_Z;
        float Gyro_X;
        float Gyro_Z;

        Gyro_X = gyroX;
        Gyro_Z = gyroZ;
        Accel_Y = accelY;
        Accel_Z = accelZ;
        if (Gyro_X > 32768) Gyro_X -= 65536;
        if (Gyro_Z > 32768) Gyro_Z -= 65536;
        if (Accel_Y > 32768) Accel_Y -= 65536;
        if (Accel_Z > 32768) Accel_Z -= 65536;
        Gyro_Balance = Gyro_X;
        Accel_Angle = (float) (Math.atan2(Accel_Y, Accel_Z) * 180 / (3.1415926));
        //Gyro_X= (float) (Gyro_X/16.4/2);
        if (way == 2) Kalman_Filter(Accel_Angle, Gyro_X);
        else if (way == 3) Yijielvbo(Accel_Angle, Gyro_X);
        Angle_Balance = angleR;
        Gyro_Turn = Gyro_Z;
        Acceleration_Z = Accel_Z;
        return Angle_Balance;
    }

    private void Kalman_Filter(float Accel, float Gyro) {
        angleR += (Gyro - Q_bias) * dt;
        Pdot[0] = (float) (Q_angle - PP_new[0][1] - PP_new[1][0]);

        Pdot[1] = -PP_new[1][1];
        Pdot[2] = -PP_new[1][1];
        Pdot[3] = (float) Q_gyro;
        PP_new[0][0] += Pdot[0] * dt;
        PP_new[0][1] += Pdot[1] * dt;
        PP_new[1][0] += Pdot[2] * dt;
        PP_new[1][1] += Pdot[3] * dt;

        Angle_err = Accel - angleR;

        PCt_0 = C_0 * PP_new[0][0];
        PCt_1 = C_0 * PP_new[1][0];

        E = (float) (R_angle + C_0 * PCt_0);

        K_0 = PCt_0 / E;
        K_1 = PCt_1 / E;

        t_0 = PCt_0;
        t_1 = C_0 * PP_new[0][1];

        PP_new[0][0] -= K_0 * t_0;
        PP_new[0][1] -= K_0 * t_1;
        PP_new[1][0] -= K_1 * t_0;
        PP_new[1][1] -= K_1 * t_1;

        angleR += K_0 * Angle_err;
        Q_bias += K_1 * Angle_err;
        angle_dot = Gyro - Q_bias;
    }

    private void Yijielvbo(float angle_m, float gyro_m) {
        angleR = (float) (K1 * angle_m + (1 - K1) * (angleR + gyro_m * 0.010));        // 0.005
    }
}
